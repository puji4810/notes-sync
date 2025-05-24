package puji.p2p_notes_sync.model.sync;

import java.time.LocalDateTime;

/**
 * 同步状态记录
 */
public record SyncStatus(
		/**
		 * 仓库别名
		 */
		String repoAlias,

		/**
		 * 最后同步时间
		 */
		LocalDateTime lastSyncTime,

		/**
		 * 同步状态
		 */
		SyncState state,

		/**
		 * 最后的错误信息
		 */
		String lastError,

		/**
		 * 已同步文件数
		 */
		long syncedFiles,

		/**
		 * 总文件数
		 */
		long totalFiles) {
}