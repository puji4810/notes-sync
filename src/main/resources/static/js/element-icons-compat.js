// 手动注册Element Plus图标组件
// 这个脚本用于解决icons-vue模块在非模块环境下的兼容性问题
window.ElementPlusIconsVue = {
    Setting: {
        name: 'Setting',
        render() {
            return h('svg', {
                viewBox: '0 0 1024 1024',
                xmlns: 'http://www.w3.org/2000/svg',
                'aria-hidden': 'true',
                class: 'setting'
            }, [
                h('path', {
                    fill: 'currentColor',
                    d: 'M600.704 64a32 32 0 0 1 30.464 22.208l35.2 109.376c14.784 7.36 28.928 16 42.432 25.728l110.592-32.512a32 32 0 0 1 40.512 20.224l31.872 109.824a32 32 0 0 1-9.216 33.264l-60.416 47.68a337.92 337.92 0 0 1 0 51.2l60.416 47.68a32 32 0 0 1 9.216 33.216L881.664 571.2a32 32 0 0 1-25.472 12.8 32 32 0 0 1-15.04-3.712l-110.592-32.512a334.08 334.08 0 0 1-42.432 25.728l-35.2 109.376A32 32 0 0 1 600.704 704H423.296a32 32 0 0 1-30.464-22.208l-35.2-109.376a334.08 334.08 0 0 1-42.432-25.728l-110.592 32.512a32 32 0 0 1-40.448-20.224l-31.936-109.824a32 32 0 0 1 9.28-33.264l60.416-47.68a337.92 337.92 0 0 1 0-51.2l-60.416-47.68a32 32 0 0 1-9.28-33.216L142.4 155.96800000000002a32 32 0 0 1 40.512-20.224l110.592 32.512c13.44-9.728 27.648-18.368 42.368-25.728l35.2-109.376A32 32 0 0 1 423.296 64H600.704zm-23.296 64H446.592l-36.8 114.304-9.92 3.072c-14.08 4.352-27.712 9.984-40.768 16.896l-8.96 4.8-115.072-33.76-37.12 128 63.232 49.92-0.64 10.24c-0.512 7.936-0.512 15.936 0 23.936l0.64 10.24-63.232 49.856 37.12 128 115.072-33.76 8.96 4.8a304.64 304.64 0 0 0 40.832 16.896l9.92 3.072L446.656 704h107.648l36.8-114.304 9.92-3.072c14.08-4.352 27.712-9.984 40.768-16.896l8.96-4.8 115.072 33.76 37.12-128-63.232-49.92 0.64-10.24c0.512-7.936 0.512-15.936 0-23.936l-0.64-10.24 63.232-49.856-37.12-128-115.072 33.76-8.96-4.8a304.64 304.64 0 0 0-40.832-16.896l-9.92-3.072L577.344 128zM512 320a192 192 0 1 1 0 384 192 192 0 0 1 0-384zm0 64a128 128 0 1 0 0 256 128 128 0 0 0 0-256z'
                })
            ]);
        }
    },
    Connection: {
        name: 'Connection',
        render() {
            return h('svg', {
                viewBox: '0 0 1024 1024',
                xmlns: 'http://www.w3.org/2000/svg',
                'aria-hidden': 'true',
                class: 'connection'
            }, [
                h('path', {
                    fill: 'currentColor',
                    d: 'M640 384v64H448a128 128 0 0 0-128 128v128a128 128 0 0 0 128 128h320a128 128 0 0 0 128-128V576a128 128 0 0 0-64-110.848V394.88c74.56 26.368 128 97.472 128 181.056v128a192 192 0 0 1-192 192H448a192 192 0 0 1-192-192V576a192 192 0 0 1 192-192h192z'
                }),
                h('path', {
                    fill: 'currentColor',
                    d: 'M384 640v-64h192a128 128 0 0 0 128-128V320a128 128 0 0 0-128-128H256a128 128 0 0 0-128 128v128a128 128 0 0 0 64 110.848v70.272A192.064 192.064 0 0 1 128 448V320a192 192 0 0 1 192-192h320a192 192 0 0 1 192 192v128a192 192 0 0 1-192 192H384z'
                })
            ]);
        }
    },
    DocumentCopy: {
        name: 'DocumentCopy',
        render() {
            return h('svg', {
                viewBox: '0 0 1024 1024',
                xmlns: 'http://www.w3.org/2000/svg',
                'aria-hidden': 'true',
                class: 'document-copy'
            }, [
                h('path', {
                    fill: 'currentColor',
                    d: 'M128 320v576h576V320H128zm-32-64h640a32 32 0 0 1 32 32v640a32 32 0 0 1-32 32H96a32 32 0 0 1-32-32V288a32 32 0 0 1 32-32zM960 96v704h-64V160H256V96h640a32 32 0 0 1 32 32v640a32 32 0 0 1-32 32H256a32 32 0 0 1-32-32V96a32 32 0 0 1 32-32h640z'
                })
            ]);
        }
    },
    RefreshRight: {
        name: 'RefreshRight',
        render() {
            return h('svg', {
                viewBox: '0 0 1024 1024',
                xmlns: 'http://www.w3.org/2000/svg',
                'aria-hidden': 'true',
                class: 'refresh-right'
            }, [
                h('path', {
                    fill: 'currentColor',
                    d: 'M784.512 230.272v-50.56a32 32 0 1 1 64 0v149.056a32 32 0 0 1-32 32H667.52a32 32 0 1 1 0-64h92.992A320 320 0 1 0 524.8 833.152a320 320 0 0 0 320-320h64a384 384 0 0 1-384 384 384 384 0 0 1-384-384 384 384 0 0 1 643.712-282.88z'
                })
            ]);
        }
    },
    Plus: {
        name: 'Plus',
        render() {
            return h('svg', {
                viewBox: '0 0 1024 1024',
                xmlns: 'http://www.w3.org/2000/svg',
                'aria-hidden': 'true',
                class: 'plus'
            }, [
                h('path', {
                    fill: 'currentColor',
                    d: 'M480 480V128a32 32 0 0 1 64 0v352h352a32 32 0 1 1 0 64H544v352a32 32 0 1 1-64 0V544H128a32 32 0 0 1 0-64h352z'
                })
            ]);
        }
    },
    Delete: {
        name: 'Delete',
        render() {
            return h('svg', {
                viewBox: '0 0 1024 1024',
                xmlns: 'http://www.w3.org/2000/svg',
                'aria-hidden': 'true',
                class: 'delete'
            }, [
                h('path', {
                    fill: 'currentColor',
                    d: 'M160 256H96a32 32 0 0 1 0-64h256V95.936a32 32 0 0 1 32-32h256a32 32 0 0 1 32 32V192h256a32 32 0 1 1 0 64h-64v672a32 32 0 0 1-32 32H192a32 32 0 0 1-32-32V256zm448-64v-64H416v64h192zM224 896h576V256H224v640zm192-128a32 32 0 0 1-32-32V416a32 32 0 0 1 64 0v320a32 32 0 0 1-32 32zm192 0a32 32 0 0 1-32-32V416a32 32 0 0 1 64 0v320a32 32 0 0 1-32 32z'
                })
            ]);
        }
    }
};

// 注册Vue的全局h函数
function h(tag, props, children) {
    return Vue.h(tag, props, children);
}

// 添加WebSocket初始化函数
window.initializeWebSocketIfNeeded = function() {
    // 如果全局WebSocket实例未初始化，尝试初始化一个
    if (window.websocket === undefined && window.WebSocketClient) {
        console.log('初始化全局WebSocket实例');
        window.websocket = new window.WebSocketClient({
            path: '/p2p'
        });
    } else if (window.websocket !== undefined) {
        console.log('全局WebSocket实例已存在');
    } else {
        console.warn('WebSocketClient类不可用，无法初始化websocket');
    }
    
    return window.websocket;
};

// 在页面加载完成后初始化
window.addEventListener('DOMContentLoaded', function() {
    // 等待一会儿再执行初始化，确保其他脚本已加载
    setTimeout(function() {
        window.initializeWebSocketIfNeeded();
    }, 500);
});
