// Element Plus 图标将在应用初始化后注册
// 假设NavMenu、loadAppState和websocket已在全局作用域中定义

// 创建Vue应用
const App = {
  components: {
    "nav-menu": NavMenu,
  },
  data() {
    return {
      // 仓库数据
      repositories: [],
      loading: true,

      // 对话框控制
      addRepoDialog: false,
      editRepoDialog: false,
      commitDialog: false,
      p2pBroadcastDialog: false,

      // 状态保持
      appState: loadAppState() || {},

      // 表单数据
      newRepo: {
        alias: "",
        gitUrl: "",
        localPath: "",
        token: "",
      },
      editingRepo: {
        alias: "",
        gitUrl: "",
        localPath: "",
        token: "",
        originalAlias: "",
      },
      commitInfo: {
        commitMessage: "",
        authorName: "",
        authorEmail: "",
        repoAlias: "",
      },
      p2pBroadcastInfo: {
        repoAlias: "",
        broadcastType: "new",
        oldRepoAlias: "",
      },
    };
  },

  created() {
    this.loadRepositories();
    // 初始化WebSocket连接
    if (window.websocket && typeof window.websocket.connect === "function") {
      window.websocket.connect();
    } else {
      console.warn(
        "WebSocket未就绪，尝试使用initializeWebSocketIfNeeded初始化"
      );
      const ws = window.initializeWebSocketIfNeeded();
      if (ws && typeof ws.connect === "function") {
        ws.connect();
      }
    }
  },

  methods: {
    // 加载所有仓库
    loadRepositories() {
      this.loading = true;

      axios
        .get("/api/v1/repositories")
        .then((response) => {
          if (response.data.success) {
            this.repositories = response.data.repositories;
          } else {
            throw new Error(response.data.error || "获取仓库列表失败");
          }
        })
        .catch((error) => {
          console.error("获取仓库列表失败:", error);
          this.$notify.error({
            title: "错误",
            message:
              "获取仓库列表失败: " +
              (error.response?.data?.error || error.message),
          });
        })
        .finally(() => {
          this.loading = false;
        });
    },

    // 显示添加仓库对话框
    showAddRepoDialog() {
      this.newRepo = {
        alias: "",
        gitUrl: "",
        localPath: "",
        token: "",
      };
      this.addRepoDialog = true;
    },

    // 添加新仓库
    addRepository() {
      axios
        .post("/api/v1/repositories", this.newRepo)
        .then((response) => {
          if (response.data.success) {
            this.$notify({
              title: "成功",
              message: response.data.message || "仓库添加成功",
              type: "success",
            });
            this.addRepoDialog = false;
            this.loadRepositories();
          } else {
            throw new Error(response.data.error || "添加仓库失败");
          }
        })
        .catch((error) => {
          console.error("添加仓库失败:", error);
          this.$notify.error({
            title: "错误",
            message:
              "添加仓库失败: " + (error.response?.data?.error || error.message),
          });
        });
    },

    // 同步仓库
    syncRepo(repo) {
      this.loading = true;

      axios
        .post(`/api/v1/repositories/${repo.alias}/sync`)
        .then((response) => {
          if (response.data.success) {
            this.$notify({
              title: "成功",
              message: response.data.message || "仓库同步操作已执行",
              type: "success",
            });
          } else {
            throw new Error(response.data.error || "同步仓库失败");
          }
        })
        .catch((error) => {
          console.error("同步仓库失败:", error);
          this.$notify.error({
            title: "错误",
            message:
              "同步仓库失败: " + (error.response?.data?.error || error.message),
          });
        })
        .finally(() => {
          this.loading = false;
        });
    },

    // 部署仓库
    deployRepo(repo) {
      this.loading = true;

      axios
        .post(`/api/v1/repositories/${repo.alias}/deploy`)
        .then((response) => {
          if (response.data.success) {
            this.$notify({
              title: "成功",
              message: response.data.message || "仓库部署操作已执行",
              type: "success",
            });
          } else {
            throw new Error(response.data.error || "部署仓库失败");
          }
        })
        .catch((error) => {
          console.error("部署仓库失败:", error);
          this.$notify.error({
            title: "错误",
            message:
              "部署仓库失败: " + (error.response?.data?.error || error.message),
          });
        })
        .finally(() => {
          this.loading = false;
        });
    },

    // 处理下拉菜单命令
    handleCommand(command, repo) {
      switch (command) {
        case "edit":
          this.showEditDialog(repo);
          break;
        case "clone":
          this.cloneRepo(repo);
          break;
        case "commit":
          this.showCommitDialog(repo);
          break;
        case "p2p-broadcast":
          this.showP2PBroadcastDialog(repo);
          break;
        case "delete":
          this.confirmDeleteRepo(repo);
          break;
      }
    },

    // 显示编辑对话框
    showEditDialog(repo) {
      this.editingRepo = {
        alias: repo.alias,
        gitUrl: repo.gitUrl,
        localPath: repo.localPath,
        token: "", // 不显示原有令牌，留空表示不修改
        originalAlias: repo.alias, // 保存原始别名用于API调用
      };
      this.editRepoDialog = true;
    },

    // 更新仓库
    updateRepository() {
      axios
        .put(`/api/v1/repositories/${this.editingRepo.originalAlias}`, {
          alias: this.editingRepo.alias,
          gitUrl: this.editingRepo.gitUrl,
          localPath: this.editingRepo.localPath,
          token: this.editingRepo.token || null, // 如果为空，则保持不变
        })
        .then((response) => {
          if (response.data.success) {
            this.$notify({
              title: "成功",
              message: response.data.message || "仓库更新成功",
              type: "success",
            });
            this.editRepoDialog = false;
            this.loadRepositories();
          } else {
            throw new Error(response.data.error || "更新仓库失败");
          }
        })
        .catch((error) => {
          console.error("更新仓库失败:", error);
          this.$notify.error({
            title: "错误",
            message:
              "更新仓库失败: " + (error.response?.data?.error || error.message),
          });
        });
    },

    // 克隆仓库
    cloneRepo(repo) {
      this.loading = true;

      axios
        .post(`/api/v1/repositories/${repo.alias}/clone`)
        .then((response) => {
          if (response.data.success) {
            this.$notify({
              title: "成功",
              message: response.data.message || "仓库克隆操作已执行",
              type: "success",
            });
          } else {
            throw new Error(response.data.error || "克隆仓库失败");
          }
        })
        .catch((error) => {
          console.error("克隆仓库失败:", error);
          this.$notify.error({
            title: "错误",
            message:
              "克隆仓库失败: " + (error.response?.data?.error || error.message),
          });
        })
        .finally(() => {
          this.loading = false;
        });
    },

    // 显示提交对话框
    showCommitDialog(repo) {
      this.commitInfo = {
        commitMessage: "",
        authorName: "",
        authorEmail: "",
        repoAlias: repo.alias,
      };
      this.commitDialog = true;
    },

    // 提交并推送更改
    commitAndPush() {
      if (!this.commitInfo.commitMessage.trim()) {
        this.$notify.warning({
          title: "警告",
          message: "请输入提交信息",
        });
        return;
      }

      this.loading = true;

      axios
        .post(`/api/v1/repositories/${this.commitInfo.repoAlias}/commit-push`, {
          commitMessage: this.commitInfo.commitMessage,
          authorName: this.commitInfo.authorName || undefined,
          authorEmail: this.commitInfo.authorEmail || undefined,
        })
        .then((response) => {
          this.$notify({
            title: "成功",
            message: "变更已提交并推送: " + response.data,
            type: "success",
          });
          this.commitDialog = false;
        })
        .catch((error) => {
          console.error("提交更改失败:", error);
          this.$notify.error({
            title: "错误",
            message: "提交更改失败: " + (error.response?.data || error.message),
          });
        })
        .finally(() => {
          this.loading = false;
        });
    },

    // 确认删除仓库
    confirmDeleteRepo(repo) {
      this.$confirm("此操作将永久删除该仓库配置，是否继续？", "警告", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          this.deleteRepo(repo);
        })
        .catch(() => {
          this.$message({
            type: "info",
            message: "已取消删除",
          });
        });
    },

    // 删除仓库
    deleteRepo(repo) {
      axios
        .delete(`/api/v1/repositories/${repo.alias}`)
        .then((response) => {
          if (response.data.success) {
            this.$notify({
              title: "成功",
              message: response.data.message || "仓库已删除",
              type: "success",
            });
            this.loadRepositories();
          } else {
            throw new Error(response.data.error || "删除仓库失败");
          }
        })
        .catch((error) => {
          console.error("删除仓库失败:", error);
          this.$notify.error({
            title: "错误",
            message:
              "删除仓库失败: " + (error.response?.data?.error || error.message),
          });
        });
    },

    // 显示P2P广播对话框
    showP2PBroadcastDialog(repo) {
      this.p2pBroadcastInfo = {
        repoAlias: repo.alias,
        broadcastType: "new",
        oldRepoAlias: "",
      };
      this.p2pBroadcastDialog = true;
    },

    // 执行P2P广播
    executeBroadcast() {
      const { repoAlias, broadcastType, oldRepoAlias } = this.p2pBroadcastInfo;

      if (!repoAlias) {
        this.$notify.warning({
          title: "警告",
          message: "仓库别名不能为空",
        });
        return;
      }

      let apiUrl = "";
      let params = {};

      switch (broadcastType) {
        case "new":
          apiUrl = `/api/v1/p2p-sync/broadcast/new/${encodeURIComponent(
            repoAlias
          )}`;
          break;
        case "update":
          if (!oldRepoAlias) {
            this.$notify.warning({
              title: "警告",
              message: "更新广播需要提供原仓库别名",
            });
            return;
          }
          apiUrl = `/api/v1/p2p-sync/broadcast/update/${encodeURIComponent(
            oldRepoAlias
          )}`;
          params.newRepoAlias = repoAlias;
          break;
        case "remove":
          apiUrl = `/api/v1/p2p-sync/broadcast/remove/${encodeURIComponent(
            repoAlias
          )}`;
          break;
        case "sync-request":
          apiUrl = `/api/v1/p2p-sync/broadcast/sync-request/${encodeURIComponent(
            repoAlias
          )}`;
          break;
        default:
          this.$notify.error({
            title: "错误",
            message: "未知的广播类型",
          });
          return;
      }

      this.loading = true;

      axios
        .post(apiUrl, null, { params })
        .then((response) => {
          if (response.data) {
            this.$notify({
              title: "成功",
              message: response.data || "P2P广播成功",
              type: "success",
            });
            this.p2pBroadcastDialog = false;
          } else {
            throw new Error("广播操作失败");
          }
        })
        .catch((error) => {
          console.error("P2P广播失败:", error);
          this.$notify.error({
            title: "错误",
            message: "P2P广播失败: " + (error.response?.data || error.message),
          });
        })
        .finally(() => {
          this.loading = false;
        });
    },

    /**
     * 保存当前应用状态到本地存储
     */
    saveState() {
      const state = {
        repositoriesLastUpdate: Date.now(),
        userSettings: {
          authorName: this.commitInfo.authorName,
          authorEmail: this.commitInfo.authorEmail,
        },
      };
      saveAppState(state);
    },

    /**
     * 从本地存储加载应用状态
     */
    loadState() {
      if (this.appState && this.appState.userSettings) {
        this.commitInfo.authorName =
          this.appState.userSettings.authorName || "";
        this.commitInfo.authorEmail =
          this.appState.userSettings.authorEmail || "";
      }
    },
  },
};

// 创建Vue应用实例
const app = Vue.createApp(App);

// 注册 Element Plus 图标组件
if (window.ElementPlusIconsVue) {
  const icons = window.ElementPlusIconsVue;
  const iconComponents = [
    "DocumentCopy",
    "Connection",
    "Setting",
    "ArrowDown",
    "RefreshRight",
    "Plus",
    "Delete",
  ];

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
