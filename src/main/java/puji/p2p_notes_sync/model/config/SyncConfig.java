package puji.p2p_notes_sync.model.config;

import java.time.Duration;
import java.util.Set;
import puji.p2p_notes_sync.model.sync.SyncStrategy;
import puji.p2p_notes_sync.model.sync.ConflictResolution;

public record SyncConfig(
		SyncStrategy strategy,
		Duration syncInterval,
		ConflictResolution conflictResolution,
		Set<String> includePaths,
		Set<String> excludePaths,
		Set<String> excludeFileTypes,
		boolean syncOnStartup,
		boolean syncOnNetworkRecovery,
		int syncTimeout,
		int retryCount,
		boolean enableEncryption,
		boolean validateData) {

	public static SyncConfig defaultConfig() {
		return new SyncConfig(
				SyncStrategy.MANUAL,
				Duration.ofHours(1),
				ConflictResolution.AUTO_MERGE,
				Set.of(),
				Set.of(),
				Set.of(),
				false,
				false,
				300, // 5分钟超时
				3, // 3次重试
				false,
				true);
	}
}