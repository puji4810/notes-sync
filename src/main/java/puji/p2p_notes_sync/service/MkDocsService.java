package puji.p2p_notes_sync.service;

import puji.p2p_notes_sync.config.RepositoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
public class MkDocsService {

	private static final Logger logger = LoggerFactory.getLogger(MkDocsService.class);

	// 可以复用GitService中的executeCommand方法，或者在这里写一个类似的
	private ProcessResult executeCommand(File workingDirectory, String... command)
			throws IOException, InterruptedException {
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		if (workingDirectory != null && workingDirectory.exists() && workingDirectory.isDirectory()) {
			processBuilder.directory(workingDirectory);
		} else {
			logger.error("Working directory for MkDocs ({}) is invalid.", workingDirectory);
			return new ProcessResult(-1, "Invalid working directory for MkDocs: " + workingDirectory);
		}
		processBuilder.redirectErrorStream(true);

		Process process = processBuilder.start();
		StringBuilder output = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append(System.lineSeparator());
				logger.debug("MkDocs CMD Output: {}", line);
			}
		}
		boolean exited = process.waitFor(120, TimeUnit.SECONDS); // MkDocs构建可能需要更长时间
		int exitCode = -1;
		if (exited) {
			exitCode = process.exitValue();
		} else {
			process.destroyForcibly();
			logger.error("MkDocs command timed out: {}", String.join(" ", command));
			throw new InterruptedException("MkDocs command timed out.");
		}
		return new ProcessResult(exitCode, output.toString());
	}

	private record ProcessResult(int exitCode, String output) {
	}

	public String buildSite(RepositoryConfig config) {
		File repoDir = new File(config.localPath());
		if (!repoDir.exists() || !repoDir.isDirectory()) {
			logger.error("Cannot build MkDocs site, repository directory {} does not exist.", config.localPath());
			return "Repository directory not found.";
		}

		// 检查mkdocs.yml是否存在
		File mkdocsYaml = new File(repoDir, "mkdocs.yml");
		if (!mkdocsYaml.exists()) {
			logger.warn(
					"mkdocs.yml not found in {}. Site might not build correctly or default will be used if mkdocs supports it.",
					config.localPath());
			// 您可以在这里添加逻辑来创建或提示用户创建一个mkdocs.yml
			// 例如，由LLM辅助生成并写入文件
		}

		try {
			logger.info("Building MkDocs site for repository at {}", config.localPath());
			ProcessResult result = executeCommand(repoDir, "mkdocs", "build"); // 确保mkdocs命令在系统PATH中
			if (result.exitCode() == 0) {
				Path sitePath = Paths.get(config.localPath(), "site");
				logger.info("MkDocs site built successfully at {}", sitePath);
				return "MkDocs site built successfully at " + sitePath + "\nOutput:\n" + result.output();
			} else {
				logger.error("MkDocs build failed for {}. Exit code: {}. Output: {}", config.localPath(),
						result.exitCode(), result.output());
				return "MkDocs build failed. Exit code: " + result.exitCode() + "\nOutput:\n" + result.output();
			}
		} catch (IOException | InterruptedException e) {
			logger.error("Error during MkDocs build for {}: {}", config.localPath(), e.getMessage(), e);
			Thread.currentThread().interrupt();
			return "Error during MkDocs build: " + e.getMessage();
		}
	}
}
