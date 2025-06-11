// 定时检查和维护WebSocket连接
(function () {
  // 当前WebSocket状态检查间隔（毫秒）
  const WS_CHECK_INTERVAL = 5000;

  // 定时检查WebSocket状态并尝试重连
  function startPeriodicChecks() {
    // 如果已经有定时器，不重复创建
    if (window._wsCheckIntervalId) return;

    // 创建定时器
    window._wsCheckIntervalId = setInterval(() => {
      if (
        window.websocket &&
        typeof window.websocket.checkAndReconnect === "function"
      ) {
        // 尝试检查并重连
        window.websocket.checkAndReconnect();
      }
    }, WS_CHECK_INTERVAL);
  }

  // 停止定时检查
  function stopPeriodicChecks() {
    if (window._wsCheckIntervalId) {
      clearInterval(window._wsCheckIntervalId);
      window._wsCheckIntervalId = null;
    }
  }

  // 导出检查函数
  window.wsConnectionMonitor = {
    start: startPeriodicChecks,
    stop: stopPeriodicChecks,
  };

  // 页面加载时启动检查
  document.addEventListener("DOMContentLoaded", () => {
    // 等待一会启动周期性检查，确保其他脚本初始化完成
    setTimeout(startPeriodicChecks, 1000);
  });

  // 页面卸载时停止检查
  window.addEventListener("beforeunload", stopPeriodicChecks);
})();
