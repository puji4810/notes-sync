package puji.p2p_notes_sync.p2p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import puji.p2p_notes_sync.config.RepositoryConfig; // 从你的项目导入
import puji.p2p_notes_sync.p2p.dto.RepoConfigP2PNotification;
import puji.p2p_notes_sync.p2p.dto.RepoSyncP2PRequest;

@Service
public class P2PCoordinatorService {

	private static final Logger logger = LoggerFactory.getLogger(P2PCoordinatorService.class);

	private final P2PWebSocketHandlerReactive p2pWebSocketHandler;
	// private final MDNSService mdnsservice; // 如果需要主动连接，可能需要它

	@Autowired
	public P2PCoordinatorService(P2PWebSocketHandlerReactive p2pWebSocketHandler /* , MDNSService mdnsservice */) {
		this.p2pWebSocketHandler = p2pWebSocketHandler;
		// this.mdnsservice = mdnsservice;
	}

	/**
	 * 当本地添加一个新的仓库配置时，向其他节点广播此信息。
	 * 
	 * @param newRepoConfig 新添加的仓库配置 (不应包含token)
	 */
	public void broadcastNewRepositoryConfiguration(RepositoryConfig newRepoConfig) {
		if (newRepoConfig == null)
			return;
		logger.info("P2P: Broadcasting ADD for new repository config: {}", newRepoConfig.alias());
		RepoConfigP2PNotification notification = new RepoConfigP2PNotification(
				RepoConfigP2PNotification.Action.ADD,
				newRepoConfig.alias(),
				newRepoConfig.gitUrl() // 确保不发送token
		);
		p2pWebSocketHandler.broadcastMessage(notification);
	}

	/**
	 * 当本地移除一个仓库配置时，向其他节点广播此信息。
	 * 
	 * @param repoAlias 被移除的仓库别名
	 */
	public void broadcastRemovedRepositoryConfiguration(String repoAlias) {
		if (repoAlias == null || repoAlias.isBlank())
			return;
		logger.info("P2P: Broadcasting REMOVE for repository config: {}", repoAlias);
		RepoConfigP2PNotification notification = new RepoConfigP2PNotification(
				RepoConfigP2PNotification.Action.REMOVE,
				repoAlias,
				null // URL 对于 REMOVE 不是必需的
		);
		p2pWebSocketHandler.broadcastMessage(notification);
	}

	/**
	 * 当本地更新一个仓库配置时，向其他节点广播此信息。
	 * 
	 * @param oldRepoAlias      更新前的仓库别名
	 * @param updatedRepoConfig 更新后的仓库配置 (不应包含token)
	 */
	public void broadcastUpdateRepositoryConfiguration(String oldRepoAlias, RepositoryConfig updatedRepoConfig) {
		if (updatedRepoConfig == null)
			return;

		logger.info("P2P: Broadcasting UPDATE for repository config: {} (old alias: {})",
				updatedRepoConfig.alias(), oldRepoAlias);

		// 使用新的构造函数，包含旧别名
		RepoConfigP2PNotification notification = new RepoConfigP2PNotification(
				RepoConfigP2PNotification.Action.UPDATE,
				oldRepoAlias, // 旧别名
				updatedRepoConfig.alias(), // 新别名
				updatedRepoConfig.gitUrl() // 确保不发送token
		);
		p2pWebSocketHandler.broadcastMessage(notification);
	}

	/**
	 * 当本地发起同步操作后，向其他节点请求对同一仓库进行同步。
	 * 
	 * @param repoUrlOrAlias 要同步的仓库URL或别名
	 */
	public void broadcastSyncRequest(String repoUrlOrAlias) {
		if (repoUrlOrAlias == null || repoUrlOrAlias.isBlank())
			return;
		logger.info("P2P: Broadcasting SYNC request for repository: {}", repoUrlOrAlias);
		RepoSyncP2PRequest request = new RepoSyncP2PRequest(repoUrlOrAlias);
		p2pWebSocketHandler.broadcastMessage(request);
	}
}