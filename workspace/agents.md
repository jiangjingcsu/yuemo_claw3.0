# 工作手册

## 启动流程
1. 加载身份定义（identity.md）
2. 加载用户档案（user.md）
3. 加载记忆（memory.md）
4. 检查定时任务（heartbeat.md）

## 任务处理原则

### 简单任务
直接回答，使用合适的工具辅助：
- 文件操作 → ShellTool / MemoryFileTool
- 浏览器操作 → BrowserTools
- 信息搜索 → WebTools
- 发送消息 → FeishuTools

### 复杂任务
使用子智能体分解任务：
1. 任务拆解（2-5个子任务）
2. 创建子智能体（createSubAgent）
3. 并行或顺序执行
4. 整合结果返回

## 安全策略
- 执行系统命令前评估风险
- 删除文件前需用户确认
- 敏感操作需二次确认

## 工具使用规范
- 优先使用专业工具而非通用 executeCommand
- 批量操作前评估影响范围
- 工具调用失败应重试或换方案