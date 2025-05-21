package puji.p2p_notes_sync.controller;

import puji.p2p_notes_sync.config.RepositoryConfig;
import puji.p2p_notes_sync.p2p.P2PCoordinatorService;
import puji.p2p_notes_sync.service.ConfigService;
import puji.p2p_notes_sync.service.GitService;
import puji.p2p_notes_sync.service.MkDocsService;
import puji.p2p_notes_sync.util.ResponseEntityUtil; // 您创建的工具类

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

// Swagger/OpenAPI 注解导入
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody; // 注意区分Spring的@RequestBody和Swagger的
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/repositories")
@Tag(name = "笔记仓库管理 (Repositories)", description = "用于管理和操作用户笔记Git仓库的API接口") // 类级别Tag
public class RepositoryController {

	private final ConfigService configService;
	private final GitService gitService;
	private final MkDocsService mkDocsService;
	private final P2PCoordinatorService p2pCoordinatorService; // P2P服务

	@Autowired
	public RepositoryController(ConfigService configService, GitService gitService, MkDocsService mkDocsService,
			P2PCoordinatorService p2pCoordinatorService) {
		this.configService = configService;
		this.gitService = gitService;
		this.mkDocsService = mkDocsService;
		this.p2pCoordinatorService = p2pCoordinatorService;
	}

