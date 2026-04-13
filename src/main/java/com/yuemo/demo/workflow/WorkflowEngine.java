package com.yuemo.demo.workflow;

import com.yuemo.demo.agent.AgentService;
import com.yuemo.demo.channel.ChannelManager;
import com.yuemo.demo.channel.ChannelType;
import com.yuemo.demo.common.event.EventBus;
import com.yuemo.demo.common.event.definitions.ChannelEvents;
import com.yuemo.demo.common.event.definitions.MessageContext;
import com.yuemo.demo.common.event.definitions.MessageType;
import com.yuemo.demo.memory.SessionContextHolder;
import com.yuemo.demo.tool.FeishuTools;
import com.yuemo.demo.workflow.definition.WorkflowDefinition;
import com.yuemo.demo.workflow.definition.WorkflowDefinition.StepType;
import com.yuemo.demo.workflow.definition.WorkflowStep;
import com.yuemo.demo.workflow.entity.WorkflowExecution;
import com.yuemo.demo.workflow.entity.WorkflowExecution.StepResult;
import com.yuemo.demo.workflow.event.WorkflowEvents;
import com.yuemo.demo.workflow.event.WorkflowEvents.WorkflowStepCompletedEvent;
import com.yuemo.demo.workflow.event.WorkflowEvents.WorkflowStepStartedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class WorkflowEngine {

    private final WorkflowRegistry workflowRegistry;
    private final AgentService agentService;
    private final EventBus eventBus;
    private final ChannelManager channelManager;

    private final Map<String, WorkflowExecution> runningExecutions = new ConcurrentHashMap<>();
    private final Map<String, Object> globalContext = new ConcurrentHashMap<>();

    public WorkflowEngine(WorkflowRegistry workflowRegistry,
                         AgentService agentService,
                         EventBus eventBus,
                         ChannelManager channelManager) {
        this.workflowRegistry = workflowRegistry;
        this.agentService = agentService;
        this.eventBus = eventBus;
        this.channelManager = channelManager;
    }

    @PostConstruct
    public void init() {
        log.info("工作流引擎初始化完成");
    }

    public String executeWorkflow(String workflowId) {
        return executeWorkflow(workflowId, null);
    }

    public String executeWorkflow(String workflowId, Map<String, Object> inputContext) {
        WorkflowDefinition workflow = workflowRegistry.getWorkflow(workflowId);
        if (workflow == null) {
            return "错误：未找到工作流 " + workflowId;
        }

        String executionId = "exec_" + System.currentTimeMillis();
        WorkflowExecution execution = WorkflowExecution.builder()
                .id(executionId)
                .workflowId(workflowId)
                .workflowName(workflow.getName())
                .status(WorkflowExecution.ExecutionStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .triggerType("MANUAL")
                .build();

        runningExecutions.put(executionId, execution);
        eventBus.publish(new WorkflowEvents.WorkflowStartedEvent(workflow, executionId));

        log.info("开始执行工作流: {} (executionId: {})", workflow.getName(), executionId);

        try {
            SessionContextHolder.setContext(executionId, "workflow");

            Map<String, Object> context = new HashMap<>();
            if (inputContext != null) {
                context.putAll(inputContext);
            }

            Object result = executeSteps(workflow, execution, context, inputContext);

            execution.markSuccess(result);
            eventBus.publish(new WorkflowEvents.WorkflowCompletedEvent(executionId, workflow, result));

            log.info("工作流执行完成: {} (executionId: {}, duration: {}ms)",
                    workflow.getName(), executionId, execution.getDurationMs());

            return result != null ? result.toString() : "工作流执行完成";

        } catch (Exception e) {
            log.error("工作流执行失败: {} (executionId: {})", workflow.getName(), executionId, e);
            execution.markFailed(e.getMessage());
            eventBus.publish(new WorkflowEvents.WorkflowFailedEvent(executionId, workflow, e.getMessage()));
            return "工作流执行失败: " + e.getMessage();

        } finally {
            runningExecutions.remove(executionId);
            SessionContextHolder.clear();
        }
    }

    private Object executeSteps(WorkflowDefinition workflow, WorkflowExecution execution,
                               Map<String, Object> context, Map<String, Object> inputContext) throws Exception {

        if (workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            return "工作流没有步骤";
        }

        Object lastResult = null;

        for (int i = 0; i < workflow.getSteps().size(); i++) {
            WorkflowStep step = workflow.getSteps().get(i);
            execution.setCurrentStepId(step.getId());

            eventBus.publish(new WorkflowStepStartedEvent(execution.getId(), workflow, step, i));

            log.info("执行工作流步骤: {} (step: {}, type: {})", step.getName(), step.getId(), step.getType());

            long startTime = System.currentTimeMillis();

            try {
                Object result = executeStep(step, context, lastResult);

                long duration = System.currentTimeMillis() - startTime;

                StepResult stepResult = StepResult.builder()
                        .stepId(step.getId())
                        .stepName(step.getName())
                        .success(true)
                        .input(lastResult)
                        .output(result)
                        .durationMs(duration)
                        .build();

                execution.addStepResult(stepResult);
                context.put("lastResult", result);
                context.put("step_" + step.getId() + "_result", result);

                lastResult = result;

                eventBus.publish(new WorkflowStepCompletedEvent(execution.getId(), workflow, step, i, result));

                log.info("步骤执行成功: {} (duration: {}ms)", step.getName(), duration);

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;

                StepResult stepResult = StepResult.builder()
                        .stepId(step.getId())
                        .stepName(step.getName())
                        .success(false)
                        .input(lastResult)
                        .error(e.getMessage())
                        .durationMs(duration)
                        .build();

                execution.addStepResult(stepResult);

                if (step.getErrorStepId() != null) {
                    log.info("步骤失败，执行错误处理分支: {}", step.getErrorStepId());
                    lastResult = "步骤执行出错: " + e.getMessage();
                } else {
                    throw e;
                }
            }
        }

        return lastResult;
    }

    private Object executeStep(WorkflowStep step, Map<String, Object> context, Object previousResult) throws Exception {
        switch (step.getType()) {
            case TOOL:
                return executeToolStep(step, context, previousResult);
            case AGENT:
                return executeAgentStep(step, context, previousResult);
            case NOTIFY:
                return executeNotifyStep(step, context, previousResult);
            case CONDITION:
                return executeConditionStep(step, context, previousResult);
            case TRANSFORM:
                return executeTransformStep(step, context, previousResult);
            default:
                return "不支持的步骤类型: " + step.getType();
        }
    }

    private Object executeToolStep(WorkflowStep step, Map<String, Object> context, Object previousResult) {
        String toolName = step.getToolName();
        if (toolName == null) {
            return "错误：TOOL 步骤未指定 tool 名称";
        }

        log.info("执行工具步骤: {}", toolName);

        switch (toolName.toLowerCase()) {
            case "feishu":
            case "send_feishu":
                String message = step.getConfigString("message");
                if (message == null && previousResult != null) {
                    message = previousResult.toString();
                }
                String targetUserId = step.getConfigString("user_id");
                if (targetUserId == null) {
                    targetUserId = SessionContextHolder.getCurrentUserId();
                }
                if (message != null && targetUserId != null) {
                    MessageContext msgContext = new MessageContext();
                    msgContext.setChannelType(ChannelType.FEISHU);
                    msgContext.setUserId("workflow");
                    msgContext.setUserName("工作流");
                    msgContext.setTargetUserId(targetUserId);
                    msgContext.setContent(message);
                    msgContext.setMessageType(MessageType.TEXT);
                    eventBus.publish(ChannelEvents.createToolMessageRequestEvent(msgContext));
                    return "消息已发送: " + message;
                }
                return "发送消息参数不完整";

            case "web_search":
            case "search":
                String keyword = step.getConfigString("keyword");
                if (keyword == null && previousResult != null) {
                    keyword = previousResult.toString();
                }
                return "模拟搜索结果: " + keyword;

            default:
                return "未知工具: " + toolName;
        }
    }

    private Object executeAgentStep(WorkflowStep step, Map<String, Object> context, Object previousResult) {
        String prompt = step.getPrompt();
        if (prompt == null) {
            prompt = previousResult != null ? previousResult.toString() : "";
        }

        prompt = interpolateString(prompt, context);

        log.info("执行 Agent 步骤，提示词: {}", prompt);
        String response = agentService.processMessage(prompt);
        return response;
    }

    private Object executeNotifyStep(WorkflowStep step, Map<String, Object> context, Object previousResult) {
        String channelName = step.getChannel();
        String message = step.getMessage();
        if (message == null && previousResult != null) {
            message = previousResult.toString();
        }

        message = interpolateString(message, context);

        try {
            ChannelType channelType = ChannelType.valueOf(channelName.toUpperCase());

            MessageContext msgContext = new MessageContext();
            msgContext.setChannelType(channelType);
            msgContext.setUserId("workflow");
            msgContext.setUserName("工作流");
            msgContext.setTargetUserId(SessionContextHolder.getCurrentUserId());
            msgContext.setContent(message);
            msgContext.setMessageType(MessageType.TEXT);

            eventBus.publish(ChannelEvents.createToolMessageRequestEvent(msgContext));

            log.info("通知已发送到 {}: {}", channelType, message);
            return "通知已发送";

        } catch (Exception e) {
            log.error("发送通知失败", e);
            return "通知发送失败: " + e.getMessage();
        }
    }

    private Object executeConditionStep(WorkflowStep step, Map<String, Object> context, Object previousResult) {
        String expression = step.getCondition() != null ? step.getCondition().getExpression() : null;
        if (expression == null) {
            return previousResult;
        }

        expression = interpolateString(expression, context);

        boolean conditionResult = evaluateCondition(expression, context, previousResult);

        context.put("condition_result", conditionResult);
        context.put("lastResult", previousResult);

        if (conditionResult && step.getNextStepId() != null) {
            return "条件为真";
        } else if (!conditionResult && step.getErrorStepId() != null) {
            return "条件为假";
        }

        return previousResult;
    }

    private Object executeTransformStep(WorkflowStep step, Map<String, Object> context, Object previousResult) {
        String transform = step.getConfigString("transform");
        if (transform == null) {
            return previousResult;
        }

        String result = interpolateString(transform, context);
        return result;
    }

    private boolean evaluateCondition(String expression, Map<String, Object> context, Object previousResult) {
        expression = expression.toLowerCase().trim();

        if (expression.equals("true") || expression.equals("yes") || expression.equals("1")) {
            return true;
        }
        if (expression.equals("false") || expression.equals("no") || expression.equals("0")) {
            return false;
        }

        if (expression.contains("contains")) {
            String searchIn = interpolateString(expression.replace("contains(", "").replace(")", "").split(",")[0], context);
            String searchFor = interpolateString(expression.replace("contains(", "").replace(")", "").split(",")[1], context);
            return searchIn.contains(searchFor);
        }

        if (previousResult != null) {
            String prevStr = previousResult.toString().toLowerCase();
            return prevStr.contains(expression);
        }

        return false;
    }

    private String interpolateString(String template, Map<String, Object> context) {
        if (template == null) return null;

        String result = template;

        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }

    public WorkflowExecution getExecution(String executionId) {
        return runningExecutions.get(executionId);
    }

    public void cancelExecution(String executionId) {
        runningExecutions.remove(executionId);
        log.info("工作流执行已取消: {}", executionId);
    }

    public Map<String, Object> getGlobalContext() {
        return new HashMap<>(globalContext);
    }

    public void setGlobalContextValue(String key, Object value) {
        globalContext.put(key, value);
    }
}