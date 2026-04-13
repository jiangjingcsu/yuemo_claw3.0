# YuEmo Claw 3.0

基于 Spring AI Alibaba 和 阿里云百炼 API 的智能助手项目，支持飞书、钉钉、Swing 等多通道消息收发，集成定时任务、工作流引擎、浏览器自动化等高级功能。

## 核心特性

- **多通道支持** - 飞书、钉钉、Swing 桌面客户端
- **事件驱动架构** - 基于 EventBus 的松耦合设计
- **技能系统** - 支持 OpenClaw/skills.sh 生态系统
- **定时任务** - 基于 Cron 的智能调度
- **工作流引擎** - YAML 定义复杂业务流程
- **浏览器自动化** - Playwright 驱动的网页操作
- **记忆文件系统** - 日记、笔记、偏好存储
- **子智能体** - 并行任务执行

## 系统架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              用户层 (Users)                                  │
│   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                        │
│   │   飞书用户   │  │   钉钉用户   │  │  桌面用户   │                        │
│   └──────┬──────┘  └──────┬──────┘  └──────┬──────┘                        │
└──────────┼────────────────┼────────────────┼────────────────────────────────┘
           │                │                │
           ▼                ▼                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            通道层 (Channel Layer)                             │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                        MessageGateway (消息网关)                          │ │
│  │   - 消息路由分发  - 通道注册管理  - 事件发布订阅                          │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│           │                      │                      │                     │
│  ┌────────┴────────┐   ┌────────┴────────┐   ┌────────┴────────┐           │
│  │  FeishuChannel │   │ DingTalkChannel │   │  SwingChannel   │           │
│  └────────────────┘   └────────────────┘   └────────────────┘           │
└─────────────────────────────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           事件层 (Event Layer)                               │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                          EventBus (事件总线)                            │ │
│  │              - 统一事件发布/订阅  - 通道间解耦                           │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           服务层 (Service Layer)                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                       AgentService (智能体服务)                          │ │
│  │              - ReactAgent  - 工具调用  - 提示词管理                      │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                   │                                           │
│  ┌────────────────────────────────┴────────────────────────────────────────┐ │
│  │                          ToolRegistry (工具注册中心)                     │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│           │            │            │            │            │             │
│  ┌────────┴───┐ ┌──────┴───┐ ┌──────┴───┐ ┌──────┴───┐ ┌────────┴───┐      │
│  │ FeishuTools│ │ Browser  │ │ Scheduler│ │ Workflow │ │   Voice   │      │
│  │            │ │  Tools   │ │   Tool   │ │   Tool   │ │   Tool    │      │
│  └────────────┘ └──────────┘ └──────────┘ └──────────┘ └────────────┘      │
│                                                                       ┌────┴────┐
│                                                                       │ Skills  │
│                                                                       │ 技能系统 │
│                                                                       └─────────┘
└─────────────────────────────────────────────────────────────────────────────┘
```

## 消息流转

```
1. 用户发送消息 ──▶ 飞书/钉钉/Swing

2. 通道接收消息
   FeishuChannel (WebSocket)
   DingTalkChannel (HTTP)
   SwingChannel (GUI)

3. 发布 MESSAGE_RECEIVED 事件
   Channel ──▶ EventBus ──▶ MessageGateway

4. MessageGateway 路由
   - 鉴权检查
   - 会话管理
   - 转发给 AgentService

5. AI 处理
   AgentService ──▶ ReactAgent ──▶ 调用工具 ──▶ 返回响应

6. 发布 MESSAGE_SENT 事件
   MessageGateway ──▶ EventBus ──▶ 发送响应给用户
