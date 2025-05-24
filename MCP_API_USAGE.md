# MCP 使用说明

本项目使用标准的 [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) 为LLM提供笔记仓库管理和P2P同步功能。

## MCP 概述

MCP (Model Context Protocol) 是一个标准化协议，使AI模型能够以结构化方式与外部工具和资源交互。本项目基于 [Spring AI MCP](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html) 和 [MCP Java SDK](https://modelcontextprotocol.io/sdk/java/mcp-overview) 实现。

## 服务器端点

### MCP服务器信息
- **服务器名称**: p2p-notes-sync-server
- **版本**: 1.0.0
- **传输方式**: Server-Sent Events (SSE)
- **端点**: `http://localhost:8080/mcp`

## 可用工具

### 笔记仓库管理工具

1. **list_repositories** - 获取所有已配置的笔记仓库列表
2. **get_repository** - 根据别名获取指定的笔记仓库配置
   - 参数: `repoAlias` (string)
3. **add_repository** - 添加一个新的笔记仓库配置
   - 参数: `alias` (string), `gitUrl` (string), `localPath` (string), `token` (string)
4. **update_repository** - 更新指定别名的笔记仓库配置
   - 参数: `currentAlias` (string), `newAlias` (string), `gitUrl` (string), `localPath` (string), `token` (string)
5. **remove_repository** - 删除指定别名的笔记仓库配置
   - 参数: `repoAlias` (string)
6. **sync_repository** - 同步指定的笔记仓库(git pull)
   - 参数: `repoAlias` (string)
7. **clone_repository** - 克隆指定的笔记仓库到本地
   - 参数: `repoAlias` (string)
8. **commit_and_push_repository** - 提交并推送指定仓库的本地更改
   - 参数: `repoAlias` (string), `commitMessage` (string), `authorName` (string), `authorEmail` (string)
9. **deploy_repository** - 将指定仓库部署为MkDocs静态网站
   - 参数: `repoAlias` (string)

### P2P同步工具

1. **broadcast_new_repository** - 向P2P网络广播新添加的仓库配置
   - 参数: `repoAlias` (string)
2. **broadcast_update_repository** - 向P2P网络广播更新后的仓库配置
   - 参数: `oldRepoAlias` (string), `newRepoAlias` (string)
3. **broadcast_remove_repository** - 向P2P网络广播删除的仓库配置
   - 参数: `repoAlias` (string)
4. **broadcast_sync_request** - 向P2P网络广播同步请求
   - 参数: `repoAlias` (string)

## LLM 客户端连接

### 使用MCP客户端连接

LLM可以通过标准的MCP客户端连接到服务器：

```json
{
  "mcpServers": {
    "p2p-notes-sync": {
      "command": "node",
      "args": ["mcp-client.js"],
      "env": {
        "MCP_SERVER_URL": "http://localhost:8080/mcp"
      }
    }
  }
}
```

### 使用 Claude Desktop

在 Claude Desktop 配置文件中添加：

```json
{
  "mcpServers": {
    "p2p-notes-sync": {
      "command": "curl",
      "args": [
        "-X", "GET",
        "-H", "Accept: text/event-stream",
        "http://localhost:8080/mcp"
      ]
    }
  }
}
```

### 工具调用示例

```javascript
// 获取所有仓库列表
await mcp.callTool("list_repositories");

// 添加新仓库
await mcp.callTool("add_repository", {
  alias: "my-notes",
  gitUrl: "https://github.com/user/my-notes.git",
  localPath: "local/path/to/repo",
  token: "github_pat_xxx"
});

// 同步仓库
await mcp.callTool("sync_repository", {
  repoAlias: "my-notes"
});

// 广播新仓库到P2P网络
await mcp.callTool("broadcast_new_repository", {
  repoAlias: "my-notes"
});
```

## 协议特性

### 支持的传输方式
- **Server-Sent Events (SSE)**: HTTP基于事件流的实时通信
- **标准JSON-RPC**: 结构化的请求/响应消息格式

### 协议功能
- **工具发现**: LLM可自动发现可用工具
- **参数验证**: 自动验证工具调用参数
- **错误处理**: 标准化的错误响应格式
- **实时通信**: 支持异步操作和实时状态更新

## 开发说明

### 添加新工具

1. 在工具类中添加方法并使用 `@McpTool` 注解：

```java
@McpTool(name = "tool_name", description = "工具描述")
public String newTool(String param1, int param2) {
    // 工具实现
    return "result";
}
```

2. 确保工具类已注册到MCP服务器配置中

### 调试和监控

- 启用DEBUG日志查看MCP协议消息
- 使用Swagger UI监控REST端点
- 检查 `/mcp` 端点的SSE连接状态

## 与REST API的区别

| 特性 | MCP协议 | REST API |
|------|---------|----------|
| 标准化 | 标准MCP协议 | 自定义HTTP接口 |
| 工具发现 | 自动发现 | 手动配置 |
| 类型安全 | 内置验证 | 手动验证 |
| 实时通信 | SSE支持 | 轮询或WebSocket |
| LLM集成 | 原生支持 | 需要适配器 |

## 最佳实践

1. **工具命名**: 使用清晰的下划线命名法
2. **参数设计**: 保持参数简单明确
3. **错误处理**: 返回用户友好的错误消息
4. **异步操作**: 对于长时间运行的操作提供进度反馈
5. **安全性**: 避免在工具描述中暴露敏感信息 