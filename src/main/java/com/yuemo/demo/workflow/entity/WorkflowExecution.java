package com.yuemo.demo.workflow.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecution {

    private String id;
    private String workflowId;
    private String workflowName;
    private ExecutionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long durationMs;
    private String currentStepId;
    private List<StepResult> stepResults;
    private Object finalResult;
    private String errorMessage;
    private String triggerType;

    public enum ExecutionStatus {
        RUNNING,
        SUCCESS,
        FAILED,
        CANCELLED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepResult {
        private String stepId;
        private String stepName;
        private boolean success;
        private Object input;
        private Object output;
        private String error;
        private long durationMs;
    }

    public void addStepResult(StepResult result) {
        if (stepResults == null) {
            stepResults = new ArrayList<>();
        }
        stepResults.add(result);
    }

    public void markSuccess(Object result) {
        this.status = ExecutionStatus.SUCCESS;
        this.finalResult = result;
        this.endTime = LocalDateTime.now();
        this.durationMs = java.time.Duration.between(startTime, endTime).toMillis();
    }

    public void markFailed(String error) {
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = error;
        this.endTime = LocalDateTime.now();
        this.durationMs = java.time.Duration.between(startTime, endTime).toMillis();
    }

    public void setStepOutput(String stepId, Object output) {
        if (stepResults == null) {
            stepResults = new ArrayList<>();
        }
        for (StepResult result : stepResults) {
            if (result.getStepId().equals(stepId)) {
                result.setOutput(output);
                return;
            }
        }
    }

    public Object getStepOutput(String stepId) {
        if (stepResults == null) return null;
        for (StepResult result : stepResults) {
            if (result.getStepId().equals(stepId)) {
                return result.getOutput();
            }
        }
        return null;
    }
}