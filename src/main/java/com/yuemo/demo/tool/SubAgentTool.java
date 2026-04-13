package com.yuemo.demo.tool;

import com.yuemo.demo.agent.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SubAgentTool implements AgentTool {

    private final Map<String, String> subAgents = new ConcurrentHashMap<>();
    private final Map<String, String> subAgentResults = new ConcurrentHashMap<>();

    private final AgentService agentService;

    public SubAgentTool(@Lazy AgentService agentService) {
        this.agentService = agentService;
    }

    @Tool(description = "创建一个子智能体，用于执行特定任务")
    public String createSubAgent(
            @ToolParam(description = "子智能体名称") String name,
            @ToolParam(description = "子智能体描述和能力说明") String description) {
        log.info("创建子智能体: {}", name);

        if (name == null || name.isBlank()) {
            return "错误：子智能体名称不能为空";
        }

        if (subAgents.containsKey(name)) {
            return "错误：子智能体已存在 - " + name;
        }

        subAgents.put(name, description);
        subAgentResults.put(name, "");

        return "成功：子智能体已创建 - " + name + "\n描述: " + description;
    }

    @Tool(description = "运行指定的子智能体执行任务")
    public String runSubAgent(
            @ToolParam(description = "子智能体名称") String name,
            @ToolParam(description = "要执行的任务描述") String task) {
        log.info("运行子智能体: {}, 任务: {}", name, task);

        if (!subAgents.containsKey(name)) {
            return "错误：子智能体不存在 - " + name;
        }

        String description = subAgents.get(name);

        try {
            String prompt = String.format(
                    "【子智能体: %s】\n" +
                    "【能力描述: %s】\n" +
                    "【任务: %s】\n\n" +
                    "请根据上述信息，执行任务并返回结果。",
                    name, description, task
            );

            log.info("子智能体 {} 开始执行任务", name);
            String result = agentService.processMessage(prompt);

            subAgentResults.put(name, result);
            log.info("子智能体 {} 任务执行完成", name);

            return "子智能体: " + name + "\n" +
                   "描述: " + description + "\n" +
                   "任务: " + task + "\n" +
                   "执行结果:\n" + result;

        } catch (Exception e) {
            log.error("子智能体 {} 执行失败", name, e);
            return "子智能体 " + name + " 执行失败: " + e.getMessage();
        }
    }

    @Tool(description = "获取子智能体上一次执行的结果")
    public String getSubAgentResult(
            @ToolParam(description = "子智能体名称") String name) {
        if (!subAgents.containsKey(name)) {
            return "错误：子智能体不存在 - " + name;
        }

        String result = subAgentResults.get(name);
        if (result == null || result.isEmpty()) {
            return "子智能体 " + name + " 还没有执行过任务，或结果已被清除";
        }

        return "子智能体 " + name + " 的执行结果:\n\n" + result;
    }

    @Tool(description = "列出所有已创建的子智能体")
    public String listSubAgents() {
        log.info("列出所有子智能体");

        if (subAgents.isEmpty()) {
            return "当前没有已创建的子智能体。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("已创建的子智能体:\n");

        subAgents.forEach((name, desc) -> {
            sb.append("━━━━━━━━━━━━━━━━━━━━\n");
            sb.append("名称: ").append(name).append("\n");
            sb.append("描述: ").append(desc).append("\n");
            String lastResult = subAgentResults.get(name);
            if (lastResult != null && !lastResult.isEmpty()) {
                sb.append("状态: 已执行\n");
            } else {
                sb.append("状态: 未执行\n");
            }
        });

        return sb.toString();
    }

    @Tool(description = "删除指定的子智能体")
    public String deleteSubAgent(
            @ToolParam(description = "要删除的子智能体名称") String name) {
        log.info("删除子智能体: {}", name);

        if (!subAgents.containsKey(name)) {
            return "错误：子智能体不存在 - " + name;
        }

        subAgents.remove(name);
        subAgentResults.remove(name);

        return "成功：子智能体已删除 - " + name;
    }
}
