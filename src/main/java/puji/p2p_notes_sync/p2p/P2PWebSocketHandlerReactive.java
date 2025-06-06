package puji.p2p_notes_sync.p2p;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers; // 导入 Schedulers

import puji.p2p_notes_sync.config.RepositoryConfig;
import puji.p2p_notes_sync.p2p.dto.P2PMessage;
import puji.p2p_notes_sync.p2p.dto.RepoConfigP2PNotification;
import puji.p2p_notes_sync.p2p.dto.RepoSyncP2PRequest;
import puji.p2p_notes_sync.service.ConfigService;
import puji.p2p_notes_sync.service.GitService;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class P2PWebSocketHandlerReactive implements WebSocketHandler {

	private static final Logger logger = LoggerFactory.getLogger(P2PWebSocketHandlerReactive.class);
	// sessions can now be managed uniformly, or distinguish between serverSessions
	// and clientSessions
	// For simplicity, let's assume that broadcastMessage will handle the sending
	// logic
	private final Map<String, WebSocketSession> serverSessions = new ConcurrentHashMap<>(); // Sessions received as
																							// server
	private final Map<String, WebSocketSession> clientSessions = new ConcurrentHashMap<>(); // Sessions that are
																							// actively connected as
																							// clients

	private final ObjectMapper objectMapper;
	private final ConfigService configService;
	private final GitService gitService;
	private final ReactorNettyWebSocketClient webSocketClient = new ReactorNettyWebSocketClient();

	@Autowired
	public P2PWebSocketHandlerReactive(ObjectMapper objectMapper, ConfigService configService, GitService gitService) {
		this.objectMapper = objectMapper;
		this.configService = configService;
		this.gitService = gitService;
	}

	// Private assistive method for distributing and processing p2 p messages
	private void dispatchP2PMessage(P2PMessage p2pMessage, WebSocketSession session) {
		String sessionId = (session != null) ? session.getId() : "N/A (client-side initiated or unknown)";
		if (p2pMessage instanceof RepoConfigP2PNotification notification) {
			logger.info("Dispatching RepoConfigP2PNotification from session {}: {}", sessionId, notification);
			// ConfigService 的方法可能是阻塞的，使用 Schedulers.boundedElastic()
			Mono.fromRunnable(() -> handleConfigNotification(notification))
					.subscribeOn(Schedulers.boundedElastic())
					.doOnError(e -> logger.error("Error in handleConfigNotification for session {}: {}", sessionId,
							e.getMessage(), e))
					.subscribe();
		} else if (p2pMessage instanceof RepoSyncP2PRequest request) {
			logger.info("Dispatching RepoSyncP2PRequest from session {}: {}", sessionId, request);
			// GitService 的方法可能是阻塞的
			Mono.fromRunnable(() -> handleSyncRequest(request, session)) // session may be useful for the context of
																			// synchronous requests
					.subscribeOn(Schedulers.boundedElastic())
					.doOnError(e -> logger.error("Error in handleSyncRequest for session {}: {}", sessionId,
							e.getMessage(), e))
					.subscribe();
		} else {
			logger.warn("Received unknown P2PMessage type from session {}: {}", sessionId,
					p2pMessage.getClass().getName());
		}
	}

	@Override
	public Mono<Void> handle(WebSocketSession session) {
		serverSessions.put(session.getId(), session);
		logger.info("P2P WebSocket connection established (server-side): {} from {}", session.getId(),
				session.getHandshakeInfo().getRemoteAddress());

		Mono<Void> input = session.receive()
				.map(WebSocketMessage::getPayloadAsText)
				.doOnNext(payload -> {
					logger.debug("P2P WebSocket message received (server-side) from {}: {}", session.getId(), payload);
					try {
						P2PMessage p2pMessage = objectMapper.readValue(payload, P2PMessage.class);
						dispatchP2PMessage(p2pMessage, session); // Calling the distribution method
					} catch (JsonProcessingException e) {
						logger.error("Failed to parse P2PMessage (server-side) from {}: {}", session.getId(), payload,
								e);
					} catch (Exception e) { // Catch a general exception
						logger.error("Error processing P2P message (server-side) from {}: {}", session.getId(), payload,
								e);
					}
				})
				.then();

		// Under normal circumstances, the server should also be able to send messages,
		// but the broadcast logic is usually called externally.
		// If the server needs to actively send messages to a connected client, you can
		// add the session.send(...) logic here
		// Here we mainly deal with inbound messages and connection closure

		return input.doFinally(signalType -> {
			logger.info("P2P WebSocket connection closed (server-side): {} with signal: {}", session.getId(),
					signalType);
			serverSessions.remove(session.getId());
		});
	}

	private void handleConfigNotification(RepoConfigP2PNotification notification) {
		String alias = notification.getRepoAlias();
		if (notification.getAction() == RepoConfigP2PNotification.Action.ADD) {
			// Check if the same configuration already exists locally to avoid duplicate
			// additions or conflicts
			if (configService.getRepositoryConfigByAlias(alias).isEmpty()) {
				RepositoryConfig newConfig = new RepositoryConfig(alias, notification.getRepoUrl(),
						"p2p_pending/" + alias, null /* 密钥 */);
				boolean added = configService.addRepositoryConfig(newConfig); // Assume this method is synchronously
																				// blocked
				if (added) {
					logger.info(
							"P2P: Added new repository config for '{}' from notification. User needs to provide token locally.",
							alias);
				} else {
					logger.warn(
							"P2P: Failed to add repository config for '{}' from notification (e.g. alias conflict or other issue).",
							alias);
				}
			} else {
				logger.info(
						"P2P: Repository config for alias '{}' from notification already exists locally. Skipping add.",
						alias);
			}
		} else if (notification.getAction() == RepoConfigP2PNotification.Action.REMOVE) {
			boolean removed = configService.removeRepositoryConfig(alias); // Assume this method is synchronously
																			// blocked
			if (removed) {
				logger.info("P2P: Removed repository config for '{}' from notification.", alias);
			} else {
				logger.warn("P2P: Repository config for alias '{}' not found locally for removal or failed to remove.",
						alias);
			}
		} else if (notification.getAction() == RepoConfigP2PNotification.Action.UPDATE) {
			// 处理更新操作
			// 首先尝试使用旧别名查找仓库
			String oldAlias = notification.getOldRepoAlias();
			String newAlias = notification.getRepoAlias();

			logger.info("P2P: Processing UPDATE notification from old alias '{}' to new alias '{}'", oldAlias,
					newAlias);

			// 首先尝试使用旧别名查找
			configService.getRepositoryConfigByAlias(oldAlias).ifPresentOrElse(
					existingConfig -> {
						// 创建新的配置对象，保留本地token和路径，但更新别名和URL
						RepositoryConfig updatedConfig = new RepositoryConfig(
								newAlias, // 使用新别名
								notification.getRepoUrl(), // 使用远程传来的新URL
								existingConfig.localPath(), // 保留本地路径
								existingConfig.token() // 保留本地token
						);

						// 使用旧别名删除旧配置
						boolean removed = configService.removeRepositoryConfig(oldAlias);
						if (!removed) {
							logger.warn("P2P: Failed to remove old repository config for '{}' during update.",
									oldAlias);
						}

						// 添加新配置
						boolean added = configService.addRepositoryConfig(updatedConfig);
						if (added) {
							logger.info("P2P: Successfully updated repository config from '{}' to '{}'.", oldAlias,
									newAlias);
						} else {
							logger.warn("P2P: Failed to add updated repository config for '{}'.", newAlias);
						}
					},
					() -> {
						// 如果找不到旧别名，尝试更新现有的相同别名配置
						configService.getRepositoryConfigByAlias(newAlias).ifPresentOrElse(
								existingConfig -> {
									// 创建新的配置对象，保留本地token和路径
									RepositoryConfig updatedConfig = new RepositoryConfig(
											newAlias,
											notification.getRepoUrl(), // 使用远程传来的新URL
											existingConfig.localPath(), // 保留本地路径
											existingConfig.token() // 保留本地token
									);

									boolean updated = configService.updateRepositoryConfig(newAlias, updatedConfig);
									if (updated) {
										logger.info("P2P: Updated repository config for '{}' from notification.",
												newAlias);
									} else {
										logger.warn("P2P: Failed to update repository config for '{}'.", newAlias);
									}
								},
								() -> {
									logger.warn(
											"P2P: Repository config not found for update with either old alias '{}' or new alias '{}'",
											oldAlias, newAlias);
									// 可选：如果URL不为空，可以考虑添加新配置
									if (notification.getRepoUrl() != null && !notification.getRepoUrl().isBlank()) {
										RepositoryConfig newConfig = new RepositoryConfig(
												newAlias,
												notification.getRepoUrl(),
												"p2p_pending/" + newAlias,
												null);
										boolean added = configService.addRepositoryConfig(newConfig);
										if (added) {
											logger.info(
													"P2P: Repository '{}' not found for update, added as new instead.",
													newAlias);
										}
									}
								});
					});
		}
	}

	private void handleSyncRequest(RepoSyncP2PRequest request, WebSocketSession session) {
		String repoId = request.getRepoUrlOrAlias();
		logger.info("Handling sync request for repo: {} from session {}", repoId,
				(session != null ? session.getId() : "N/A"));
		configService.getRepositoryConfigByAlias(repoId)
				.or(() -> configService.getAllRepositoryConfigs().stream()
						.filter(cfg -> cfg.gitUrl().equalsIgnoreCase(repoId))
						.findFirst())
				.ifPresentOrElse(
						config -> {
							logger.info("P2P: Executing sync for repository '{}' due to P2P request.", config.alias());
							String result = gitService.pullRepository(config); // Assume this method is synchronously
																				// blocked
							logger.info("P2P: Sync result for repository '{}': {}", config.alias(), result);
							// 可选：通过session将同步结果反馈给请求方
							// if (session != null && session.isOpen()) {
							// session.send(Mono.just(session.textMessage("Sync for " + config.alias() + "
							// result: " + result))).subscribe();
							// }
						},
						() -> logger.warn("P2P: Received sync request for unknown repository '{}'", repoId));
	}

	public Mono<Void> connectToPeer(String peerAddress /* 主机：端口 */) {
		// Avoid repeated connections
		if (clientSessions.containsKey(peerAddress) && clientSessions.get(peerAddress).isOpen()) {
			logger.debug("Already connected or connecting to peer {}", peerAddress);
			return Mono.empty();
		}

		URI wsUri = URI.create("ws://" + peerAddress + "/p2p");
		logger.info("Attempting to connect to peer at {}", wsUri);

		// 此特定客户连接的WebSockethandler
		WebSocketHandler clientConnectionHandler = clientSession -> {
			clientSessions.put(peerAddress, clientSession); // Use peerAddress as key
			logger.info("Successfully connected to peer (client-side): {}, session ID: {}", peerAddress,
					clientSession.getId());

			Mono<Void> clientInput = clientSession.receive()
					.map(WebSocketMessage::getPayloadAsText)
					.doOnNext(payload -> {
						logger.debug("Message received from peer server {}: {}", peerAddress, payload);
						try {
							P2PMessage p2pMessage = objectMapper.readValue(payload, P2PMessage.class);
							dispatchP2PMessage(p2pMessage, clientSession); // Calling the distribution method
						} catch (JsonProcessingException e) {
							logger.error("Failed to parse P2PMessage from peer server {}: {}", peerAddress, payload, e);
						} catch (Exception e) {
							logger.error("Error processing P2P message from peer server {}: {}", peerAddress, payload,
									e);
						}
					})
					.then();

			// If the client needs to send a message immediately after connecting, you can
			// add clientSession.send(...)
			// Mono<Void> initialSend =
			// clientSession.send(Mono.just(clientSession.textMessage("Hello from
			// client!"));
			// return initialSend.then(clientInput);

			return clientInput.doFinally(signalType -> {
				logger.info("Client connection to peer {} closed with signal {}", peerAddress, signalType);
				clientSessions.remove(peerAddress);
			});
		};

		return webSocketClient.execute(wsUri, clientConnectionHandler)
				.doOnError(e -> {
					logger.error("Error connecting to peer: {}, Error: {}", wsUri, e.getMessage());
					clientSessions.remove(peerAddress); // Also remove when the connection fails to avoid inconsistent
														// status
				});
	}

	public void broadcastMessage(P2PMessage message) {
		try {
			String messageJson = objectMapper.writeValueAsString(message);
			logger.info("Attempting to broadcast message to {} server sessions and {} client sessions: {}",
					serverSessions.size(), clientSessions.size(), messageJson);

			// Broadcast to sessions connected to this server
			serverSessions.values().forEach(session -> {
				if (session.isOpen()) {
					session.send(Mono.just(session.textMessage(messageJson)))
							.doOnError(e -> logger.error("Error sending broadcast to server session {}: {}",
									session.getId(), e.getMessage(), e))
							.onErrorResume(e -> Mono.empty()) // Ignore individual send failures and continue with
																// others
							.subscribe();
				}
			});

			// Broadcast the session to which this node is connected as a client
			clientSessions.values().forEach(session -> {
				if (session.isOpen()) {
					session.send(Mono.just(session.textMessage(messageJson)))
							.doOnError(e -> logger.error("Error sending broadcast to client session {}: {}",
									session.getId(), e.getMessage(), e))
							.onErrorResume(e -> Mono.empty())
							.subscribe();
				}
			});

		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize message for broadcast", e);
		}
	}
}