	@Operation(summary = "获取所有已配置的笔记仓库列表", description = "返回一个包含所有已注册笔记仓库配置的列表。")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "成功获取仓库列表", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RepositoryConfig[].class))) // 表示返回RepositoryConfig数组
	})
	@GetMapping
	public Flux<RepositoryConfig> getAllRepositories() {
		return Flux.fromIterable(configService.getAllRepositoryConfigs());
	}

	@Operation(summary = "添加一个新的笔记仓库配置", description = "注册一个新的Git仓库供应用管理。别名在用户配置中应唯一。")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "仓库配置成功创建并返回", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RepositoryConfig.class))),
			@ApiResponse(responseCode = "400", description = "请求参数错误或仓库别名已存在", content = @Content) // 没有特定schema的空响应体
	})
	@PostMapping
	public Mono<ResponseEntity<RepositoryConfig>> addRepository(
			@RequestBody(description = "新的仓库配置信息。`token`字段用于在此设备本地进行安全存储和后续的Git操作。", required = true, content = @Content(schema = @Schema(implementation = RepositoryConfig.class)))
			// 下面这个@org.springframework.web.bind.annotation.RequestBody是Spring
			// MVC/WebFlux的注解，用于参数绑定
			@org.springframework.web.bind.annotation.RequestBody RepositoryConfig newRepoConfig) {
		return Mono.fromCallable(() -> {
			boolean success = configService.addRepositoryConfig(newRepoConfig);
			if (success) {
				// 广播新仓库配置到其他节点
				p2pCoordinatorService.broadcastNewRepositoryConfiguration(newRepoConfig);
				return ResponseEntity.status(HttpStatus.CREATED).body(newRepoConfig);
			} else {
				return ResponseEntityUtil.<RepositoryConfig>badRequest();
			}
		}).subscribeOn(Schedulers.boundedElastic());
	}

	@Operation(summary = "获取指定别名的笔记仓库配置", description = "根据提供的仓库别名查找并返回其详细配置信息。")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "成功找到并返回仓库配置", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RepositoryConfig.class))),
			@ApiResponse(responseCode = "404", description = "未找到具有指定别名的仓库", content = @Content)
	})
	@GetMapping("/{repoAlias}")
	public Mono<ResponseEntity<RepositoryConfig>> getRepositoryByAlias(
			@Parameter(description = "要查询的仓库别名", required = true, example = "my-work-notes") @PathVariable String repoAlias) {
		return Mono.justOrEmpty(configService.getRepositoryConfigByAlias(repoAlias))
				.map(config -> ResponseEntity.ok(config))
				.defaultIfEmpty(ResponseEntityUtil.<RepositoryConfig>notFound());
	}

	@Operation(summary = "更新指定别名的笔记仓库配置", description = "修改已存在的笔记仓库的配置信息。如果别名被修改，请确保新别名未被其他仓库占用。")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "仓库配置成功更新并返回", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RepositoryConfig.class))),
			@ApiResponse(responseCode = "404", description = "未找到具有指定别名的仓库进行更新", content = @Content),
			@ApiResponse(responseCode = "400", description = "更新请求不合法（例如，新别名冲突）", content = @Content)
	})
	@PutMapping("/{repoAlias}")
	public Mono<ResponseEntity<RepositoryConfig>> updateRepository(
			@Parameter(description = "要更新的仓库的当前别名", required = true, example = "old-alias") @PathVariable String repoAlias,
			@RequestBody(description = "更新后的仓库配置信息。如果`alias`字段与URL中的`repoAlias`不同，则表示尝试修改别名。", required = true, content = @Content(schema = @Schema(implementation = RepositoryConfig.class))) @org.springframework.web.bind.annotation.RequestBody RepositoryConfig updatedRepoConfig) {
		return Mono.fromCallable(() -> {
			boolean success = configService.updateRepositoryConfig(repoAlias, updatedRepoConfig);
			if (success) {
				// 更新成功后，广播更新配置到其他节点
				p2pCoordinatorService.broadcastUpdateRepositoryConfiguration(repoAlias, updatedRepoConfig);
				return ResponseEntity.ok(updatedRepoConfig);
			} else {
				// 根据updateRepositoryConfig的内部逻辑，失败可能是404或400
				// 为了简化，如果configService不抛出特定异常来区分，我们这里统一用404或400
				// 假设 updateRepositoryConfig 如果找不到旧别名会返回false, 如果新别名冲突也返回false
				if (configService.getRepositoryConfigByAlias(repoAlias).isEmpty()) {
					return ResponseEntityUtil.<RepositoryConfig>notFound();
				} else {
					return ResponseEntityUtil.<RepositoryConfig>badRequest(); // 例如，新别名冲突
				}
			}
		}).subscribeOn(Schedulers.boundedElastic());
	}

	@Operation(summary = "删除指定别名的笔记仓库配置", description = "从应用中移除对某个笔记仓库的管理，本地文件不会被删除。")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "仓库配置成功删除 (无返回内容)"),
			@ApiResponse(responseCode = "404", description = "未找到具有指定别名的仓库进行删除", content = @Content)
	})
	@DeleteMapping("/{repoAlias}")
	public Mono<ResponseEntity<Void>> removeRepository(
			@Parameter(description = "要删除的仓库别名", required = true, example = "obsolete-notes") @PathVariable String repoAlias) {
		return Mono.fromCallable(() -> {
			boolean success = configService.removeRepositoryConfig(repoAlias);
			if (success) {
				// 删除成功后，广播删除配置到其他节点
				p2pCoordinatorService.broadcastRemovedRepositoryConfiguration(repoAlias);
			}
			return success;
		})
				.subscribeOn(Schedulers.boundedElastic())
				.flatMap(success -> success
						? Mono.just(ResponseEntity.noContent().<Void>build())
						: Mono.just(ResponseEntityUtil.<Void>notFound()));
	}

	@Operation(summary = "同步指定别名的笔记仓库", description = "对指定的笔记仓库执行`git pull`操作，从远程拉取最新更改。如果本地仓库不存在，会先尝试`git clone`。")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "同步操作已成功发起或完成，返回Git操作的输出信息", content = @Content(mediaType = "text/plain")), // Git输出通常是文本
			@ApiResponse(responseCode = "404", description = "未找到具有指定别名的仓库", content = @Content(mediaType = "text/plain"))
	})
	@PostMapping("/{repoAlias}/sync")
	public Mono<ResponseEntity<String>> syncRepository(
			@Parameter(description = "要同步的仓库别名", required = true, example = "my-work-notes") @PathVariable String repoAlias) {
		return Mono.justOrEmpty(configService.getRepositoryConfigByAlias(repoAlias))
				.flatMap(config -> Mono.fromCallable(() -> {
					String localSyncResult = gitService.pullRepository(config); // 或其他同步方法
					// 本地同步后，广播P2P同步请求
					if (localSyncResult.toLowerCase().contains("successful")
							|| !localSyncResult.toLowerCase().contains("fail")) { // 简单判断成功
						// p2pCoordinatorService.broadcastSyncRequest(config.alias()); // 使用别名或URL
						// 这里认为不需要同步，每个用户的别名是可以不同的
					}
					return localSyncResult;
				})
						.subscribeOn(Schedulers.boundedElastic())
						.map(resultMessage -> ResponseEntity.ok(resultMessage)))
				.defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("Repository with alias '" + repoAlias + "' not found."));
	}

	// clone, deploy 端点也可以用类似方式添加 @Operation, @Parameter, @ApiResponses

	@Operation(summary = "克隆指定的笔记仓库到本地", description = "如果本地尚不存在该仓库的副本，则从远程URL克隆。")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "克隆操作成功，返回Git操作信息", content = @Content(mediaType = "text/plain")),
			@ApiResponse(responseCode = "400", description = "仓库配置中Git URL缺失", content = @Content(mediaType = "text/plain")),
			@ApiResponse(responseCode = "404", description = "未找到仓库配置", content = @Content(mediaType = "text/plain"))
	})
	@PostMapping("/{repoAlias}/clone")
	public Mono<ResponseEntity<String>> cloneRepository(
			@Parameter(description = "要克隆的仓库别名", required = true, example = "new-project-notes") @PathVariable String repoAlias) {
		return Mono.justOrEmpty(configService.getRepositoryConfigByAlias(repoAlias))
				.flatMap(config -> {
					if (config.gitUrl() == null || config.gitUrl().isBlank()) {
						return Mono.just(ResponseEntity.badRequest()
								.body("Git URL is not configured for repository '" + repoAlias + "'."));
					}
					return Mono.fromCallable(() -> gitService.cloneRepository(config))
							.subscribeOn(Schedulers.boundedElastic())
							.map(resultMessage -> ResponseEntity.ok(resultMessage));
				})
				.defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("Repository with alias '" + repoAlias + "' not found."));
	}

	@Operation(summary = "提交并推送指定仓库的本地更改", description = "将本地所有未提交的更改添加到暂存区，使用提供的消息进行提交，然后将提交推送到远程仓库。")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "提交和推送操作成功执行，返回Git操作的输出信息", content = @Content(mediaType = "text/plain")),
			@ApiResponse(responseCode = "400", description = "请求体无效（例如缺少提交信息）", content = @Content(mediaType = "text/plain")),
			@ApiResponse(responseCode = "404", description = "未找到具有指定别名的仓库", content = @Content(mediaType = "text/plain")),
			@ApiResponse(responseCode = "500", description = "Git操作过程中发生错误", content = @Content(mediaType = "text/plain"))
	})
	@PostMapping("/{repoAlias}/commit-push")
	public Mono<ResponseEntity<String>> commitAndPushRepository(
			@Parameter(description = "要操作的仓库别名", required = true, example = "my-work-notes") @PathVariable String repoAlias,
			@RequestBody(description = "包含提交信息的请求体。例如：{\"commitMessage\": \"My daily updates\", \"authorName\": \"Puji\", \"authorEmail\": \"puji@example.com\"} (authorName 和 authorEmail 可选)", required = true, content = @Content(schema = @Schema(type = "object", example = "{\"commitMessage\": \"My daily updates\", \"authorName\": \"Optional Author Name\", \"authorEmail\": \"optional@example.com\"}"))) @org.springframework.web.bind.annotation.RequestBody Map<String, String> payload) { // 使用Map接收简单JSON

		String commitMessage = payload.get("commitMessage");
		if (commitMessage == null || commitMessage.isBlank()) {
			return Mono.just(ResponseEntity.badRequest().body("Commit message is required."));
		}
		// authorName 和 authorEmail 是可选的
		String authorName = payload.get("authorName");
		String authorEmail = payload.get("authorEmail");

		return Mono.justOrEmpty(configService.getRepositoryConfigByAlias(repoAlias))
				.flatMap(config -> Mono
						.fromCallable(() -> gitService.addCommitAndPush(config, commitMessage, authorName, authorEmail))
						.subscribeOn(Schedulers.boundedElastic())
						.map(resultMessage -> {
							if (resultMessage.startsWith("JGit API exception")
									|| resultMessage.startsWith("JGit: Error with repository operation")) {
								return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resultMessage);
							}
							return ResponseEntity.ok(resultMessage);
						}))
				.defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("Repository with alias '" + repoAlias + "' not found."));
	}

	@Operation(summary = "将指定仓库部署为MkDocs静态网站", description = "使用MkDocs构建指定笔记仓库的内容，并使其可通过特定URL访问。")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "MkDocs站点构建成功，返回相关信息或访问URL", content = @Content(mediaType = "text/plain")),
			@ApiResponse(responseCode = "404", description = "未找到仓库配置", content = @Content(mediaType = "text/plain")),
			@ApiResponse(responseCode = "500", description = "MkDocs构建过程中发生错误", content = @Content(mediaType = "text/plain"))
	})
	@PostMapping("/{repoAlias}/deploy")
	public Mono<ResponseEntity<String>> deployRepository(
			@Parameter(description = "要部署的仓库别名", required = true, example = "my-published-notes") @PathVariable String repoAlias) {
		// 注意：这里deploy的请求体中可能包含mkdocs的配置建议 (来自LLM或用户)
		// 我们之前的deployNotesToWebTool (LLM工具) 包含mkdocsConfigOverrides
		// 如果API也需要这个，需要在方法参数中添加 @org.springframework.web.bind.annotation.RequestBody
		// Map<String, Object> mkdocsConfigOverrides
		// 并传递给 mkDocsService.buildSite() (如果buildSite支持这个参数的话)
		// 为简化，这里暂时不包含mkdocs配置覆盖
		return Mono.justOrEmpty(configService.getRepositoryConfigByAlias(repoAlias))
				.flatMap(config -> Mono.fromCallable(() -> mkDocsService.buildSite(config)) // 假设mkDocsService.buildSite返回可访问URL或成功消息
						.subscribeOn(Schedulers.boundedElastic())
						.map(resultMessage -> ResponseEntity.ok(resultMessage)))
				.defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("Repository with alias '" + repoAlias + "' not found."));
	}
}