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
	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
	private final ObjectMapper objectMapper;

	private final ConfigService configService;
	private final GitService gitService;

	// Reactive WebSocket客户端
	private final ReactorNettyWebSocketClient webSocketClient = new ReactorNettyWebSocketClient();

	@Autowired
	public P2PWebSocketHandlerReactive(ObjectMapper objectMapper, ConfigService configService, GitService gitService) {
		this.objectMapper = objectMapper;
		this.configService = configService;
		this.gitService = gitService;
	}

	@Override
	public Mono<Void> handle(WebSocketSession session) {
		// 保存会话
		sessions.put(session.getId(), session);
		logger.info("P2P WebSocket connection established: {}", session.getId());

		// 处理接收到的消息
		Mono<Void> input = session.receive()
				.map(WebSocketMessage::getPayloadAsText)
				.doOnNext(payload -> {
					logger.debug("P2P WebSocket message received from {}: {}", session.getId(), payload);
					try {
						P2PMessage p2pMessage = objectMapper.readValue(payload, P2PMessage.class);

						if (p2pMessage instanceof RepoConfigP2PNotification notification) {
							logger.info("Received RepoConfigP2PNotification: {}", notification);
							handleConfigNotification(notification);
						} else if (p2pMessage instanceof RepoSyncP2PRequest request) {
							logger.info("Received RepoSyncP2PRequest: {}", request);
							handleSyncRequest(request, session);
						} else {
							logger.warn("Received unknown P2PMessage type from {}", session.getId());
						}
					} catch (JsonProcessingException e) {
						logger.error("Failed to parse P2PMessage from {}: {}", session.getId(), payload, e);
					} catch (Exception e) {
						logger.error("Error handling P2P message from {}: {}", session.getId(), payload, e);
					}
				})
				.then();

		// 注册会话关闭时的清理操作
		return session.closeStatus()
				.doOnNext(status -> {
					logger.info("P2P WebSocket connection closed: {} with status: {}", session.getId(), status);
					sessions.remove(session.getId());
				})
				.then(input);
	}

	private void handleConfigNotification(RepoConfigP2PNotification notification) {
		String alias = notification.getRepoAlias();
		if (notification.getAction() == RepoConfigP2PNotification.Action.ADD) {
			if (configService.getRepositoryConfigByAlias(alias).isEmpty()) {
				// Token 为 null 或特定标记，因为P2P不传输Token
				RepositoryConfig newConfig = new RepositoryConfig(alias, notification.getRepoUrl(),
						"p2p_pending/" + alias, null /* NO TOKEN */);
				boolean added = configService.addRepositoryConfig(newConfig);
				if (added) {
					logger.info("Added new repository config from P2P notification: {}", alias);
				}
			}
		} else if (notification.getAction() == RepoConfigP2PNotification.Action.REMOVE) {
			configService.removeRepositoryConfig(alias);
			logger.info("Removed repository config from P2P notification: {}", alias);
		}
	}

	private void handleSyncRequest(RepoSyncP2PRequest request, WebSocketSession session) {
		// 实现同步请求处理逻辑
		logger.info("Handling sync request for repo: {}", request.getRepoUrlOrAlias());
		// 此处根据项目需求实现...
	}

	/**
	 * 连接到对等节点
	 * 
	 * @param peerAddress 对等节点地址
	 */
	public Mono<Void> connectToPeer(String peerAddress) {
		URI wsUri = URI.create("ws://" + peerAddress + "/p2p");
		logger.info("Attempting to connect to peer at {}", wsUri);

		return webSocketClient.execute(wsUri, session -> {
			logger.info("Connected to peer as client: {}", wsUri);

			// 处理接收到的消息
			Mono<Void> receive = session.receive()
					.map(WebSocketMessage::getPayloadAsText)
					.doOnNext(payload -> {
						logger.debug("Received message as client: {}", payload);
						try {
							P2PMessage p2pMessage = objectMapper.readValue(payload, P2PMessage.class);
							// 客户端处理逻辑
						} catch (JsonProcessingException e) {
							logger.error("Failed to parse message as client", e);
						}
					})
					.then();

			// 发送初始消息 - 如果需要的话
			return receive;
		}).doOnError(e -> {
			logger.error("Error connecting to peer: {}", wsUri, e);
		});
	}

	/**
	 * 向所有连接的对等节点广播消息
	 * 
	 * @param message 要广播的消息
	 */
	public void broadcastMessage(P2PMessage message) {
		try {
			String messageJson = objectMapper.writeValueAsString(message);
			logger.debug("Broadcasting message to {} peers: {}", sessions.size(), messageJson);

			for (WebSocketSession session : sessions.values()) {
				session.send(Mono.just(session.textMessage(messageJson)))
						.subscribe(
								null,
								error -> logger.error("Error sending message to session {}", session.getId(), error));
			}
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize message for broadcast", e);
		}
	}
}
