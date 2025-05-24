package puji.p2p_notes_sync.model.sync;

/**
 * 同步状态枚举
 */
public enum SyncState {
	/**
	 * 空闲状态
	 */
	IDLE,

	/**
	 * 同步中
	 */
	SYNCING,

	/**
	 * 同步成功
	 */
	SUCCESS,

	/**
	 * 同步失败
	 */
	FAILED,

	/**
	 * 存在冲突
	 */
	CONFLICT
}