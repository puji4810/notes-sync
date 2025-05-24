package puji.p2p_notes_sync.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import puji.p2p_notes_sync.model.config.RepositoryConfig;
import puji.p2p_notes_sync.model.sync.SyncStrategy;
import puji.p2p_notes_sync.model.sync.SyncState;
import puji.p2p_notes_sync.model.sync.SyncStatus;
import puji.p2p_notes_sync.p2p.coordinator.P2PCoordinatorService;
import puji.p2p_notes_sync.service.config.ConfigService;
import puji.p2p_notes_sync.service.git.GitService;
import puji.p2p_notes_sync.service.sync.SyncService;
import puji.p2p_notes_sync.service.sync.SyncStatusStorage;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SyncServiceImpl implements SyncService {
	private static final Logger logger = LoggerFactory.getLogger(SyncServiceImpl.class);

	private final ConfigService configService;
	private final GitService gitService;
	private final P2PCoordinatorService p2pCoordinatorService;
	private final SyncStatusStorage syncStatusStorage;
	private final Map<String, Disposable> autoSyncTasks = new ConcurrentHashMap<>();

	@Autowired
	public SyncServiceImpl(ConfigService configService, GitService gitService,
			P2PCoordinatorService p2pCoordinatorService, SyncStatusStorage syncStatusStorage) {
		this.configService = configService;
		this.gitService = gitService;
		this.p2pCoordinatorService = p2pCoordinatorService;
		this.syncStatusStorage = syncStatusStorage;
	}

	@Override
	public Mono<SyncStatus> syncRepository(String repoAlias) {
		return Mono.fromCallable(() -> {
			RepositoryConfig config = configService.getRepositoryConfigByAlias(repoAlias)
					.orElseThrow(() -> new RuntimeException("Repository not found"));

			// 更新同步状态为进行中
			SyncStatus syncingStatus = new SyncStatus(
					repoAlias,
					LocalDateTime.now(),
					SyncState.SYNCING,
					null,
					0,
					0);
			syncStatusStorage.updateStatus(syncingStatus);

			try {
				// 执行同步
				String result = gitService.pullRepository(config);

				// 广播同步请求
				p2pCoordinatorService.broadcastSyncRequest(repoAlias);

				// 更新同步状态为成功
				SyncStatus successStatus = new SyncStatus(
						repoAlias,
						LocalDateTime.now(),
						SyncState.SUCCESS,
						null,
						0, // TODO: 实现文件计数
						0);
				syncStatusStorage.updateStatus(successStatus);
				return successStatus;
			} catch (Exception e) {
				// 更新同步状态为失败
				SyncStatus failedStatus = new SyncStatus(
						repoAlias,
						LocalDateTime.now(),
						SyncState.FAILED,
						e.getMessage(),
						0,
						0);
				syncStatusStorage.updateStatus(failedStatus);
				throw e;
			}
		}).subscribeOn(Schedulers.boundedElastic());
	}

	@Override
	public Mono<SyncStatus> getSyncStatus(String repoAlias) {
		return Mono.fromCallable(() -> syncStatusStorage.getCurrentStatus(repoAlias))
				.subscribeOn(Schedulers.boundedElastic());
	}

	@Override
	public Flux<SyncStatus> getSyncHistory(String repoAlias, LocalDateTime startTime, LocalDateTime endTime) {
		return Flux.fromIterable(syncStatusStorage.getHistory(repoAlias, startTime, endTime))
				.subscribeOn(Schedulers.boundedElastic());
	}

	@Override
	public void startAutoSync(String repoAlias) {
		RepositoryConfig config = configService.getRepositoryConfigByAlias(repoAlias)
				.orElseThrow(() -> new RuntimeException("Repository not found"));

		if (config.syncConfig().strategy() == SyncStrategy.SCHEDULED) {
			Disposable task = Flux.interval(config.syncConfig().syncInterval())
					.subscribe(tick -> syncRepository(repoAlias).subscribe());
			autoSyncTasks.put(repoAlias, task);
		}
	}

	@Override
	public void stopAutoSync(String repoAlias) {
		Disposable task = autoSyncTasks.remove(repoAlias);
		if (task != null) {
			task.dispose();
		}
	}
}