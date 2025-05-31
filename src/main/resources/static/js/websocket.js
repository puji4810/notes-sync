// WebSocket通信工具类
class WebSocketClient {
    constructor(options = {}) {
        this.baseUrl = options.baseUrl || window.location.host;
        this.protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        this.path = options.path || '/p2p';
        this.reconnectInterval = options.reconnectInterval || 5000;
        this.maxReconnectAttempts = options.maxReconnectAttempts || 5;
        this.reconnectAttempts = 0;
        this.listeners = {};
        this.socket = null;
        this.messageQueue = [];
        this.connected = false;
        this.isConnecting = false;
        
        // 绑定方法上下文
        this.onOpen = this.onOpen.bind(this);
        this.onMessage = this.onMessage.bind(this);
        this.onClose = this.onClose.bind(this);
        this.onError = this.onError.bind(this);
    }
    
    // 建立连接
    connect() {
        if (this.isConnecting) {
            console.log('WebSocket正在连接中，跳过重复连接');
            return;
        }
        
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            console.log('WebSocket已连接');
            return;
        }
        
        this.isConnecting = true;
        
        try {
            const url = `${this.protocol}//${this.baseUrl}${this.path}`;
            console.log(`正在连接WebSocket: ${url}`);
            
            this.socket = new WebSocket(url);
            this.socket.onopen = this.onOpen;
            this.socket.onmessage = this.onMessage;
            this.socket.onclose = this.onClose;
            this.socket.onerror = this.onError;
        } catch (error) {
            console.error('WebSocket连接失败:', error);
            this.isConnecting = false;
            this.scheduleReconnect();
        }
    }
    
    // 连接打开时处理
    onOpen(event) {
        console.log('WebSocket已连接');
        this.connected = true;
        this.isConnecting = false;
        this.reconnectAttempts = 0;
        
        // 发送等待队列中的消息
        if (this.messageQueue.length > 0) {
            console.log(`发送${this.messageQueue.length}条排队消息`);
            this.messageQueue.forEach(msg => this.send(msg, true));
            this.messageQueue = [];
        }
        
        // 触发连接事件
        this.triggerEvent('connect', event);
    }
    
    // 收到消息时处理
    onMessage(event) {
        try {
            const data = JSON.parse(event.data);
            console.log('收到WebSocket消息:', data);
            
            // 根据消息类型触发对应事件
            if (data.type) {
                this.triggerEvent(data.type, data);
            }
            
            // 总是触发通用消息事件
            this.triggerEvent('message', data);
        } catch (error) {
            console.error('解析WebSocket消息失败:', error, event.data);
        }
    }
    
    // 连接关闭时处理
    onClose(event) {
        console.log(`WebSocket连接已关闭: ${event.code} ${event.reason}`);
        this.connected = false;
        this.isConnecting = false;
        this.triggerEvent('disconnect', event);
        
        // 非正常关闭时重连
        if (event.code !== 1000) {
            this.scheduleReconnect();
        }
    }
    
    // 发生错误时处理
    onError(error) {
        console.error('WebSocket错误:', error);
        this.triggerEvent('error', error);
    }
    
    // 发送消息
    send(data, skipQueue = false) {
        // 如果不是字符串则转为JSON
        const message = typeof data === 'string' ? data : JSON.stringify(data);
        
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            this.socket.send(message);
            return true;
        } else if (!skipQueue) {
            // 如果连接未就绪且未禁用队列，则加入待发送队列
            console.log('WebSocket未连接，消息已排队');
            this.messageQueue.push(data);
        }
        
        return false;
    }
    
    // 主动关闭连接
    disconnect() {
        if (this.socket) {
            this.socket.close(1000, '客户端主动断开连接');
            this.socket = null;
            this.connected = false;
        }
    }
    
    // 安排重连
    scheduleReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`尝试重新连接 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
            
            setTimeout(() => {
                this.connect();
            }, this.reconnectInterval);
        } else {
            console.error(`达到最大重连次数 (${this.maxReconnectAttempts})，停止重连`);
            this.triggerEvent('reconnect_failed');
        }
    }
    
    // 注册事件监听器
    on(event, callback) {
        if (!this.listeners[event]) {
            this.listeners[event] = [];
        }
        
        this.listeners[event].push(callback);
        return this;
    }
    
    // 移除事件监听器
    off(event, callback) {
        if (this.listeners[event]) {
            if (callback) {
                this.listeners[event] = this.listeners[event].filter(cb => cb !== callback);
            } else {
                delete this.listeners[event];
            }
        }
        
        return this;
    }
    
    // 触发事件
    triggerEvent(event, data) {
        if (this.listeners[event]) {
            this.listeners[event].forEach(callback => {
                try {
                    callback(data);
                } catch (error) {
                    console.error(`执行${event}事件回调时出错:`, error);
                }
            });
        }
    }
}

// 创建全局WebSocket实例
const websocket = new WebSocketClient({
    path: '/p2p'
});

// P2P消息处理函数
const handleP2PMessage = (message) => {
    if (!message || !message.type) return;
    
    switch (message.type) {
        case 'peer_connected':
            // 处理节点连接事件
            notifyPeerConnected(message);
            break;
            
        case 'peer_disconnected':
            // 处理节点断开事件
            notifyPeerDisconnected(message);
            break;
            
        case 'repository_updated':
            // 处理仓库更新事件
            notifyRepositoryUpdate(message);
            break;
            
        default:
            console.log('收到未知类型P2P消息:', message);
            break;
    }
};

// 通知函数
const notifyPeerConnected = (message) => {
    if (window.ElementPlus) {
    ElementPlus.ElNotification({
        title: '节点已连接',
        message: `节点 ${message.peer} 已连接`,
        type: 'success',
        duration: 3000
    });
    }
};

const notifyPeerDisconnected = (message) => {
    if (window.ElementPlus) {
    ElementPlus.ElNotification({
        title: '节点已断开',
        message: `节点 ${message.peer} 已断开连接`,
        type: 'warning',
        duration: 3000
    });
    }
};

const notifyRepositoryUpdate = (message) => {
    if (window.ElementPlus) {
    ElementPlus.ElNotification({
        title: '仓库已更新',
        message: `仓库 ${message.repoAlias} 已由节点 ${message.peer} 更新`,
        type: 'info',
        duration: 5000
    });
    }
};

// 添加到全局作用域
if (typeof window !== 'undefined') {
    // 只在没有WebSocketClient的情况下设置，防止重复定义
    if (!window.WebSocketClient) {
        window.WebSocketClient = WebSocketClient;
    }
    
    // 只在没有websocket的情况下设置全局websocket实例
    if (!window.websocket) {
        console.log('初始化全局websocket实例');
        window.websocket = websocket;
    }
    
    if (!window.handleP2PMessage) {
        window.handleP2PMessage = handleP2PMessage;
    }
}

// 导出给模块使用（如果支持）
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { WebSocketClient, websocket, handleP2PMessage };
}