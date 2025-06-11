// 持久化WebSocket连接管理器
(function () {
  // WebSocket状态存储键
  const WS_STATE_KEY = "notes-sync-websocket-state";
  const WS_SESSION_ID_KEY = "notes-sync-websocket-session-id";
  const WS_SESSION_TIMESTAMP_KEY = "notes-sync-websocket-timestamp";

  // 生成唯一会话ID
  function generateSessionId() {
    return "ws-" + Date.now() + "-" + Math.random().toString(36).substr(2, 9);
  }

  // 保存会话ID
  function saveSessionId() {
    if (!sessionStorage.getItem(WS_SESSION_ID_KEY)) {
      sessionStorage.setItem(WS_SESSION_ID_KEY, generateSessionId());
      sessionStorage.setItem(WS_SESSION_TIMESTAMP_KEY, Date.now().toString());
    }
  }

  // 获取会话ID
  function getSessionId() {
    return sessionStorage.getItem(WS_SESSION_ID_KEY);
  }

  // 保存WebSocket连接状态到localStorage
  function saveWebSocketState(isConnected) {
    localStorage.setItem(
      WS_STATE_KEY,
      JSON.stringify({
        connected: isConnected,
        timestamp: Date.now(),
        sessionId: getSessionId() || generateSessionId(),
      })
    );
  }

  // 获取WebSocket连接状态
  function getWebSocketState() {
    try {
      const state = JSON.parse(localStorage.getItem(WS_STATE_KEY));
      // 如果状态存在且不超过30分钟
      if (state && Date.now() - state.timestamp < 30 * 60 * 1000) {
        return state;
      }
    } catch (e) {
      console.error("解析WebSocket状态失败", e);
    }
    return { connected: false, timestamp: 0 };
  }

  // 增强WebSocketClient类
  function enhanceWebSocketClient() {
    if (!window.WebSocketClient) return;

    // 保存原始方法引用
    const originalConnect = WebSocketClient.prototype.connect;
    const originalOnOpen = WebSocketClient.prototype.onOpen;
    const originalOnClose = WebSocketClient.prototype.onClose;
    const originalDisconnect = WebSocketClient.prototype.disconnect;

    // 增强connect方法
    WebSocketClient.prototype.connect = function () {

      // 检查WebSocket是否仍然有效
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        console.log("WebSocket连接已存在且有效，无需重新连接");
        this.connected = true;
        return;
      }

      originalConnect.call(this);
    };

    // 增强onOpen方法
    WebSocketClient.prototype.onOpen = function (event) {
      console.log("WebSocket连接已建立", getSessionId());
      originalOnOpen.call(this, event);
      saveWebSocketState(true);
    };

    // 增强onClose方法
    WebSocketClient.prototype.onClose = function (event) {
      // 检查是否是导航造成的关闭
      const isNavigating =
        sessionStorage.getItem("notes-sync-nav-state") === "navigating";
      console.log(
        `WebSocket连接关闭 ${isNavigating ? "(导航中)" : ""}`,
        event.code
      );

      originalOnClose.call(this, event);

      // 如果不是导航引起的关闭，才更新连接状态为false
      if (!isNavigating) {
        saveWebSocketState(false);
      }
    };

    // 增强disconnect方法
    WebSocketClient.prototype.disconnect = function () {
      console.log("主动断开WebSocket连接");
      originalDisconnect.call(this);
      saveWebSocketState(false);
    };

    // 添加自动重连的检查方法
    WebSocketClient.prototype.checkAndReconnect = function () {
      const state = getWebSocketState();
      const now = Date.now();

      // 只在30分钟内的连接状态有效
      if (state.connected && now - state.timestamp < 30 * 60 * 1000) {
        if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
          console.log("根据存储状态重新连接WebSocket");
          this.connect();
        } else {
          console.log("WebSocket连接已经打开");
        }
      } else if (!this.connected && !this.isConnecting) {
        // 如果超过30分钟或者之前未连接，尝试新建连接
        console.log("无有效WebSocket状态或状态已过期，尝试新建连接");
        this.connect();
      }
    };
  }

  // 检查导航状态
  function checkNavigation() {
    const navState = sessionStorage.getItem("notes-sync-nav-state");
    if (navState === "navigating") {
      console.log("检测到页面导航，保持WebSocket状态");
      sessionStorage.removeItem("notes-sync-nav-state");
      return true;
    }
    return false;
  }

  // 页面加载时自动调用
  function setupPersistentConnection() {
    // 保存或获取会话ID
    saveSessionId();

    // 增强WebSocketClient类
    enhanceWebSocketClient();

    // 初始化WebSocketClient实例
    let ws = window.websocket;
    if (!ws && typeof window.initializeWebSocketIfNeeded === "function") {
      ws = window.initializeWebSocketIfNeeded();
    }

    // 是否是页面导航过来的
    const isNavigation = checkNavigation();

    // 检查连接状态
    if (ws && typeof ws.checkAndReconnect === "function") {
      // 给予一点时间确保DOM完全加载
      setTimeout(
        () => {
          ws.checkAndReconnect();
        },
        isNavigation ? 100 : 500
      ); // 如果是导航过来的，更快地重连
    }

    // 监听页面可见性变化
    document.addEventListener("visibilitychange", () => {
      if (
        !document.hidden &&
        ws &&
        typeof ws.checkAndReconnect === "function"
      ) {
        ws.checkAndReconnect();
      }
    });

    // 监听页面卸载事件，用于处理页面跳转
    window.addEventListener("beforeunload", () => {
      // 只记录状态，不关闭连接
      if (ws && ws.connected) {
        saveWebSocketState(true);
      }
    });
  }

  // 页面加载时调用
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", setupPersistentConnection);
  } else {
    setupPersistentConnection();
  }

  // 设置全局钩子
  window.checkAndReconnectWebSocket = function () {
    if (
      window.websocket &&
      typeof window.websocket.checkAndReconnect === "function"
    ) {
      window.websocket.checkAndReconnect();
    }
  };
})();
