package puji.p2p_notes_sync.model.sync;

public enum SyncStrategy {
	MANUAL, // 手动同步
	SCHEDULED, // 定时同步
	ON_CHANGE, // 文件变更时同步
	ON_STARTUP, // 启动时同步
	ON_NETWORK // 网络恢复时同步
}