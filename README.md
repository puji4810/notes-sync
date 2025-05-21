# Notes-Sync

本项目的目标是开发一个应用程序，它允许用户通过Git版本控制系统在多个设备间同步和管理其笔记（支持多个独立的笔记仓库），并将笔记内容方便地部署为静态网页。应用采用P2P（对等网络）架构进行设备间的操作触发和配置同步，并通过MCP（模型上下文协议）集成LLM（大语言模型）以实现更智能的交互和自动化操作。

## 一、核心架构与理念

1. **对等网络 (P2P) 模型：** 每个安装并运行此程序的设备都是一个功能对等的节点。没有中央服务器控制核心的同步触发或配置共享逻辑。
2. **Git为数据核心：** 所有笔记的实际内容、版本历史均存储在用户指定的远程Git仓库中（如GitHub、GitLab等）。本应用不直接存储笔记数据，而是作为这些Git仓库的客户端和管理器。
3. **多仓库管理：** 用户可以在应用中添加和管理多个独立的Git笔记仓库，每个仓库代表一个不同的笔记集（例如“工作笔记”、“学习资料”）。
4. **用户体验：** 提供图形化的Dashboard（仪表盘）进行手动操作，同时支持LLM通过MCP工具进行自动化操作。

## 二、程序实例（在每个用户设备上运行）的关键组成部分

每个设备上运行的程序实例都包含以下模块：

1. **配置管理模块：**
   * 负责管理用户添加的多个笔记仓库的配置信息。每个配置包括：用户定义的仓库别名、Git仓库URL、笔记在本地的存储路径。
   * 这些配置信息将持久化存储在设备本地的一个JSON文件（例如 `managed_repositories.json`）中。
   * **Git访问凭证（如Token）由用户在每台设备上为每个仓库单独输入，并进行本地加密存储。凭证本身绝不通过P2P网络传输。**
2. **Git集成模块：**
   * 执行所有与Git相关的操作（`clone`, `pull`, `push`, `add`, `commit`, `status`等），针对用户配置的特定笔记仓库及其本地路径。
3. **P2P网络模块：**
   * **节点发现 (mDNS)：** 使用mDNS协议在局域网（LAN）内自动发现其他运行相同程序的对等节点。同时也通过mDNS广播自身的存在及通信端口。外部设备可通过VPN接入局域网，从而被mDNS发现。
   * **节点间通信 (WebSocket)：** 一旦通过mDNS发现对等节点，设备间将使用WebSocket协议进行实际的P2P消息传递。主要消息类型包括：
     * `REQUEST_SYNC { "repo_alias": "..." }`: 请求对等节点对指定的仓库执行`git pull`。 (注意：之前为repo_url，已更正为repo_alias以便于用户识别和LLM操作)
     * `CONFIG_ADD_REPO { "repo_alias": "...", "repo_url": "...", "local_path": "..." }`: 通知对等节点用户新增了一个仓库配置（不含Token）。接收方会更新本地JSON，并将新仓库的Token状态标记为“待用户本地输入”。
     * `CONFIG_UPDATE_REPO { "old_repo_alias": "...", "new_repo_alias": "...", "repo_url": "...", "local_path": "..." }`: 通知对等节点用户更新了一个仓库配置（不含Token）。
     * `CONFIG_REMOVE_REPO { "repo_alias": "..." }`: 通知对等节点用户移除了一个仓库配置。 (注意：之前为repo_url_or_alias，已更正为repo_alias)
   * **对等节点列表管理：** 维护当前已知的、活跃的对等节点信息。
4. **MkDocs部署模块：**
   * 集成MkDocs命令行工具，能够将用户指定的、已同步的笔记仓库内容构建为静态HTML网站。
   * 管理`mkdocs.yaml`配置文件，其生成和修改可由LLM辅助。
5. **用户界面 (Dashboard - JavaScript SPA)：**
   * 采用JavaScript单页面应用（SPA）技术（如React, Vue等）构建，提供图形化用户界面。
   * 通过RESTful API与运行在同一设备上的Java Spring Boot后端进行通信。
   * **主要功能：**
     * 列出本地JSON文件中所有已配置的笔记仓库。
     * 允许用户添加新的笔记仓库（输入别名、URL、Token（用于本地存储）、本地路径），添加成功后会触发P2P配置广播。
     * 允许用户修改已配置的笔记仓库（别名、URL、本地路径），修改后也会触发P2P配置广播。
     * 允许用户移除已配置的笔记仓库，移除后也会触发P2P配置广播。
     * 针对选定的笔记仓库，执行“一键同步”（本地执行Git操作，并向P2P网络广播同步请求）、“推送本地更改”、“部署为MkDocs网站”等操作。
     * 查看和修改特定仓库的MkDocs配置。
     * **集成MkDocs站点查看：** MkDocs构建生成的静态网站将由Spring Boot后端直接提供服务（例如通过 `/render/{仓库别名}/` 这样的路径）。用户可以直接在浏览器中打开此URL，或者Dashboard可以通过`<iframe>`嵌入显示该站点，实现应用内查看。
