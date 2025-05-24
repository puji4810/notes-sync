package puji.p2p_notes_sync.model.sync;

public enum ConflictResolution {
	AUTO_MERGE, // 自动合并
	KEEP_LOCAL, // 保留本地
	KEEP_REMOTE, // 保留远程
	MANUAL // 手动解决
}