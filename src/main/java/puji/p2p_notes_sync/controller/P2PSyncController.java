package puji.p2p_notes_sync.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import puji.p2p_notes_sync.model.config.RepositoryConfig;
import puji.p2p_notes_sync.p2p.coordinator.P2PCoordinatorService;
import puji.p2p_notes_sync.service.config.ConfigService;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/p2p-sync")
@Tag(name = "P2P同步管理", description = "用于管理P2P网络上的笔记同步")
public class P2PSyncController {

	private final P2PCoordinatorService p2pCoordinatorService;
	private final ConfigService configService;

	@Autowired
	public P2PSyncController(P2PCoordinatorService p2pCoordinatorService, ConfigService configService) {
		this.p2pCoordinatorService = p2pCoordinatorService;
		this.configService = configService;
	}

	@Operation(summary = "广播新仓库配置", description = "向P2P网络中的其他节点广播新添加的仓库配置")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "广播成功", content = @Content(mediaType = "text/plain")),
			@ApiResponse(responseCode = "404", description = "未找到指定别名的仓库", content = @Content(mediaType = "text/plain"))
	})
	@PostMapping("/broadcast/new/{repoAlias}")
	public Mono<ResponseEntity<String>> broadcastNewRepository(
			@Parameter(description = "要广播的仓库别名", required = true) @PathVariable String repoAlias) {
		return Mono.fromCallable(() -> {
			return configService.getRepositoryConfigByAlias(repoAlias)
					.map(config -> {
						p2pCoordinatorService.broadcastNewRepositoryConfiguration(config);
						return ResponseEntity.ok("仓库配置广播成功");
					})
					.orElse(ResponseEntity.notFound().build());
		}).subscribeOn(Schedulers.boundedElastic());
	}

	@Operation(summary = "广播更新仓库配置", description = "向P2P网络中的其他节点广播更新后的仓库配置")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "广播成功", content = @Content(mediaType = "text/plain")),
			@ApiResponse(responseCode = "404", description = "未找到指定别名的仓库", content = @Content(mediaType = "text/plain"))
	})
	@PostMapping("/broadcast/update/{oldRepoAlias}")
	public Mono<ResponseEntity<String>> broadcastUpdateRepository(
			@Parameter(description = "更新前的仓库别名", required = true) @PathVariable String oldRepoAlias,
			@Parameter(description = "更新后的仓库别名", required = true) @RequestParam String newRepoAlias) {
		return Mono.fromCallable(() -> {
			return configService.getRepositoryConfigByAlias(newRepoAlias)
					.map(config -> {
						p2pCoordinatorService.broadcastUpdateRepositoryConfiguration(oldRepoAlias, config);
						return ResponseEntity.ok("仓库配置更新广播成功");
					})
					.orElse(ResponseEntity.notFound().build());
		}).subscribeOn(Schedulers.boundedElastic());
	}

	@Operation(summary = "广播删除仓库配置", description = "向P2P网络中的其他节点广播已删除的仓库配置")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "广播成功", content = @Content(mediaType = "text/plain"))
	})
	@PostMapping("/broadcast/remove/{repoAlias}")
	public Mono<ResponseEntity<String>> broadcastRemoveRepository(
			@Parameter(description = "要删除的仓库别名", required = true) @PathVariable String repoAlias) {
		return Mono.fromCallable(() -> {
			p2pCoordinatorService.broadcastRemovedRepositoryConfiguration(repoAlias);
			return ResponseEntity.ok("仓库删除广播成功");
		}).subscribeOn(Schedulers.boundedElastic());
	}

	@Operation(summary = "广播同步请求", description = "向P2P网络中的其他节点广播同步请求")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "同步请求广播成功", content = @Content(mediaType = "text/plain"))
	})
	@PostMapping("/broadcast/sync-request/{repoAlias}")
	public Mono<ResponseEntity<String>> broadcastSyncRequest(
			@Parameter(description = "要同步的仓库别名", required = true) @PathVariable String repoAlias) {
		return Mono.fromCallable(() -> {
			p2pCoordinatorService.broadcastSyncRequest(repoAlias);
			return ResponseEntity.ok("同步请求广播成功");
		}).subscribeOn(Schedulers.boundedElastic());
	}
}