package puji.p2p_notes_sync.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import puji.p2p_notes_sync.config.RepositoryConfig; // 更新导入

import java.io.File;
import java.io.IOException;

@Service
public class GitService {

	private static final Logger logger = LoggerFactory.getLogger(GitService.class);

	private UsernamePasswordCredentialsProvider getCredentialsProvider(String token) {
		// 对于许多Git服务商（如GitHub, GitLab），PAT通常用作密码，用户名可以是任意非空字符串或特定值。
		// "PRIVATE-TOKEN" 或 "oauth2" 是常见的占位用户名，或者直接使用实际用户名。
		// 如果token本身就是用户名，则密码字段为空。这里假设token是密码。
		if (token == null || token.isBlank()) {
			logger.warn("No Git token provided. Operations on private repositories may fail.");
			return null; // 对于公开仓库可能不需要凭证
		}
		return new UsernamePasswordCredentialsProvider("PRIVATE-TOKEN", token);
	}

	public String cloneRepository(RepositoryConfig config) {
		File localDir = new File(config.localPath());
		if (localDir.exists()) {
			logger.info("Directory {} already exists. Skipping clone. Consider pull.", config.localPath());
			return "Directory already exists. Consider pull.";
		}

		logger.info("Cloning repository {} into {} using JGit", config.gitUrl(), config.localPath());
		try (Git result = Git.cloneRepository()
				.setURI(config.gitUrl())
				.setDirectory(localDir)
				.setCredentialsProvider(getCredentialsProvider(config.token())) // 设置凭证
				.call()) {
			logger.info("Clone successful for {}. Repository cloned to: {}", config.gitUrl(),
					result.getRepository().getDirectory());
			return "Clone successful. Repository at: " + result.getRepository().getDirectory();
		} catch (GitAPIException e) {
			logger.error("JGit clone failed for {}: {}", config.gitUrl(), e.getMessage(), e);
			return "JGit clone failed: " + e.getMessage();
		}
	}

	public String pullRepository(RepositoryConfig config) {
		File repoDirFile = new File(config.localPath(), ".git"); // JGit需要指向.git目录或其父目录
		File workTree = repoDirFile.getParentFile();

		if (!repoDirFile.exists() || !repoDirFile.isDirectory()) {
			logger.warn("Repository at {} does not seem to exist or is not a git repository. Attempting clone first.",
					config.localPath());
			return cloneRepository(config);
		}

		logger.info("Pulling latest changes for repository at {} using JGit", config.localPath());
		// 使用 FileRepositoryBuilder 打开现有仓库
		try (Repository repository = new FileRepositoryBuilder().setGitDir(repoDirFile).readEnvironment().findGitDir()
				.build();
				Git git = new Git(repository)) {

			PullResult pullResult = git.pull()
					.setCredentialsProvider(getCredentialsProvider(config.token())) // 设置凭证
					.call();

			if (pullResult.isSuccessful()) {
				logger.info("JGit pull successful for {}. Merge status: {}", config.localPath(),
						pullResult.getMergeResult() != null ? pullResult.getMergeResult().getMergeStatus() : "N/A");
				return "JGit pull successful. Fetch result: " + pullResult.getFetchResult().getMessages() +
						(pullResult.getMergeResult() != null
								? " Merge status: " + pullResult.getMergeResult().getMergeStatus()
								: "");
			} else {
				logger.error("JGit pull not reported as successful for {}. Fetch result: {}, Merge result: {}",
						config.localPath(),
						pullResult.getFetchResult() != null ? pullResult.getFetchResult().getMessages() : "N/A",
						pullResult.getMergeResult() != null ? pullResult.getMergeResult().getMergeStatus() : "N/A");
				return "JGit pull not successful. Fetch: "
						+ (pullResult.getFetchResult() != null ? pullResult.getFetchResult().getMessages() : "N/A") +
						" Merge: "
						+ (pullResult.getMergeResult() != null ? pullResult.getMergeResult().getMergeStatus() : "N/A");
			}
		} catch (IOException e) {
			logger.error("JGit: Could not open repository at {}: {}", config.localPath(), e.getMessage(), e);
			return "JGit: Could not open repository: " + e.getMessage();
		} catch (GitAPIException e) {
			logger.error("JGit pull API exception for {}: {}", config.localPath(), e.getMessage(), e);
			return "JGit pull API exception: " + e.getMessage();
		}
	}

	// TODO: 使用JGit实现 push, add, commit 等其他Git操作
	// 例如:
	// public String addAndCommit(RepositoryConfig config, String commitMessage) {
	// ... }
	// public String pushRepository(RepositoryConfig config) { ... }
}