6. **后端服务与API层 (Java Spring Boot)：**
   * 作为每个程序实例的“引擎”，运行在用户设备本地。
   * 提供RESTful API供前端JavaScript SPA调用。
   * 管理本地`managed_repositories.json`配置文件。
   * 执行Git操作、调用MkDocs。
   * 实现mDNS服务发现和P2P的WebSocket通信（同时作为服务器接收连接，也作为客户端连接其他节点）。
   * 托管前端SPA的静态文件，并托管MkDocs构建后的静态站点。
   * 集成MCP服务器。
7. **LLM集成 (MCP - 模型上下文协议)：**
   * 后端集成`spring-ai-mcp-server-spring-boot-starter`，通过SSE（Server-Sent Events）与LLM通信。
   * 向LLM暴露以下工具（Java函数）：
     * `register_new_repository_tool(repo_alias, git_url, local_path, git_token)`: 在当前设备注册新仓库，加密存储Token，并通过P2P广播新仓库配置（不含Token）。
     * `update_repository_tool(old_repo_alias, new_repo_alias, git_url, local_path, new_git_token_optional)`: 更新现有仓库配置。如果提供了`new_git_token_optional`，则更新Token。通过P2P广播配置变更（不含Token）。
     * `sync_repository_tool(repo_alias)`: 对指定仓库执行本地Git同步，并向P2P网络广播同步请求。
     * `deploy_repository_to_mkdocs_tool(repo_alias, mkdocs_config_suggestions)`: 将指定仓库部署为MkDocs网站，LLM可辅助配置，并返回可访问的URL。
     * `list_configured_repositories_tool()`: 返回当前已配置的笔记仓库列表。
     * `remove_repository_tool(repo_alias)`: 在当前设备移除仓库配置，并通过P2P广播此变更。

## 三、关键工作流程

* **单设备使用：** 用户添加仓库、同步、部署等操作均在本地完成。mDNS发现不到其他节点，P2P广播无实际接收方，但不影响本地功能。
* **多设备（局域网/VPN内）添加新仓库：** 一设备添加后，通过P2P（WebSocket）将仓库元信息（不含Token）通知其他设备。其他设备收到后，用户需在该设备上为这个新识别的仓库单独输入Token。
* **多设备（局域网/VPN内）更新仓库配置：** 一设备更新仓库配置（例如修改别名或URL）后，通过P2P（WebSocket）将新的仓库元信息（不含Token，但包含旧别名以供查找）通知其他设备。其他设备收到后，会更新其本地配置。如果URL变更导致需要新的Token，用户需在该设备上为这个仓库更新Token。
* **多设备（局域网/VPN内）触发同步：** 一设备发起针对某仓库的同步后，本地执行Git操作，然后通过P2P（WebSocket）通知其他设备对同一仓库进行同步（这些设备使用各自本地存储的Token）。

## 四、项目价值与特色

* **用户数据主权：** 笔记内容存储在用户自己的Git仓库中。
* **P2P协同：** 设备间操作触发和部分配置同步采用去中心化方式。
* **多仓库管理：** 灵活管理不同的笔记项目。
* **智能化操作：** 通过LLM集成，简化复杂操作，提供智能辅助。
* **本地优先与网络扩展：** 完美支持单机本地使用，并通过mDNS和VPN轻松扩展到局域网P2P协同。
* **开放与集成：** 前后端分离，API驱动，易于维护和扩展。

## 五、技术栈

* **后端:** Java, Spring Boot, Spring WebFlux (用于P2P WebSocket通信和非阻塞API)
* **P2P发现:** mDNS (例如使用 JmDNS库)
* **P2P通信:** WebSocket
* **Git操作:** JGit库
* **LLM集成:** spring-ai-mcp-server-spring-boot-starter
* **前端:** JavaScript SPA (React/Vue/Angular - 具体待定)
* **静态站点生成:** MkDocs
* **构建工具:** Maven
* **数据库 (配置存储):** 本地JSON文件

## 六、后续步骤与展望 (部分已完成)

1. ✅ **基础项目搭建与依赖管理 (pom.xml)**
2. ✅ **核心配置管理逻辑 (Java)**
3. ✅ **Git集成模块 (Java + JGit)**
4. ✅ **mDNS服务发现 (Java + JmDNS)**
5. ✅ **P2P WebSocket通信 (Java Spring WebFlux)**
   * ✅ 定义P2P消息格式 (DTOs)
   * ✅ 实现WebSocket Handler处理消息收发
   * ✅ 实现P2PCoordinatorService进行广播和节点管理
   * ✅ **支持仓库别名变更的P2P同步 (`oldRepoAlias`)**
6. ✅ **RESTful API (Spring Boot)**
7. **MkDocs部署模块 (Java + MkDocs CLI)**
8. **LLM集成 (MCP)**
   * 定义并实现MCP工具 (Java)
9. **前端Dashboard (JavaScript SPA)**
   * 设计UI/UX
   * 实现与后端的API交互
   * 实现MkDocs站点嵌入式查看
10. **本地Git Token加密存储**
11. **打包与分发 (例如使用 jpackage)****
12. ✅ **编写README.md (持续更新)**
13. **Swagger/OpenAPI文档完善**
