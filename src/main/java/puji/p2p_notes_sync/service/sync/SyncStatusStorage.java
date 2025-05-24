package puji.p2p_notes_sync.service.sync;

import org.springframework.stereotype.Service;
import puji.p2p_notes_sync.model.sync.SyncState;
import puji.p2p_notes_sync.model.sync.SyncStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 同步状态存储服务
 */
@Service
public class SyncStatusStorage {
	private final Map<String, SyncStatus> currentStatus = new ConcurrentHashMap<>();
	private final Map<String, List<SyncStatus>> syncHistory = new ConcurrentHashMap<>();

	/**
	 * 更新同步状态
	 * 
	 * @param status 新的同步状态
	 */
	public void updateStatus(SyncStatus status) {
		currentStatus.put(status.repoAlias(), status);
		syncHistory.computeIfAbsent(status.repoAlias(), k -> new ArrayList<>())
				.add(status);
	}

	/**
	 * 获取当前同步状态
	 * 
	 * @param repoAlias 仓库别名
	 * @return 当前同步状态
	 */
	public SyncStatus getCurrentStatus(String repoAlias) {
		return currentStatus.getOrDefault(repoAlias,
				new SyncStatus(repoAlias, LocalDateTime.now(), SyncState.IDLE, null, 0, 0));
	}

	/**
	 * 获取同步历史记录
	 * 
	 * @param repoAlias 仓库别名
	 * @param startTime 开始时间
	 * @param endTime   结束时间
	 * @return 同步历史记录列表
	 */
	public List<SyncStatus> getHistory(String repoAlias, LocalDateTime startTime, LocalDateTime endTime) {
		List<SyncStatus> history = syncHistory.getOrDefault(repoAlias, new ArrayList<>());
		return history.stream()
				.filter(status -> !status.lastSyncTime().isBefore(startTime) &&
						!status.lastSyncTime().isAfter(endTime))
				.collect(Collectors.toList());
	}
}