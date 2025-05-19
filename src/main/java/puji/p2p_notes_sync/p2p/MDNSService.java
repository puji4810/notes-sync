package puji.p2p_notes_sync.p2p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceEvent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MDNSService {

	private static final Logger logger = LoggerFactory.getLogger(MDNSService.class);
	private static final String SERVICE_TYPE = "_p2pnotesync._tcp.local."; // 服务类型
	private static final String SERVICE_NAME_PREFIX = "P2PNotesSyncNode-"; // 服务名称前缀

	private JmDNS jmdns;
	private ServiceInfo serviceInfo;
	private String serviceName;

	// 添加对P2PWebSocketHandlerReactive的引用，用于自动连接新发现的节点
	private P2PWebSocketHandlerReactive webSocketHandler;

	@Value("${server.port:8080}") // 从application.properties中获取WebSocket服务器端口，默认为8080
	private int appWebSocketPort; // 假设WebSocket与HTTP服务端口一致或可配置

	// 用于存储发现的对等节点信息 (例如 "host:port")
	private final Set<String> discoveredPeers = ConcurrentHashMap.newKeySet();

	// 构造函数注入WebSocketHandler
	public MDNSService(P2PWebSocketHandlerReactive webSocketHandler) {
		this.webSocketHandler = webSocketHandler;
	}

	@PostConstruct
	public void start() {
		try {
			// 在特定网络接口上创建JmDNS实例，如果有多网卡环境，可能需要更复杂的处理
			// InetAddress addr = InetAddress.getLocalHost(); // 或者选择特定网卡
			// jmdns = JmDNS.create(addr);
			jmdns = JmDNS.create(); // 简化处理，使用默认主机地址

			// 生成一个唯一的服务名称
			this.serviceName = SERVICE_NAME_PREFIX + InetAddress.getLocalHost().getHostName().split("\\.")[0] + "-"
					+ appWebSocketPort;

			// 注册服务监听器以发现其他节点
			jmdns.addServiceListener(SERVICE_TYPE, new P2PServiceListener());
			logger.info("mDNS ServiceListener registered for type: {}", SERVICE_TYPE);

			// 注册本节点的服务
			// 属性可以用来传递额外信息，但通常WebSocket的连接地址通过ServiceInfo的host/port获取
			serviceInfo = ServiceInfo.create(SERVICE_TYPE, this.serviceName, appWebSocketPort, "P2P Notes Sync Node");
			jmdns.registerService(serviceInfo);
			logger.info("mDNS service registered: Name='{}', Type='{}', Port={}", this.serviceName, SERVICE_TYPE,
					appWebSocketPort);

		} catch (UnknownHostException e) {
			logger.error("mDNS start failed - UnknownHostException: {}", e.getMessage(), e);
		} catch (IOException e) {
			logger.error("mDNS start failed - IOException: {}", e.getMessage(), e);
		} catch (Exception e) {
			logger.error("mDNS start failed - Unexpected exception: {}", e.getMessage(), e);
		}
	}

	@PreDestroy
	public void stop() {
		if (jmdns != null) {
			logger.info("Unregistering mDNS service: {}", serviceInfo.getName());
			jmdns.unregisterService(serviceInfo);
			jmdns.unregisterAllServices(); // 确保所有服务都被注销
			try {
				jmdns.close();
				logger.info("JmDNS closed.");
			} catch (IOException e) {
				logger.error("Error closing JmDNS: {}", e.getMessage(), e);
			}
		}
		discoveredPeers.clear();
	}

	public Set<String> getDiscoveredPeers() {
		return Collections.unmodifiableSet(new HashSet<>(discoveredPeers));
	}

	private class P2PServiceListener implements ServiceListener {
		@Override
		public void serviceAdded(ServiceEvent event) {
			logger.info("mDNS Service added: {}", event.getName());
			// 服务被添加时，JmDNS会尝试解析它。我们可以在serviceResolved中处理。
			jmdns.requestServiceInfo(event.getType(), event.getName(), 1000); // 请求解析服务信息
		}

		@Override
		public void serviceRemoved(ServiceEvent event) {
			logger.info("mDNS Service removed: {}", event.getName());
			ServiceInfo info = event.getInfo();
			if (info != null) {
				String peerAddress = constructPeerAddress(info);
				if (discoveredPeers.remove(peerAddress)) {
					logger.info("Peer removed: {} ({})", peerAddress, event.getName());
					// 在这里可以通知 P2PWebSocketHandler 关闭到该节点的连接 (如果已建立)
				}
			}
		}

		@Override
		public void serviceResolved(ServiceEvent event) {
			logger.info("mDNS Service resolved: {}", event.getName());
			ServiceInfo info = event.getInfo();
			if (info != null && !serviceName.equals(info.getName())) { // 排除自身
				// JmDNS 可能会为同一服务返回多个IPv4/IPv6地址，这里简单选择第一个IPv4
				String hostAddress = null;
				if (info.getInet4Addresses().length > 0) {
					hostAddress = info.getInet4Addresses()[0].getHostAddress();
				} else if (info.getInetAddresses().length > 0) { // 备选其他地址
					hostAddress = info.getInetAddresses()[0].getHostAddress();
				}

				if (hostAddress != null) {
					String peerAddress = hostAddress + ":" + info.getPort();
					if (discoveredPeers.add(peerAddress)) {
						logger.info("Peer discovered and resolved: {} ({}) at {}. Attempting to connect.", peerAddress, info.getName(),
								info.getApplication());
						// 自动连接到新发现的节点
						webSocketHandler.connectToPeer(peerAddress)
							.subscribe(
								null, // onComplete: do nothing
								error -> logger.error("Failed to connect to peer {} after discovery: {}", peerAddress, error.getMessage())
							);
					} else {
						logger.info("Peer {} ({}) already discovered.", peerAddress, info.getName());
					}
				} else {
					logger.warn("Could not resolve host address for service: {}. IPv4 addresses: {}, IPv6 addresses: {}, Other addresses: {}", 
								info.getName(), info.getInet4Addresses().length, info.getInet6Addresses().length, info.getInetAddresses().length);
				}
			} else {
				if (info == null) {
					logger.warn("Service resolved but ServiceInfo is null for: {}", event.getName());
				} else { // It must be the service itself
					logger.info("Service resolved but it is our own service ({}). Ignoring.", event.getName());
				}
			}
		}

		private String constructPeerAddress(ServiceInfo info) {
			// 优先使用IPv4地址
			if (info.getInet4Addresses().length > 0) {
				return info.getInet4Addresses()[0].getHostAddress() + ":" + info.getPort();
			} else if (info.getInetAddresses().length > 0) { // 如果没有IPv4，尝试其他地址
				return info.getInetAddresses()[0].getHostAddress() + ":" + info.getPort();
			}
			return null; // 无法构建地址
		}
	}
}