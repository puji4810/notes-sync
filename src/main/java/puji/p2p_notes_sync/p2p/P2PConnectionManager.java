package puji.p2p_notes_sync.p2p;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import puji.p2p_notes_sync.p2p.websocket.P2PWebSocketHandlerReactive;
import puji.p2p_notes_sync.p2p.discovery.MDNSService;

/**
 * 用于扩展P2PWebSocketHandlerReactive的辅助类
 * 提供用于前端界面的连接状态查询和管理方法
 */
public class P2PConnectionManager {
	private final P2PWebSocketHandlerReactive webSocketHandler;
	private final Set<String> manualPeers = ConcurrentHashMap.newKeySet();

	public P2PConnectionManager(P2PWebSocketHandlerReactive webSocketHandler) {
		this.webSocketHandler = webSocketHandler;
	}

	/**
	 * 获取已手动添加的节点
	 * 
	 * @return 手动添加的节点集合
	 */
	public Set<String> getManualPeers() {
		return Collections.unmodifiableSet(manualPeers);
	}

	/**
	 * 添加手动节点
	 * 
	 * @param address 节点地址
	 * @return 添加是否成功
	 */
	public boolean addManualPeer(String address) {
		return manualPeers.add(address);
	}

	/**
	 * 移除手动节点
	 * 
	 * @param address 节点地址
	 * @return 移除是否成功
	 */
	public boolean removeManualPeer(String address) {
		return manualPeers.remove(address);
	}

	/**
	 * 获取统计信息
	 * 
	 * @param mdnsService mDNS服务
	 * @return 节点统计信息
	 */
	public P2PStats getStats(MDNSService mdnsService) {
		P2PStats stats = new P2PStats();
		stats.setConnectedPeersCount(webSocketHandler.getConnectedPeers().size());
		stats.setDiscoveredPeersCount(mdnsService.getDiscoveredPeers().size());
		stats.setManualPeersCount(manualPeers.size());
		stats.setTotalPeersCount(
				webSocketHandler.getConnectedPeers().size() +
						mdnsService.getDiscoveredPeers().size() +
						manualPeers.size());
		return stats;
	}

	/**
	 * P2P统计信息类
	 */
	public static class P2PStats {
		private int connectedPeersCount;
		private int discoveredPeersCount;
		private int manualPeersCount;
		private int totalPeersCount;

		// Getters and setters
		public int getConnectedPeersCount() {
			return connectedPeersCount;
		}

		public void setConnectedPeersCount(int connectedPeersCount) {
			this.connectedPeersCount = connectedPeersCount;
		}

		public int getDiscoveredPeersCount() {
			return discoveredPeersCount;
		}

		public void setDiscoveredPeersCount(int discoveredPeersCount) {
			this.discoveredPeersCount = discoveredPeersCount;
		}

		public int getManualPeersCount() {
			return manualPeersCount;
		}

		public void setManualPeersCount(int manualPeersCount) {
			this.manualPeersCount = manualPeersCount;
		}

		public int getTotalPeersCount() {
			return totalPeersCount;
		}

		public void setTotalPeersCount(int totalPeersCount) {
			this.totalPeersCount = totalPeersCount;
		}
	}
}
