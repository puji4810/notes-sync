// P2P节点管理Vue应用
// NavMenu和websocket已在全局作用域中定义
const PeersApp = {
  components: {
    "nav-menu": NavMenu,
  },
  data() {
    return {
      // 加载状态
      loading: true,

      // 对等节点列表
      peers: [],

      // 统计信息
      stats: {
        connectedPeersCount: 0,
        discoveredPeersCount: 0,
        manualPeersCount: 0,
        totalPeersCount: 0,
      },

      // 添加节点对话框
      addPeerDialog: false,
      newPeer: {
        address: "",
      },

      // WebSocket连接状态
      wsConnected: false,

      // 定时器
      refreshTimer: null,
    };
  },

  created() {
    this.loadPeers();
    this.loadStats();

    // 设置自动刷新
    this.refreshTimer = setInterval(() => {
      this.loadPeers();
      this.loadStats();
    }, 10000); // 每10秒刷新一次

    // 初始化WebSocket连接
    let ws = window.websocket;
    if (!ws && typeof window.initializeWebSocketIfNeeded === "function") {
      ws = window.initializeWebSocketIfNeeded();
    }

    if (ws && typeof ws.connect === "function") {
      ws.connect();

      // 监听WebSocket事件
      ws.on("peer_connected", this.handlePeerConnected);
      ws.on("peer_disconnected", this.handlePeerDisconnected);
    } else {
      console.error("WebSocket对象不可用，无法注册事件监听器");
    }
  },

  beforeUnmount() {
    // 清除定时器
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }

    // 清理WebSocket事件监听
    if (window.websocket) {
      window.websocket.off("peer_connected", this.handlePeerConnected);
      window.websocket.off("peer_disconnected", this.handlePeerDisconnected);
    }
  },

  methods: {
    // 加载对等节点列表
    loadPeers() {
      this.loading = true;

      axios
        .get("/api/v1/peers")
        .then((response) => {
          this.peers = response.data;
        })
        .catch((error) => {
          console.error("获取节点列表失败:", error);
          this.$notify.error({
            title: "错误",
            message:
              "获取节点列表失败: " + (error.response?.data || error.message),
          });
        })
        .finally(() => {
          this.loading = false;
        });
    },

    // 加载统计信息
    loadStats() {
      axios
        .get("/api/v1/peers/stats")
        .then((response) => {
          this.stats = response.data;
        })
        .catch((error) => {
          console.error("获取节点统计信息失败:", error);
        });
    },

    // 显示添加节点对话框
    showAddPeerDialog() {
      this.newPeer = {
        name: "",
        address: "",
      };
      this.addPeerDialog = true;
    },

    // 添加新节点
    addPeer() {
      if (!this.newPeer.address) {
        this.$notify.warning({
          title: "警告",
          message: "请填写完整的节点信息",
        });
        return;
      }

      // 只发送地址信息，因为后端API只处理地址
      axios
        .post("/api/v1/peers", {
          address: this.newPeer.address,
        })
        .then(() => {
          this.$notify({
            title: "成功",
            message: "节点添加成功",
            type: "success",
          });
          this.addPeerDialog = false;
          this.loadPeers();
          this.loadStats();
        })
        .catch((error) => {
          console.error("添加节点失败:", error);
          this.$notify.error({
            title: "错误",
            message: "添加节点失败: " + (error.response?.data || error.message),
          });
        });
    },

    // 连接到节点
    connectToPeer(peer) {
      this.loading = true;
      axios
        .post("/api/v1/peers/connect", {
          address: peer.address,
        })
        .then(() => {
          this.$notify({
            title: "成功",
            message: "正在连接到节点: " + peer.name,
            type: "success",
          });
        })
        .catch((error) => {
          console.error("连接节点失败:", error);
          this.$notify.error({
            title: "错误",
            message: "连接节点失败: " + (error.response?.data || error.message),
          });
        })
        .finally(() => {
          this.loading = false;
        });
    },

    // 断开节点连接
    disconnectFromPeer(peer) {
      this.loading = true;
      axios
        .post("/api/v1/peers/disconnect", {
          address: peer.address,
        })
        .then(() => {
          this.$notify({
            title: "成功",
            message: "已断开与节点的连接: " + peer.name,
            type: "success",
          });
        })
        .catch((error) => {
          console.error("断开节点连接失败:", error);
          this.$notify.error({
            title: "错误",
            message:
              "断开节点连接失败: " + (error.response?.data || error.message),
          });
        })
        .finally(() => {
          this.loading = false;
        });
    },

    // 删除手动添加的节点
    removePeer(peer) {
      if (peer.status === "connected") {
        this.$notify.warning({
          title: "警告",
          message: "无法删除已连接的节点，请先断开连接",
        });
        return;
      }

      axios
        .delete("/api/v1/peers", {
          data: {
            address: peer.address,
          },
        })
        .then((response) => {
          this.$notify({
            title: "成功",
            message: "节点已移除",
            type: "success",
          });
          this.loadPeers();
          this.loadStats();
        })
        .catch((error) => {
          console.error("移除节点失败:", error);
          this.$notify.error({
            title: "错误",
            message: "移除节点失败: " + (error.response?.data || error.message),
          });
        });
    },

    // 刷新节点列表
    refreshPeers() {
      this.loadPeers();
      this.loadStats();
    },

    // 处理节点连接事件
    handlePeerConnected(message) {
      const peer = this.peers.find((p) => p.address === message.peer);
      if (peer) {
        peer.status = "connected";
      }
    },

    // 处理节点断开事件
    handlePeerDisconnected(message) {
      const peer = this.peers.find((p) => p.address === message.peer);
      if (peer) {
        peer.status = "disconnected";
      }
    },

    // 获取节点状态标签类型
    getStatusType(status) {
      switch (status) {
        case "connected":
          return "success";
        case "discovered":
          return "info";
        case "manual":
          return "warning";
        default:
          return "info";
      }
    },

    // 获取节点状态显示文本
    getStatusText(status) {
      switch (status) {
        case "connected":
          return "已连接";
        case "discovered":
          return "已发现";
        case "manual":
          return "手动添加";
        default:
          return status;
      }
    },

    // 页面导航
    navigateTo(page) {
      navigation.navigateTo(page);
    },
  },
};

// 创建Vue应用实例
const app = Vue.createApp(PeersApp);

// 注册 Element Plus 图标组件
if (window.ElementPlusIconsVue) {
  const icons = window.ElementPlusIconsVue;
  const iconComponents = ["DocumentCopy", "Connection", "Setting"];

  iconComponents.forEach((name) => {
    if (icons[name]) {
      app.component(name.toLowerCase(), icons[name]);
    }
  });
}

// 使用Element Plus
app.use(ElementPlus);

// 挂载应用
app.mount("#app");
