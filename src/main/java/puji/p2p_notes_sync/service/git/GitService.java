package puji.p2p_notes_sync.service.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.Status; // 新增导入 for checking if there are changes
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent; // 新增导入
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import puji.p2p_notes_sync.model.config.RepositoryConfig;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.stream.Collectors;

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

	private Repository openRepository(RepositoryConfig config) throws IOException {
		File repoDirFile = new File(config.localPath(), ".git");
		if (!repoDirFile.exists() || !repoDirFile.isDirectory()) {
			logger.error("Repository at {} does not seem to exist or is not a git repository.", config.localPath());
			throw new IOException("Git repository not found at " + config.localPath());
		}
		return new FileRepositoryBuilder().setGitDir(repoDirFile).readEnvironment().findGitDir().build();
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

	/**
	 * 添加所有更改、提交并推送到远程仓库。
	 *
	 * @param config        仓库配置
	 * @param commitMessage 提交信息
	 * @param authorName    提交者名称 (如果为null，JGit会尝试使用Git配置)
	 * @param authorEmail   提交者邮箱 (如果为null，JGit会尝试使用Git配置)
	 * @return 操作结果信息
	 */
	public String addCommitAndPush(RepositoryConfig config, String commitMessage, String authorName,
			String authorEmail) {
		try (Repository repository = openRepository(config);
				Git git = new Git(repository)) {

			// 1. 检查是否有更改
			Status status = git.status().call();
			if (status.isClean()) {
				logger.info("No changes to commit in repository {}", config.localPath());
				// 即使没有本地提交，也尝试推送，以防远程分支超前但本地没有拉取（尽管这通常意味着先pull）
				// 或者本地分支落后于远程，需要先pull。这里简化处理，若无更改也尝试推送。
				// return "No changes to commit. Attempting push for any upstream differences.";
			} else {
				// 2. 添加所有更改 (相当于 git add .)
				logger.info("Adding changes in repository {}", config.localPath());
				git.add().addFilepattern(".").call();

				// 3. 提交更改
				logger.info("Committing changes with message: '{}' in repository {}", commitMessage,
						config.localPath());
				PersonIdent author = (authorName != null && authorEmail != null)
						? new PersonIdent(authorName, authorEmail)
						: null;
				if (author != null) {
					git.commit().setMessage(commitMessage).setAuthor(author).setCommitter(author).call();
				} else {
					// JGit会尝试使用Git配置中的user.name和user.email
					git.commit().setMessage(commitMessage).call();
				}
				logger.info("Commit successful in repository {}", config.localPath());
			}

			// 4. 推送更改
			logger.info("Pushing changes for repository {}", config.localPath());
			Iterable<PushResult> pushResults = git.push()
					.setCredentialsProvider(getCredentialsProvider(config.token()))
					.call();

			StringBuilder pushResponse = new StringBuilder("Push results:\n");
			for (PushResult result : pushResults) {
				pushResponse.append(result.getMessages()).append("\n");
				result.getRemoteUpdates()
						.forEach(update -> pushResponse.append("  Update: ").append(update.toString()).append("\n"));
			}
			logger.info("Push command executed for {}. Response: {}", config.localPath(),
					pushResponse.toString().trim());
			return "Add, Commit successful.\n" + pushResponse.toString().trim();

		} catch (IOException e) {
			logger.error("JGit: Could not open or operate on repository at {}: {}", config.localPath(), e.getMessage(),
					e);
			return "JGit: Error with repository operation: " + e.getMessage();
		} catch (GitAPIException e) {
			logger.error("JGit API exception during add/commit/push for {}: {}", config.localPath(), e.getMessage(), e);
			return "JGit API exception: " + e.getMessage();
		}
	}
}