package com.yuemo.demo.workflow.tool;

import com.yuemo.demo.tool.AgentTool;
import com.yuemo.demo.workflow.WorkflowEngine;
import com.yuemo.demo.workflow.WorkflowRegistry;
import com.yuemo.demo.workflow.definition.WorkflowDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WorkflowTool implements AgentTool {

    private final WorkflowEngine workflowEngine;
    private final WorkflowRegistry workflowRegistry;

    public WorkflowTool(@Lazy WorkflowEngine workflowEngine, WorkflowRegistry workflowRegistry) {
        this.workflowEngine = workflowEngine;
        this.workflowRegistry = workflowRegistry;
    }

    @Tool(description = "列出所有可用的工作流")
    public String listWorkflows() {
        try {
            List<WorkflowDefinition> workflows = workflowRegistry.getAllWorkflows();
            if (workflows.isEmpty()) {
                return "没有可用的工作流。请先在工作流目录创建工作流 YAML 文件。\n" +
                       "工作流目录: workspace/workflows/";
            }

            StringBuilder sb = new StringBuilder("可用工作流列表：\n\n");
            for (WorkflowDefinition workflow : workflows) {
                sb.append("━━━━━━━━━━━━━━━━━━━━\n");
                sb.append("ID: ").append(workflow.getId()).append("\n");
                sb.append("名称: ").append(workflow.getName()).append("\n");
                sb.append("描述: ").append(workflow.getDescription() != null ? workflow.getDescription() : "无").append("\n");
                sb.append("版本: ").append(workflow.getVersion() != null ? workflow.getVersion() : "1.0").append("\n");
                sb.append("步骤数: ").append(workflow.getSteps() != null ? workflow.getSteps().size() : 0).append("\n");
                sb.append("触发方式: ").append(workflow.getTrigger() != null ? workflow.getTrigger().getType() : "MANUAL").append("\n");
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("列出工作流失败", e);
            return "错误：列出工作流失败 - " + e.getMessage();
        }
    }

    @Tool(description = "获取工作流详细信息")
    public String getWorkflowInfo(@ToolParam(description = "工作流ID") String workflowId) {
        try {
            WorkflowDefinition workflow = workflowRegistry.getWorkflow(workflowId);
            if (workflow == null) {
                return "错误：未找到工作流 " + workflowId;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("工作流详情：\n\n");
            sb.append("ID: ").append(workflow.getId()).append("\n");
            sb.append("名称: ").append(workflow.getName()).append("\n");
            sb.append("描述: ").append(workflow.getDescription() != null ? workflow.getDescription() : "无").append("\n");
            sb.append("版本: ").append(workflow.getVersion() != null ? workflow.getVersion() : "1.0").append("\n\n");

            sb.append("触发方式: ");
            if (workflow.getTrigger() != null) {
                sb.append(workflow.getTrigger().getType());
                if (workflow.getTrigger().getExpression() != null) {
                    sb.append(" (").append(workflow.getTrigger().getExpression()).append(")");
                }
            } else {
                sb.append("MANUAL");
            }
            sb.append("\n\n");

            sb.append("步骤列表：\n");
            if (workflow.getSteps() != null && !workflow.getSteps().isEmpty()) {
                for (int i = 0; i < workflow.getSteps().size(); i++) {
                    var step = workflow.getSteps().get(i);
                    sb.append("\n  ").append(i + 1).append(". [").append(step.getType()).append("] ");
                    sb.append(step.getName());
                    if (step.getConfig() != null) {
                        var tool = step.getConfigString("tool");
                        var prompt = step.getConfigString("prompt");
                        if (tool != null) sb.append(" - 工具: ").append(tool);
                        if (prompt != null) sb.append(" - 提示: ").append(prompt.length() > 50 ? prompt.substring(0, 50) + "..." : prompt);
                    }
                }
            } else {
                sb.append("  无");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("获取工作流信息失败", e);
            return "错误：获取工作流信息失败 - " + e.getMessage();
        }
    }

    @Tool(description = "手动执行工作流")
    public String runWorkflow(
            @ToolParam(description = "工作流ID") String workflowId,
            @ToolParam(description = "输入上下文（JSON格式，可选）", required = false) String inputContext) {
        try {
            WorkflowDefinition workflow = workflowRegistry.getWorkflow(workflowId);
            if (workflow == null) {
                return "错误：未找到工作流 " + workflowId;
            }

            Map<String, Object> context = null;
            if (inputContext != null && !inputContext.trim().isEmpty()) {
                context = parseSimpleJson(inputContext);
            }

            log.info("手动执行工作流: {}", workflowId);
            String result = workflowEngine.executeWorkflow(workflowId, context);

            return "工作流执行完成！\n\n" +
                   "工作流: " + workflow.getName() + "\n" +
                   "结果:\n" + result;
        } catch (Exception e) {
            log.error("执行工作流失败", e);
            return "错误：执行工作流失败 - " + e.getMessage();
        }
    }

    @Tool(description = "创建简单的工作流")
    public String createSimpleWorkflow(
            @ToolParam(description = "工作流ID") String workflowId,
            @ToolParam(description = "工作流名称") String workflowName,
            @ToolParam(description = "工作流描述") String workflowDescription,
            @ToolParam(description = "步骤描述（用 | 分隔每步）") String stepsDescription) {
        try {
            StringBuilder yaml = new StringBuilder();
            yaml.append("id: ").append(workflowId).append("\n");
            yaml.append("name: ").append(workflowName).append("\n");
            yaml.append("description: ").append(workflowDescription).append("\n");
            yaml.append("version: 1.0\n");
            yaml.append("trigger:\n");
            yaml.append("  type: MANUAL\n");
            yaml.append("steps:\n");

            String[] steps = stepsDescription.split("\\|");
            int stepNum = 1;
            for (String stepDesc : steps) {
                stepDesc = stepDesc.trim();
                if (stepDesc.isEmpty()) continue;

                String[] parts = stepDesc.split("->");
                String stepName = parts[0].trim();
                String stepType = "TOOL";
                String toolName = "";

                if (parts.length > 1) {
                    String action = parts[1].trim().toLowerCase();
                    if (action.contains("发送") || action.contains("feishu")) {
                        stepType = "NOTIFY";
                        toolName = "FEISHU";
                    } else if (action.contains("搜索") || action.contains("search")) {
                        stepType = "TOOL";
                        toolName = "web_search";
                    } else if (action.contains("分析") || action.contains("生成")) {
                        stepType = "AGENT";
                    }
                }

                yaml.append("  - id: step_").append(stepNum).append("\n");
                yaml.append("    name: ").append(stepName).append("\n");
                yaml.append("    type: ").append(stepType).append("\n");
                if (!toolName.isEmpty()) {
                    yaml.append("    tool: ").append(toolName).append("\n");
                }
                yaml.append("\n");
                stepNum++;
            }

            String filePath = "workspace/workflows/" + workflowId + ".yaml";
            java.nio.file.Files.writeString(
                java.nio.file.Paths.get(filePath),
                yaml.toString()
            );

            workflowRegistry.reloadWorkflows();

            return "工作流创建成功！\n\n" +
                   "工作流ID: " + workflowId + "\n" +
                   "文件路径: " + filePath + "\n" +
                   "可以使用 run_workflow 工具运行它";

        } catch (Exception e) {
            log.error("创建工作流失败", e);
            return "错误：创建工作流失败 - " + e.getMessage();
        }
    }

    @Tool(description = "获取工作流执行状态")
    public String getWorkflowExecutionStatus(@ToolParam(description = "执行ID") String executionId) {
        try {
            var execution = workflowEngine.getExecution(executionId);
            if (execution == null) {
                return "执行已完成或不存在: " + executionId;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("执行状态：\n\n");
            sb.append("执行ID: ").append(execution.getId()).append("\n");
            sb.append("状态: ").append(execution.getStatus()).append("\n");
            sb.append("开始时间: ").append(execution.getStartTime()).append("\n");
            if (execution.getEndTime() != null) {
                sb.append("结束时间: ").append(execution.getEndTime()).append("\n");
                sb.append("耗时: ").append(execution.getDurationMs()).append("ms\n");
            }
            sb.append("当前步骤: ").append(execution.getCurrentStepId()).append("\n");

            return sb.toString();
        } catch (Exception e) {
            log.error("获取执行状态失败", e);
            return "错误：获取执行状态失败 - " + e.getMessage();
        }
    }

    private Map<String, Object> parseSimpleJson(String json) {
        Map<String, Object> result = new HashMap<>();
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
            String[] pairs = json.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "").replace("'", "");
                    String value = kv[1].trim().replace("\"", "").replace("'", "");
                    result.put(key, value);
                }
            }
        }
        return result;
    }
}