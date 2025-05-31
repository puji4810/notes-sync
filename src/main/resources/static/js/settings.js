// 创建Vue应用
// 假设NavMenu和websocket已在全局作用域中定义
const App = {
    components: {
        'nav-menu': NavMenu
    },
    data() {
        return {
            loading: false,
            activeTab: 'general',
            settings: {
                appName: '',
                defaultRepoPath: '',
                logLevel: 'INFO',
                authorName: '',
                authorEmail: '',
                syncInterval: 300,
                git: {
                    executablePath: '',
                    port: 8080
                },
                mkdocs: {
                    theme: 'material',
                    enableNavigation: true
                }
            },
            systemInfo: {
                version: '',
                uptime: 0,
                memory: {
                    total: 0,
                    used: 0,
                    free: 0
                },
                disk: {
                    total: 0,
                    used: 0,
                    free: 0
                },
                javaVersion: '',
                osName: '',
                osVersion: '',
                osArch: '',
                availableProcessors: 0
            }
        };
    },
    
    created() {
        this.loadSettings();
        this.loadSystemInfo();
        
        // 初始化WebSocket连接
        if (window.websocket && typeof window.websocket.connect === 'function') {
            window.websocket.connect();
        } else {
            console.warn('WebSocket未就绪，尝试使用initializeWebSocketIfNeeded初始化');
            const ws = window.initializeWebSocketIfNeeded();
            if (ws && typeof ws.connect === 'function') {
                ws.connect();
            }
        }
    },
    
    methods: {
        // 加载设置
        loadSettings() {
            this.loading = true;
            axios.get('/api/v1/settings')
                .then(response => {
                    if (response.data.success) {
                        this.settings = { ...this.settings, ...response.data.settings };
                    } else {
                        throw new Error(response.data.error || '加载设置失败');
                    }
                })
                .catch(error => {
                    console.error('加载设置失败:', error);
                    this.$notify.error({
                        title: '错误',
                        message: '加载设置失败: ' + (error.response?.data?.error || error.message)
                    });
                })
                .finally(() => {
                    this.loading = false;
                });
        },
        
        // 加载系统信息
        loadSystemInfo() {
            this.loading = true;
            axios.get('/api/v1/settings/system-info')
                .then(response => {
                    if (response.data.success) {
                        // 映射后端返回的扁平数据结构到前端嵌套结构
                        this.systemInfo = {
                            version: response.data.systemInfo.version || '',
                            uptime: response.data.systemInfo.uptime || 0,
                            memory: {
                                total: response.data.systemInfo.totalMemory || 0,
                                used: (response.data.systemInfo.totalMemory || 0) - (response.data.systemInfo.freeMemory || 0),
                                free: response.data.systemInfo.freeMemory || 0
                            },
                            disk: {
                                total: response.data.systemInfo.totalDisk || 0,
                                used: response.data.systemInfo.usedDisk || 0,
                                free: response.data.systemInfo.freeDisk || 0
                            },
                            javaVersion: response.data.systemInfo.javaVersion || '',
                            osName: response.data.systemInfo.osName || '',
                            osVersion: response.data.systemInfo.osVersion || '',
                            osArch: response.data.systemInfo.osArch || '',
                            availableProcessors: response.data.systemInfo.availableProcessors || 0
                        };
                    } else {
                        throw new Error(response.data.error || '加载系统信息失败');
                    }
                })
                .catch(error => {
                    console.error('加载系统信息失败:', error);
                    this.$notify.error({
                        title: '错误',
                        message: '加载系统信息失败: ' + (error.response?.data?.error || error.message)
                    });
                })
                .finally(() => {
                    this.loading = false;
                });
        },
        
        // 更新设置
        updateSettings() {
            this.loading = true;
            axios.put('/api/v1/settings', this.settings)
                .then(response => {
                    if (response.data.success) {
                        this.$notify({
                            title: '成功',
                            message: response.data.message || '设置已更新',
                            type: 'success'
                        });
                    } else {
                        throw new Error(response.data.error || '更新设置失败');
                    }
                })
                .catch(error => {
                    console.error('更新设置失败:', error);
                    this.$notify.error({
                        title: '错误',
                        message: '更新设置失败: ' + (error.response?.data?.error || error.message)
                    });
                })
                .finally(() => {
                    this.loading = false;
                });
        },
        
        // 格式化内存大小
        formatSize(bytes) {
            if (bytes === 0) return '0 B';
            const k = 1024;
            const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
            const i = Math.floor(Math.log(bytes) / Math.log(k));
            return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
        }
    }
};

// 创建Vue应用实例
const app = Vue.createApp(App);

// 注册 Element Plus 图标组件
if (window.ElementPlusIconsVue) {
    const icons = window.ElementPlusIconsVue;
    const iconComponents = [
        'DocumentCopy',
        'Connection',
        'Setting',
        'RefreshRight',
        'Plus',
        'Delete'
    ];
    
    iconComponents.forEach(name => {
        if (icons[name]) {
            app.component(name.toLowerCase(), icons[name]);
        }
    });
}

// 使用Element Plus
app.use(ElementPlus);

// 挂载应用
app.mount('#app');