```

## 提示词文件结构

项目使用 OpenClaw 标准的提示词文件结构，位于 `workspace/` 目录：

| 文件 | 说明 |
|------|------|
| `identity.md` | 身份定义（名称、性格、自称规则） |
| `soul.md` | 核心灵魂（思维模式、价值观、边界红线） |
| `agents.md` | 工作手册（启动流程、任务处理原则、安全策略） |
| `user.md` | 用户档案（偏好、使用习惯） |
| `memory.md` | 长期记忆 |
| `heartbeat.md` | 定时任务配置 |
| `tools.md` | 工具清单 |

## 可用工具

### 消息发送
- `sendTextToFeishu` - 发送文本消息到飞书
- `sendFileToFeishu` - 发送文件到飞书

### 浏览器自动化
- `browserNavigate` - 导航到 URL
- `browserClick` - 点击页面元素
- `browserType` - 输入文本
- `browserScreenshot` - 页面截图
- `browserGetContent` - 获取页面 HTML

### 定时任务
- `createScheduledTask` - 创建 Cron 定时任务
- `createDelayedTask` - 创建延迟任务
- `listScheduledTasks` - 列出任务
- `pauseScheduledTask` - 暂停任务
- `resumeScheduledTask` - 恢复任务
- `cancelScheduledTask` - 取消任务
- `runTaskNow` - 立即执行任务

### 工作流
- `listWorkflows` - 列出工作流
- `runWorkflow` - 执行工作流
- `createSimpleWorkflow` - 创建简单工作流

### 记忆管理
- `readTodayNote` - 读取今日日记
- `writeTodayNote` - 写入日记
- `appendToTodayNote` - 追加日记
- `searchNotes` - 搜索笔记
- `readNote` - 读取笔记
- `writeNote` - 写入笔记
- `savePreference` - 保存偏好
- `getPreference` - 获取偏好

### 子智能体
- `createSubAgent` - 创建子智能体
- `runSubAgent` - 运行子智能体
- `getSubAgentResult` - 获取执行结果
- `listSubAgents` - 列出子智能体
- `deleteSubAgent` - 删除子智能体

### 语音服务
- `textToSpeech` - 文字转语音
- `speechToText` - 语音转文字
- `getVoiceServiceStatus` - 获取语音服务状态

### 技能系统
- `read_skill` - 读取技能详情
- `search_skills` - 搜索技能

## 技能列表

| 技能名称 | 功能说明 |
|---------|---------|
| baidu-search | 百度 AI 搜索 |
| browser-automator | 浏览器自动化操作 |
| docx-skill | Word 文档处理 |
| pdf-extractor | PDF 文本提取 |
| git-manager | Git 操作管理 |
| json-validator | JSON 验证 |
| poetry-skill | 诗歌创作 |
| find-skills | 技能发现 |

## 技术栈

| 组件 | 技术 |
|------|------|
| 核心框架 | Spring Boot 3.5 |
| AI 框架 | Spring AI Alibaba 1.1.2.0 |
| 飞书 SDK | oapi-sdk 2.5.3 |
| 钉钉 SDK | dingtalk 2.2.21 |
| 浏览器自动化 | Playwright 1.40.0 |
| 构建工具 | Maven |
| 代码简化 | Lombok |

## 项目结构

```
src/main/java/com/yuemo/demo/
├── SpringAiAlibabaDemoApplication.java     # 主应用入口
│
├── channel/                                # 消息通道层
│   ├── Channel.java                        # 通道接口
│   ├── AbstractChannel.java               # 通道抽象基类
│   ├── ChannelManager.java                # 通道管理器
│   ├── ChannelType.java                   # 通道类型枚举
│   ├── feishu/                            # 飞书通道
│   ├── dingtalk/                          # 钉钉通道
│   └── swing/                             # 桌面客户端
│
├── event/                                  # 事件层
│   ├── Event.java                         # 事件基类
│   ├── EventBus.java                      # 事件总线
│   └── definitions/                        # 事件定义
│
├── gateway/                                # 网关层
│   └── MessageGateway.java                # 消息网关
│
├── agent/                                 # 智能体层
│   ├── AgentService.java                  # AI 智能体服务
│   ├── SystemPromptLoader.java            # 提示词加载
│   └── ToolRegistry.java                  # 工具注册
│
├── tool/                                  # 工具层
│   ├── AgentTool.java                     # 工具接口
│   ├── FeishuTools.java                   # 飞书工具
│   ├── BrowserTools.java                  # 浏览器工具
│   ├── ShellTool2.java                    # Shell 工具
│   ├── SubAgentTool.java                  # 子智能体工具
│   └── ...
│
├── scheduler/                             # 定时任务模块
│   ├── SchedulerService.java              # 调度服务
│   ├── entity/                            # 实体类
│   ├── repository/                        # 数据仓储
│   └── tool/                              # 调度工具
│
├── workflow/                              # 工作流模块
│   ├── WorkflowEngine.java                # 工作流引擎
│   ├── WorkflowRegistry.java              # 工作流注册
│   ├── definition/                        # 定义类
│   └── tool/                              # 工作流工具
│
├── voice/                                 # 语音模块
│   ├── VoiceService.java                  # 语音服务
│   └── tool/                              # 语音工具
│
├── memory/                                # 记忆模块
│   ├── SessionContextHolder.java          # 会话上下文
│   └── file/                              # 文件记忆
│
└── common/                               # 公共组件
    ├── config/                            # 配置类
    └── event/                             # 事件公共类

workspace/
├── *.md                                   # 提示词文件
├── skills/                               # 技能目录
├── workflows/                            # 工作流定义
├── memory/                               # 记忆存储
│   ├── daily/                            # 日记
│   ├── notes/                            # 笔记
│   └── preferences/                      # 偏好
└── data/                                # 数据存储
```

## 快速开始

### 配置

1. 设置环境变量：
```bash
export ALI_API_KEYS=your_api_key
```

2. 配置业务通道（`workspace/config.json`）：
```json
{
  "feishu": {
    "appId": "your_app_id",
    "appSecret": "your_app_secret",
    "enabled": true
  }
}
```

### 运行

```bash
mvn clean compile
mvn spring-boot:run
```

服务启动后访问 http://localhost:8080

## License

MIT
