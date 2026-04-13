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
public class ScheduledTask {

    private String id;

    private String name;

    private String description;

    private String cronExpression;

    private TaskType taskType;

    private String userId;

    private String channelType;

    private String triggerMessage;

    private TaskStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime lastRunAt;

    private LocalDateTime nextRunAt;

    private int executionCount;

    public enum TaskType {
        ONE_TIME,
        RECURRING,
        CRON
    }

    public enum TaskStatus {
        ACTIVE,
        PAUSED,
        COMPLETED,
        FAILED
    }

    public void incrementExecutionCount() {
        this.executionCount++;
    }

    public void updateLastRun() {
        this.lastRunAt = LocalDateTime.now();
    }
}