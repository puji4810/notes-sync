package puji.p2p_notes_sync.service;

import puji.p2p_notes_sync.config.RepositoryConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ConfigService {

	private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);
	private static final String CONFIG_DIR = "data";
	private static final String CONFIG_FILE_NAME = "repository_config.json";
	private Path configFilePath;

	private final ObjectMapper objectMapper;

	private List<RepositoryConfig> repositoryConfigs = new ArrayList<>();

	public ConfigService() {
		this.objectMapper = new ObjectMapper();
		this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		Path projectRoot = Paths.get("").toAbsolutePath();
		this.configFilePath = projectRoot.resolve(CONFIG_DIR).resolve(CONFIG_FILE_NAME);

		try {
			Files.createDirectories(this.configFilePath.getParent());
		} catch (IOException e) {
			logger.error("Could not create config directory: {}", this.configFilePath.getParent(), e);
		}
	}

	@PostConstruct
	public void init() {
		loadConfigs();
	}

	public synchronized void loadConfigs() {
		File configFile = configFilePath.toFile();
		if (configFile.exists() && configFile.length() > 0) { // 检查文件是否为空
			try {
				repositoryConfigs = objectMapper.readValue(configFile, new TypeReference<List<RepositoryConfig>>() {
				});
				logger.info("Configurations loaded successfully for {} repositories from {}", repositoryConfigs.size(),
						configFilePath);
			} catch (IOException e) {
				logger.error("Error loading configurations from {}: {}", configFilePath, e.getMessage(), e);
				repositoryConfigs = new ArrayList<>(); // 出错则使用空列表
				logger.warn("Falling back to empty configuration list.");
			}
		} else {
			logger.warn("Configuration file {} not found or is empty. Initializing with an empty list.",
					configFilePath);
			repositoryConfigs = new ArrayList<>();
			// saveConfigs(); // 首次可以不保存空列表，等用户添加
		}
	}

	private synchronized void saveConfigs() {
		try {
			objectMapper.writeValue(configFilePath.toFile(), repositoryConfigs);
			logger.info("Configurations saved successfully to {}", configFilePath);
		} catch (IOException e) {
			logger.error("Error saving configurations to {}: {}", configFilePath, e.getMessage(), e);
		}
	}

	public List<RepositoryConfig> getAllRepositoryConfigs() {
		return new ArrayList<>(repositoryConfigs); // 返回副本以防外部修改
	}

	public Optional<RepositoryConfig> getRepositoryConfigByAlias(String alias) {
		return repositoryConfigs.stream()
				.filter(config -> config.alias().equalsIgnoreCase(alias))
				.findFirst();
	}

	public synchronized boolean addRepositoryConfig(RepositoryConfig newRepoConfig) {
		if (newRepoConfig == null || newRepoConfig.alias() == null || newRepoConfig.alias().isBlank()) {
			logger.error("Attempted to add a repository with null or blank alias.");
			return false;
		}
		if (getRepositoryConfigByAlias(newRepoConfig.alias()).isPresent()) {
			logger.warn("Repository with alias '{}' already exists. Cannot add.", newRepoConfig.alias());
			return false; // 别名已存在
		}
		repositoryConfigs.add(newRepoConfig);
		saveConfigs();
		logger.info("Repository '{}' added.", newRepoConfig.alias());
		return true;
	}

	public synchronized boolean removeRepositoryConfig(String alias) {
		boolean removed = repositoryConfigs.removeIf(config -> config.alias().equalsIgnoreCase(alias));
		if (removed) {
			saveConfigs();
			logger.info("Repository '{}' removed.", alias);
		} else {
			logger.warn("Repository with alias '{}' not found. Cannot remove.", alias);
		}
		return removed;
	}

	public synchronized boolean updateRepositoryConfig(String alias, RepositoryConfig updatedRepoConfig) {
		if (updatedRepoConfig == null || updatedRepoConfig.alias() == null || updatedRepoConfig.alias().isBlank()) {
			logger.error("Updated repository config is invalid (null or blank alias).");
			return false;
		}
		if (!alias.equalsIgnoreCase(updatedRepoConfig.alias())
				&& getRepositoryConfigByAlias(updatedRepoConfig.alias()).isPresent()) {
			logger.warn("Cannot update repository alias to '{}' because it already exists.", updatedRepoConfig.alias());
			return false;
		}

		for (int i = 0; i < repositoryConfigs.size(); i++) {
			if (repositoryConfigs.get(i).alias().equalsIgnoreCase(alias)) {
				repositoryConfigs.set(i, updatedRepoConfig);
				saveConfigs();
				logger.info("Repository '{}' updated.", alias);
				return true;
			}
		}
		logger.warn("Repository with alias '{}' not found for update.", alias);
		return false;
	}
}