package com.yuemo.demo.scheduler.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionHistory {

    private String id;

    private String taskId;

    private String taskName;

    private ExecutionStatus status;

    private String result;

    private String errorMessage;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private long durationMs;

    public enum ExecutionStatus {
        RUNNING,
        SUCCESS,
        FAILED,
        TIMEOUT
    }

    public void markSuccess(String result) {
        this.status = ExecutionStatus.SUCCESS;
        this.result = result;
        this.endTime = LocalDateTime.now();
        this.durationMs = java.time.Duration.between(startTime, endTime).toMillis();
    }

    public void markFailed(String errorMessage) {
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.endTime = LocalDateTime.now();
        this.durationMs = java.time.Duration.between(startTime, endTime).toMillis();
    }
}