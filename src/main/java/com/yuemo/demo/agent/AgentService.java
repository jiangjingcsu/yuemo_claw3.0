package com.yuemo.demo.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool2;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import com.yuemo.demo.memory.entity.MessageRecord;
import com.yuemo.demo.memory.entity.ChatSession;
import com.yuemo.demo.memory.service.MemoryService;
import com.yuemo.demo.tool.TimeTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AgentService {

    private ReactAgent reactAgent;
    private String systemPrompt;
    private final ToolRegistry toolRegistry;
    private final ToolCallback[] allToolCallbacks;
    private boolean agentInitialized;
    private final ChatModel chatModel;
    private final SystemPromptLoader systemPromptLoader;
    private final MemoryService memoryService;

    private static final String SKILLS_DIR = "workspace/skills";
    private static final int DEFAULT_CONTEXT_SIZE = 20;

    public AgentService(ChatModel chatModel, ToolRegistry toolRegistry,
                       SystemPromptLoader systemPromptLoader, MemoryService memoryService) {
        log.info("初始化 AgentService...");
        this.chatModel = chatModel;
        this.toolRegistry = toolRegistry;
        this.systemPromptLoader = systemPromptLoader;
        this.memoryService = memoryService;
        this.allToolCallbacks = toolRegistry.getToolCallbacksArray();

        log.info("从 ToolRegistry 获取到 {} 个工具", allToolCallbacks.length);

        loadAndInitializeAgent();

        log.info("AgentService 初始化完成！");
    }

    public void loadAndInitializeAgent() {
        log.info("正在从文件加载系统提示词...");

        this.systemPrompt = loadSystemPromptFromFile();
        log.info("系统提示词加载完成");

        ReactAgent tempAgent = null;
        boolean tempInitialized = false;

        try {
            log.info("正在初始化 SkillRegistry...");
            SkillRegistry skillRegistry = FileSystemSkillRegistry.builder()
                    .projectSkillsDirectory(SKILLS_DIR)
                    .build();

            log.info("正在初始化 SkillsAgentHook...");
            SkillsAgentHook skillsHook = SkillsAgentHook.builder()
                    .skillRegistry(skillRegistry)
                    .build();

            log.info("正在初始化 ShellToolAgentHook...");
            ShellToolAgentHook shellHook = ShellToolAgentHook.builder()
                    .shellTool2(ShellTool2.builder(System.getProperty("user.dir")).build())
                    .build();

            log.info("正在构建 ReactAgent...");
            tempAgent = ReactAgent.builder()
                    .name("multi_agent_assistant")
                    .model(chatModel)
                    .tools(allToolCallbacks)
                    .systemPrompt(systemPrompt)
                    .saver(new MemorySaver())
                    .hooks(List.of(skillsHook, shellHook))
                    .build();
            tempInitialized = true;
            log.info("ReactAgent 初始化成功！");
        } catch (Exception e) {
            log.error("ReactAgent 初始化失败，使用模拟模式", e);
        }

        this.reactAgent = tempAgent;
        this.agentInitialized = tempInitialized;
    }

    private String loadSystemPromptFromFile() {
        log.info("通过 SystemPromptLoader 加载系统提示词...");
        log.info("加载的文件列表: {}", systemPromptLoader.getLoadedFileNames());
        return systemPromptLoader.getSystemPrompt();
    }

    public String getOrCreateCurrentSession(String userId) {
        return memoryService.getOrCreateCurrentSession(userId);
    }

    public String processMessage(String userMessage, String sessionId) {
        log.info("处理用户消息: sessionId={}, message={}", sessionId, userMessage);

        try {
            memoryService.addUserMessage(sessionId, userMessage);

            String context = memoryService.getRecentContextBySessionId(sessionId, DEFAULT_CONTEXT_SIZE);
            String fullPrompt = context + userMessage;

            String response;
            if (!agentInitialized || reactAgent == null) {
                response = processMessageFallback(userMessage);
            } else {
                try {
                    log.info("通过 ReactAgent 处理消息...");
                    AssistantMessage aiResponse = reactAgent.call(fullPrompt);
                    response = aiResponse.getText();
                } catch (Exception e) {
                    log.error("ReactAgent 调用失败，使用降级处理", e);
                    response = processMessageFallback(userMessage);
                }
            }

            memoryService.addAssistantMessage(sessionId, response);
            return response;
        } catch (Exception e) {
            log.error("处理消息异常", e);
            throw new RuntimeException("处理消息失败", e);
        }
    }

    public String processMessage(String userMessage) {
        log.info("处理用户消息（无会话）: {}", userMessage);

        if (!agentInitialized || reactAgent == null) {
            return processMessageFallback(userMessage);
        }

        try {
            log.info("通过 ReactAgent 处理消息...");
            AssistantMessage response = reactAgent.call(userMessage);
            return response.getText();
        } catch (Exception e) {
            log.error("ReactAgent 调用失败，使用降级处理", e);
            return processMessageFallback(userMessage);
        }
    }

    public List<ChatSession> getUserSessions(String userId, int limit) {
        return memoryService.getUserSessions(userId, limit);
    }

    public void switchSession(String userId, String sessionId) {
        memoryService.switchSession(userId, sessionId);
    }

    public List<MessageRecord> getSessionMessages(String userId, int offset, int limit) {
        String sessionId = memoryService.getOrCreateCurrentSession(userId);
        return memoryService.getSessionMessages(sessionId, offset, limit);
    }

    public void updateSessionTitle(String sessionId, String title) {
        memoryService.updateSessionTitle(sessionId, title);
    }

    public String createNewSession(String userId, String title) {
        return memoryService.createSession(userId, title);
    }

    private String processMessageFallback(String userMessage) {
        log.info("使用降级模式处理消息");

        String lowerMessage = userMessage.toLowerCase();

        if (lowerMessage.contains("天气") || lowerMessage.contains("weather")) {
            return handleWeatherQuery(userMessage);
        } else if (lowerMessage.contains("计算") || lowerMessage.contains("+") ||
                lowerMessage.contains("-") || lowerMessage.contains("*") ||
                lowerMessage.contains("/")) {
            return handleCalculation(userMessage);
        } else if (lowerMessage.contains("时间") || lowerMessage.contains("time")) {
            return handleTimeQuery();
        } else {
            return handleGeneralQuery(userMessage);
        }
    }

    private String handleWeatherQuery(String message) {
        String city = "北京";
        int index = message.indexOf("的天气");
        if (index > 0) {
            city = message.substring(0, index).trim();
        }

        return "好的，让我帮您查询天气...\n" +
               "模拟：" + city + "的天气是晴朗，温度 25°C。";
    }

    private String handleCalculation(String message) {
        Pattern pattern = Pattern.compile(
                "(\\d+(?:\\.\\d+)?)\\s*([+\\-*/])\\s*(\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            double num1 = Double.parseDouble(matcher.group(1));
            double num2 = Double.parseDouble(matcher.group(3));
            String op = matcher.group(2);
            double result;
            String operationName;

            switch (op) {
                case "+":
                    result = num1 + num2;
                    operationName = "加法";
                    break;
                case "-":
                    result = num1 - num2;
                    operationName = "减法";
                    break;
                case "*":
                    result = num1 * num2;
                    operationName = "乘法";
                    break;
                case "/":
                    if (num2 == 0) {
                        return "错误：除数不能为0";
                    }
                    result = num1 / num2;
                    operationName = "除法";
                    break;
                default:
                    return "抱歉，不支持的运算符";
            }

            return "好的，让我帮您计算...\n" +
                   "运算: " + num1 + " " + op + " " + num2 + "\n" +
                   "结果: " + result;
        }

        return "抱歉，我无法解析这个计算表达式，请使用类似 \"256 * 12\" 这样的格式。";
    }

    private String handleTimeQuery() {
        TimeTool tool = new TimeTool();
        String result = tool.getTime(null);
        return "好的，让我帮您查询时间...\n" + result;
    }

    private String handleGeneralQuery(String message) {
        log.warn("Agent 不可用，降级处理通用查询");
        return "抱歉，AI 智能体服务暂时不可用，无法处理您的请求。\n\n" +
                "可能的原因：\n" +
                "  - AI 模型服务未正确配置\n" +
                "  - 网络连接问题\n" +
                "  - 系统负载过高\n\n" +
                "请稍后重试，或联系管理员检查系统状态。";
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    public ToolCallback[] getAllToolCallbacks() {
        return allToolCallbacks;
    }

    public void reloadSystemPrompt() {
        log.info("重新加载系统提示词...");
        loadAndInitializeAgent();
        log.info("系统提示词重新加载完成！");
    }
}