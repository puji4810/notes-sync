package puji.p2p_notes_sync.controller;

import puji.p2p_notes_sync.service.config.ConfigService;
import puji.p2p_notes_sync.model.config.RepositoryConfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Swagger Imports
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/settings")
@CrossOrigin(origins = "*")
@Tag(name = "系统设置", description = "管理应用程序的系统设置和系统信息")
public class SettingsController {

	private final ConfigService configService;

	// 从application.properties读取配置值
	@Value("${server.port:8080}")
	private int serverPort;

	@Value("${app.p2p.enabled:true}")
	private boolean p2pEnabled;

	@Value("${app.mdns.enabled:true}")
	private boolean mdnsEnabled;

	@Value("${app.service.name:notes-sync}")
	private String serviceName;

	public SettingsController(ConfigService configService) {
		this.configService = configService;
	}

	@Operation(summary = "获取所有系统设置，包括P2P和系统信息")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "成功获取设置", content = @Content(schema = @Schema(implementation = Map.class))),
			@ApiResponse(responseCode = "500", description = "服务器内部错误")
	})
	@GetMapping
	public Mono<ResponseEntity<Map<String, Object>>> getSettings() {
		return Mono.fromCallable(() -> {
			Map<String, Object> settings = new HashMap<>();

			// P2P设置（从配置文件读取）
			Map<String, Object> p2pSettings = new HashMap<>();
			p2pSettings.put("p2pEnabled", p2pEnabled);
			p2pSettings.put("mdnsEnabled", mdnsEnabled);
			p2pSettings.put("serverPort", serverPort);
			p2pSettings.put("serviceName", serviceName);
			settings.put("p2p", p2pSettings);

			// 仓库概览信息
			List<RepositoryConfig> repositories = configService.getAllRepositoryConfigs();
			Map<String, Object> repositorySettings = new HashMap<>();
			repositorySettings.put("repositoryCount", repositories.size());
			settings.put("repositories", repositorySettings);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("settings", settings);
			return ResponseEntity.ok(response);
		}).subscribeOn(Schedulers.boundedElastic())
				.onErrorResume(throwable -> {
					Map<String, Object> errorResponse = new HashMap<>();
					errorResponse.put("success", false);
					errorResponse.put("error", "Failed to retrieve settings: " + throwable.getMessage());
					return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
				});
	}

	@Operation(summary = "获取系统信息，包括Java版本、操作系统、内存等")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "成功获取系统信息", content = @Content(schema = @Schema(implementation = Map.class))),
			@ApiResponse(responseCode = "500", description = "服务器内部错误")
	})
	@GetMapping("/system-info")
	public Mono<ResponseEntity<Map<String, Object>>> getSystemInfo() {
		return Mono.fromCallable(() -> {
			Map<String, Object> systemInfo = new HashMap<>();
			// 基本系统信息
			systemInfo.put("version", "1.0.0");
			systemInfo.put("javaVersion", System.getProperty("java.version"));
			systemInfo.put("osName", System.getProperty("os.name"));
			systemInfo.put("osVersion", System.getProperty("os.version"));
			systemInfo.put("osArch", System.getProperty("os.arch"));
			systemInfo.put("availableProcessors", Runtime.getRuntime().availableProcessors());

			// 内存信息
			systemInfo.put("maxMemory", Runtime.getRuntime().maxMemory());
			systemInfo.put("totalMemory", Runtime.getRuntime().totalMemory());
			systemInfo.put("freeMemory", Runtime.getRuntime().freeMemory());

			// 磁盘信息
			Map<String, Long> diskInfo = getDiskSpaceInfo();
			systemInfo.putAll(diskInfo);

			// 运行时间
			systemInfo.put("uptime", getSystemUptime());

			// 仓库信息
			systemInfo.put("repositoryCount", configService.getAllRepositoryConfigs().size());
			// 添加系统运行时间
			systemInfo.put("uptime", getSystemUptime());

			// 添加磁盘空间信息
			Map<String, Long> diskSpaceInfo = getDiskSpaceInfo();
			systemInfo.put("diskSpace", diskSpaceInfo);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("systemInfo", systemInfo);
			return ResponseEntity.ok(response);
		}).subscribeOn(Schedulers.boundedElastic())
				.onErrorResume(throwable -> {
					Map<String, Object> errorResponse = new HashMap<>();
					errorResponse.put("success", false);
					errorResponse.put("error", "Failed to retrieve system info: " + throwable.getMessage());
					return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
				});
	}

	@Operation(summary = "更新系统设置")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "设置更新成功", content = @Content(schema = @Schema(implementation = Map.class))),
			@ApiResponse(responseCode = "500", description = "服务器内部错误")
	})
	@PutMapping
	public Mono<ResponseEntity<Map<String, Object>>> updateSettings(
			@RequestBody(description = "要更新的设置内容", required = true) Map<String, Object> settingsUpdate) {
		return Mono.fromCallable(() -> {
			Map<String, Object> response = new HashMap<>();

			try {
				// 这里可以添加设置更新逻辑
				// 目前仅返回成功响应，实际应用中可以扩展为真正的设置保存

				// 示例：如何从settingsUpdate中获取值
				// String appName = (String) settingsUpdate.get("appName");
				// int syncInterval = (Integer) settingsUpdate.get("syncInterval");

				response.put("success", true);
				response.put("message", "Settings updated successfully");
				response.put("settings", settingsUpdate);

				return ResponseEntity.ok(response);
			} catch (Exception e) {
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("success", false);
				errorResponse.put("error", "Failed to update settings: " + e.getMessage());
				return ResponseEntity.internalServerError().body(errorResponse);
			}
		}).subscribeOn(Schedulers.boundedElastic())
				.onErrorResume(throwable -> {
					Map<String, Object> errorResponse = new HashMap<>();
					errorResponse.put("success", false);
					errorResponse.put("error", "Failed to update settings: " + throwable.getMessage());
					return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
				});
	}

	/**
	 * 获取系统运行时间（以毫秒为单位）
	 * 
	 * @return 系统运行时间
	 */
	private long getSystemUptime() {
		try {
			// 获取Java虚拟机启动时间
			return java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
		} catch (Exception e) {
			return 0;
		}
	}

	/**
	 * 获取磁盘空间信息
	 * 
	 * @return 包含总磁盘空间、已用空间和可用空间的Map
	 */
	private Map<String, Long> getDiskSpaceInfo() {
		Map<String, Long> diskInfo = new HashMap<>();
		try {
			// 获取根目录（应用运行目录）的磁盘空间信息
			java.io.File root = new java.io.File(".");
			java.nio.file.FileStore store = java.nio.file.Files.getFileStore(root.toPath());

			// 总空间
			long totalSpace = store.getTotalSpace();
			// 可用空间
			long freeSpace = store.getUsableSpace();
			// 已使用空间
			long usedSpace = totalSpace - freeSpace;

			// 使用前端期望的字段名称
			diskInfo.put("totalDisk", totalSpace);
			diskInfo.put("freeDisk", freeSpace);
			diskInfo.put("usedDisk", usedSpace);
		} catch (Exception e) {
			// 如果发生错误，设置默认值为0
			diskInfo.put("totalDisk", 0L);
			diskInfo.put("freeDisk", 0L);
			diskInfo.put("usedDisk", 0L);
		}
		return diskInfo;
	}
}