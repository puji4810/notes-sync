// 全局导航功能
// 用于不同页面之间的导航和状态保持
const navigateTo = (path) => {
    window.location.href = path;
};

// 检查当前页面并高亮对应的导航项
const highlightCurrentPage = () => {
    const path = window.location.pathname;
    let activeItem = '';
    
    if (path === '/' || path === '/index.html') {
        activeItem = 'repositories';
    } else if (path === '/peers.html' || path === '/peers/view' || path === '/peers/page') {
        activeItem = 'peers';
    } else if (path === '/settings.html' || path === '/settings/view' || path === '/settings/page') {
        activeItem = 'settings';
    }
    
    return activeItem;
};

// 保存应用状态到本地存储
const saveAppState = (state) => {
    localStorage.setItem('notes-sync-state', JSON.stringify(state));
};

// 从本地存储恢复应用状态
const loadAppState = () => {
    const savedState = localStorage.getItem('notes-sync-state');
    return savedState ? JSON.parse(savedState) : null;
};

// 提取公共 Vue 组件
const NavMenu = {
    data() {
        return {
            activeIndex: highlightCurrentPage()
        };
    },
    methods: {
        navigateTo
    },
    template: `
    <el-menu
      :default-active="activeIndex"
      class="el-menu-vertical"
      background-color="#001529"
      text-color="#fff"
      active-text-color="#409EFF">
        <el-menu-item index="repositories" @click="navigateTo('/')">
            <span>📚 仓库管理</span>
        </el-menu-item>
        <el-menu-item index="peers" @click="navigateTo('/peers.html')">
            <span>🔗 对等节点</span>
        </el-menu-item>
        <el-menu-item index="settings" @click="navigateTo('/settings.html')">
            <span>⚙️ 设置</span>
        </el-menu-item>
    </el-menu>
    `
};

// 导出给模块使用（如果支持）
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { navigateTo, highlightCurrentPage, NavMenu, saveAppState, loadAppState };
}

// 添加到全局作用域
if (typeof window !== 'undefined') {
    window.navigateTo = navigateTo;
    window.highlightCurrentPage = highlightCurrentPage;
    window.NavMenu = NavMenu;
    window.saveAppState = saveAppState;
    window.loadAppState = loadAppState;
    
    // 创建navigation对象
    window.navigation = {
        navigateTo: navigateTo,
        highlightCurrentPage: highlightCurrentPage,
        saveAppState: saveAppState,
        loadAppState: loadAppState
    };
}