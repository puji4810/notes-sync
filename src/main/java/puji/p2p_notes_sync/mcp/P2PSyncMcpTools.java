package puji.p2p_notes_sync.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import puji.p2p_notes_sync.p2p.coordinator.P2PCoordinatorService;
import puji.p2p_notes_sync.service.config.ConfigService;
import puji.p2p_notes_sync.model.config.RepositoryConfig;

import java.util.Optional;

/**
 * 工具类：P2P同步管理
 * 使用标准Spring AI工具调用协议提供给LLM调用的P2P网络同步功能
 */
@Service
public class P2PSyncMcpTools {

	private final P2PCoordinatorService p2pCoordinatorService;
	private final ConfigService configService;

	public P2PSyncMcpTools(P2PCoordinatorService p2pCoordinatorService, ConfigService configService) {
		this.p2pCoordinatorService = p2pCoordinatorService;
		this.configService = configService;
	}

	@Tool(description = "向P2P网络广播新添加的仓库配置")
	public String broadcastNewRepository(String repoAlias) {
		Optional<RepositoryConfig> configOpt = configService.getRepositoryConfigByAlias(repoAlias);
		if (configOpt.isEmpty()) {
			return "Repository with alias '" + repoAlias + "' not found.";
		}

		try {
			p2pCoordinatorService.broadcastNewRepositoryConfiguration(configOpt.get());
			return "Successfully broadcasted new repository configuration for '" + repoAlias + "'";
		} catch (Exception e) {
			return "Failed to broadcast new repository: " + e.getMessage();
		}
	}

	@Tool(description = "向P2P网络广播更新后的仓库配置")
	public String broadcastUpdateRepository(String oldRepoAlias, String newRepoAlias) {
		Optional<RepositoryConfig> configOpt = configService.getRepositoryConfigByAlias(newRepoAlias);
		if (configOpt.isEmpty()) {
			return "Repository with alias '" + newRepoAlias + "' not found.";
		}

		try {
			p2pCoordinatorService.broadcastUpdateRepositoryConfiguration(oldRepoAlias, configOpt.get());
			return "Successfully broadcasted repository update from '" + oldRepoAlias + "' to '" + newRepoAlias + "'";
		} catch (Exception e) {
			return "Failed to broadcast repository update: " + e.getMessage();
		}
	}

	@Tool(description = "向P2P网络广播删除的仓库配置")
	public String broadcastRemoveRepository(String repoAlias) {
		try {
			p2pCoordinatorService.broadcastRemovedRepositoryConfiguration(repoAlias);
			return "Successfully broadcasted repository removal for '" + repoAlias + "'";
		} catch (Exception e) {
			return "Failed to broadcast repository removal: " + e.getMessage();
		}
	}

	@Tool(description = "向P2P网络广播同步请求")
	public String broadcastSyncRequest(String repoAlias) {
		try {
			p2pCoordinatorService.broadcastSyncRequest(repoAlias);
			return "Successfully broadcasted sync request for '" + repoAlias + "'";
		} catch (Exception e) {
			return "Failed to broadcast sync request: " + e.getMessage();
		}
	}
}