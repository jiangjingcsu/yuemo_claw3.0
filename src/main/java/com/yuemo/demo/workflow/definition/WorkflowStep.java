package com.yuemo.demo.workflow.definition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStep {

    private String id;
    private String name;
    private WorkflowDefinition.StepType type;
    private Map<String, Object> config;
    private String nextStepId;
    private String errorStepId;
    private Condition condition;
    private List<WorkflowStep> loopSteps;
    private int maxLoopCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Condition {
        private String type;
        private String expression;
        private Map<String, String> outcomes;
    }

    public Object getConfigValue(String key) {
        return config != null ? config.get(key) : null;
    }

    public String getConfigString(String key) {
        Object value = getConfigValue(key);
        return value != null ? value.toString() : null;
    }

    public String getToolName() {
        return getConfigString("tool");
    }

    public String getPrompt() {
        return getConfigString("prompt");
    }

    public String getChannel() {
        return getConfigString("channel");
    }

    public String getMessage() {
        return getConfigString("message");
    }
}