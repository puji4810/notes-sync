package puji.p2p_notes_sync.service.sync;

import puji.p2p_notes_sync.model.sync.SyncStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

public interface SyncService {
	Mono<SyncStatus> syncRepository(String repoAlias);

	Mono<SyncStatus> getSyncStatus(String repoAlias);

	Flux<SyncStatus> getSyncHistory(String repoAlias, LocalDateTime startTime, LocalDateTime endTime);

	void startAutoSync(String repoAlias);

	void stopAutoSync(String repoAlias);
}