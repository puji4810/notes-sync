package puji.p2p_notes_sync.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import puji.p2p_notes_sync.service.config.ConfigService;
import puji.p2p_notes_sync.service.git.GitService;
import puji.p2p_notes_sync.service.docs.MkDocsService;
import puji.p2p_notes_sync.model.config.RepositoryConfig;

import java.util.List;
import java.util.Optional;

/**
 * 工具类：笔记仓库管理
 * 使用标准Spring AI工具调用协议提供给LLM调用的笔记仓库管理功能
 */
@Service
public class NotesManagementMcpTools {

	private final ConfigService configService;
	private final GitService gitService;
	private final MkDocsService mkDocsService;

	public NotesManagementMcpTools(ConfigService configService, GitService gitService, MkDocsService mkDocsService) {
		this.configService = configService;
		this.gitService = gitService;
		this.mkDocsService = mkDocsService;
	}

	@Tool(description = "获取所有仓库列表")
	public List<RepositoryConfig> listRepositories() {
		return configService.getAllRepositoryConfigs();
	}

	@Tool(description = "获取指定仓库配置")
	public Optional<RepositoryConfig> getRepositoryConfig(String repoAlias) {
		return configService.getRepositoryConfigByAlias(repoAlias);
	}

	@Tool(description = "添加新仓库")
	public String addRepository(String repoAlias, String remoteUrl, String localPath, String token) {
		try {
			RepositoryConfig config = new RepositoryConfig(repoAlias, remoteUrl, localPath, token, null);
			boolean success = configService.addRepositoryConfig(config);
			return success ? "Successfully added repository: " + repoAlias : "Failed to add repository: " + repoAlias;
		} catch (Exception e) {
			return "Failed to add repository: " + e.getMessage();
		}
	}

	@Tool(description = "更新仓库配置")
	public String updateRepository(String oldRepoAlias, String newRepoAlias, String remoteUrl, String localPath,
			String token) {
		try {
			Optional<RepositoryConfig> oldConfig = configService.getRepositoryConfigByAlias(oldRepoAlias);
			if (oldConfig.isEmpty()) {
				return "Repository not found: " + oldRepoAlias;
			}

			RepositoryConfig newConfig = new RepositoryConfig(newRepoAlias, remoteUrl, localPath, token,
					oldConfig.get().syncConfig());
			boolean success = configService.updateRepositoryConfig(oldRepoAlias, newConfig);
			return success ? "Successfully updated repository from " + oldRepoAlias + " to " + newRepoAlias
					: "Failed to update repository: " + oldRepoAlias;
		} catch (Exception e) {
			return "Failed to update repository: " + e.getMessage();
		}
	}

	@Tool(description = "删除仓库")
	public String removeRepository(String repoAlias) {
		try {
			boolean success = configService.removeRepositoryConfig(repoAlias);
			return success ? "Successfully removed repository: " + repoAlias
					: "Failed to remove repository: " + repoAlias;
		} catch (Exception e) {
			return "Failed to remove repository: " + e.getMessage();
		}
	}

	@Tool(description = "同步仓库")
	public String syncRepository(String repoAlias) {
		try {
			Optional<RepositoryConfig> config = configService.getRepositoryConfigByAlias(repoAlias);
			if (config.isEmpty()) {
				return "Repository not found: " + repoAlias;
			}
			String result = gitService.pullRepository(config.get());
			return "Successfully synced repository: " + repoAlias + "\n" + result;
		} catch (Exception e) {
			return "Failed to sync repository: " + e.getMessage();
		}
	}

	@Tool(description = "克隆仓库")
	public String cloneRepository(String repoAlias) {
		try {
			Optional<RepositoryConfig> config = configService.getRepositoryConfigByAlias(repoAlias);
			if (config.isEmpty()) {
				return "Repository not found: " + repoAlias;
			}
			String result = gitService.cloneRepository(config.get());
			return "Successfully cloned repository: " + repoAlias + "\n" + result;
		} catch (Exception e) {
			return "Failed to clone repository: " + e.getMessage();
		}
	}

	@Tool(description = "提交并推送更改")
	public String commitAndPush(String repoAlias, String commitMessage) {
		try {
			Optional<RepositoryConfig> config = configService.getRepositoryConfigByAlias(repoAlias);
			if (config.isEmpty()) {
				return "Repository not found: " + repoAlias;
			}
			String result = gitService.addCommitAndPush(config.get(), commitMessage, null, null);
			return "Successfully committed and pushed changes for repository: " + repoAlias + "\n" + result;
		} catch (Exception e) {
			return "Failed to commit and push changes: " + e.getMessage();
		}
	}

	@Tool(description = "部署为 MkDocs 网站")
	public String deployAsMkDocs(String repoAlias) {
		try {
			Optional<RepositoryConfig> config = configService.getRepositoryConfigByAlias(repoAlias);
			if (config.isEmpty()) {
				return "Repository not found: " + repoAlias;
			}
			String result = mkDocsService.buildSite(config.get());
			return "Successfully deployed repository as MkDocs site: " + repoAlias + "\n" + result;
		} catch (Exception e) {
			return "Failed to deploy as MkDocs site: " + e.getMessage();
		}
	}
}