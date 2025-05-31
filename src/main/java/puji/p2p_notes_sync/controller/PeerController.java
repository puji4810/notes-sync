package puji.p2p_notes_sync.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import puji.p2p_notes_sync.p2p.discovery.MDNSService;
import puji.p2p_notes_sync.p2p.websocket.P2PWebSocketHandlerReactive;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/peers")
@Tag(name = "P2P对等节点管理", description = "用于发现和管理P2P网络中的对等节点")
public class PeerController {

	private static final Logger logger = LoggerFactory.getLogger(PeerController.class);

	private final MDNSService mdnsService;
	private final P2PWebSocketHandlerReactive webSocketHandler;

	// 存储手动添加的对等节点
	private final Set<String> manualPeers = ConcurrentHashMap.newKeySet();

	@Autowired
	public PeerController(MDNSService mdnsService, P2PWebSocketHandlerReactive webSocketHandler) {
		this.mdnsService = mdnsService;
		this.webSocketHandler = webSocketHandler;
	}

	@Operation(summary = "获取所有对等节点", description = "返回通过mDNS发现的和手动添加的所有P2P对等节点列表")
	@GetMapping
	public Flux<PeerInfo> getAllPeers() {
		// 获取通过mDNS发现的节点
		Set<String> discoveredPeers = mdnsService.getDiscoveredPeers();
		// 获取当前已连接的节点
		Set<String> connectedPeers = webSocketHandler.getConnectedPeers();

		// 合并发现的和手动添加的节点
		Set<String> allPeers = ConcurrentHashMap.newKeySet();
		allPeers.addAll(discoveredPeers);
		allPeers.addAll(manualPeers);

		List<PeerInfo> peerInfoList = allPeers.stream()
				.map(address -> {
					PeerInfo info = new PeerInfo();
					info.setAddress(address);
					info.setLastSeen(new Date());

					// 检查节点连接状态
					if (connectedPeers.contains(address)) {
						info.setStatus("connected");
					} else if (discoveredPeers.contains(address)) {
						info.setStatus("discovered");
					} else {
						info.setStatus("manual");
					}

					return info;
				})
				.collect(Collectors.toList());

		return Flux.fromIterable(peerInfoList);
	}

	@Operation(summary = "手动添加对等节点", description = "添加一个不是通过mDNS自动发现的对等节点")
	@PostMapping
	public ResponseEntity<String> addPeer(@RequestBody Map<String, String> request) {
		String peerAddress = request.get("address");
		if (peerAddress == null || peerAddress.isBlank()) {
			return ResponseEntity.badRequest().body("节点地址不能为空");
		}

		if (manualPeers.add(peerAddress)) {
			logger.info("手动添加了对等节点: {}", peerAddress);
			return ResponseEntity.ok("节点已添加: " + peerAddress);
		} else {
			return ResponseEntity.ok("节点已存在，无需重复添加");
		}
	}

	@Operation(summary = "移除手动添加的对等节点", description = "从手动添加的对等节点列表中移除指定节点")
	@DeleteMapping("/{peerAddress}")
	public ResponseEntity<String> removePeer(@PathVariable String peerAddress) {
		if (manualPeers.remove(peerAddress)) {
			logger.info("移除了手动添加的对等节点: {}", peerAddress);
			return ResponseEntity.ok("节点已移除: " + peerAddress);
		} else {
			return ResponseEntity.ok("找不到指定的手动添加节点");
		}
	}

	@Operation(summary = "连接到指定的对等节点", description = "尝试建立到指定地址的WebSocket连接")
	@PostMapping("/connect")
	public Mono<ResponseEntity<String>> connectToPeer(@RequestBody Map<String, String> request) {
		String peerAddress = request.get("address");
		if (peerAddress == null || peerAddress.isBlank()) {
			return Mono.just(ResponseEntity.badRequest().body("节点地址不能为空"));
		}

		logger.info("尝试连接到对等节点: {}", peerAddress);

		// 如果是手动添加的节点，确保它被记录
		manualPeers.add(peerAddress);

		return webSocketHandler.connectToPeer(peerAddress)
				.thenReturn(ResponseEntity.ok("已成功连接到节点: " + peerAddress))
				.onErrorResume(e -> {
					logger.error("连接到节点 {} 失败: {}", peerAddress, e.getMessage());
					return Mono.just(ResponseEntity.status(500).body("连接失败: " + e.getMessage()));
				});
	}

	@Operation(summary = "断开与指定对等节点的连接", description = "断开与指定地址的WebSocket连接")
	@PostMapping("/disconnect")
	public Mono<ResponseEntity<String>> disconnectFromPeer(@RequestBody Map<String, String> request) {
		String peerAddress = request.get("address");
		if (peerAddress == null || peerAddress.isBlank()) {
			return Mono.just(ResponseEntity.badRequest().body("节点地址不能为空"));
		}

		logger.info("尝试断开与对等节点的连接: {}", peerAddress);

		return webSocketHandler.disconnectFromPeer(peerAddress)
				.thenReturn(ResponseEntity.ok("已断开与节点的连接: " + peerAddress))
				.onErrorResume(e -> {
					logger.error("断开与节点 {} 的连接失败: {}", peerAddress, e.getMessage());
					return Mono.just(ResponseEntity.status(500).body("断开连接失败: " + e.getMessage()));
				});
	}

	@Operation(summary = "获取连接状态统计", description = "返回P2P连接的统计信息")
	@GetMapping("/stats")
	public ResponseEntity<Map<String, Object>> getConnectionStats() {
		Map<String, Object> stats = new HashMap<>();

		stats.put("connectedPeersCount", webSocketHandler.getConnectedPeers().size());
		stats.put("discoveredPeersCount", mdnsService.getDiscoveredPeers().size());
		stats.put("manualPeersCount", manualPeers.size());
		stats.put("totalPeersCount",
				webSocketHandler.getConnectedPeers().size() +
						mdnsService.getDiscoveredPeers().size() +
						manualPeers.size());

		return ResponseEntity.ok(stats);
	}

	// 定义内部类表示对等节点信息
	public static class PeerInfo {
		private String address;
		private Date lastSeen;
		private String status; // "connected", "discovered" 或 "manual"

		// Getters and setters
		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public Date getLastSeen() {
			return lastSeen;
		}

		public void setLastSeen(Date lastSeen) {
			this.lastSeen = lastSeen;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}
	}
}
