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
public class WorkflowDefinition {

    private String id;
    private String name;
    private String description;
    private String version;
    private Trigger trigger;
    private List<WorkflowStep> steps;
    private Map<String, Object> config;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Trigger {
        private TriggerType type;
        private String expression;
        private String channelType;
    }

    public enum TriggerType {
        MANUAL,
        CRON,
        EVENT,
        WEBHOOK
    }

    public enum StepType {
        TOOL,
        AGENT,
        CONDITION,
        LOOP,
        NOTIFY,
        TRANSFORM,
        FILTER
    }
}