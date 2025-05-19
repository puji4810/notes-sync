package puji.p2p_notes_sync.config;

import io.swagger.v3.oas.annotations.media.Schema; // 导入Schema注解

public record RepositoryConfig(
		@Schema(description = "用户定义的仓库别名，在单个用户配置中应唯一", example = "我的工作笔记", requiredMode = Schema.RequiredMode.REQUIRED) String alias,

		@Schema(description = "Git仓库的HTTPS或SSH URL", example = "https://github.com/user/work-notes.git", requiredMode = Schema.RequiredMode.REQUIRED) String gitUrl,

		@Schema(description = "笔记仓库在本地设备上的存储路径", example = "my_notes/work", requiredMode = Schema.RequiredMode.REQUIRED) String localPath,

		@Schema(description = "用于访问Git仓库的Personal Access Token (PAT)。此Token仅在当前设备本地加密存储，不会通过P2P网络传输。在注册新仓库时提供。", example = "ghp_xxxxxxxxxxxxxxxxxxxx") String token) {
	public static RepositoryConfig defaultConfig() {
		// 提供一个示例或空配置
		return new RepositoryConfig(
				"My Default Notes",
				"https://github.com/your-username/your-default-notes.git", // 示例HTTPS URL
				"default_notes_repo",
				"YOUR_GIT_PAT_HERE" // 提醒用户替换
		);
	}
}