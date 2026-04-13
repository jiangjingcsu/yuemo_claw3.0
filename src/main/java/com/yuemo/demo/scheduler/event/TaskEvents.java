package com.yuemo.demo.scheduler.event;

import com.yuemo.demo.common.event.BaseEvent;
import com.yuemo.demo.scheduler.entity.ScheduledTask;
import com.yuemo.demo.scheduler.entity.TaskExecutionHistory;
import lombok.Getter;

public class TaskEvents {

    public static final String TASK_TRIGGERED = "task.triggered";
    public static final String TASK_EXECUTED = "task.executed";
    public static final String TASK_FAILED = "task.failed";
    public static final String TASK_COMPLETED = "task.completed";

    private TaskEvents() {
    }

    @Getter
    public static class TaskTriggeredEvent extends BaseEvent {
        private final ScheduledTask task;

        public TaskTriggeredEvent(ScheduledTask task) {
            super(TASK_TRIGGERED);
            this.task = task;
        }
    }

    @Getter
    public static class TaskExecutedEvent extends BaseEvent {
        private final ScheduledTask task;
        private final TaskExecutionHistory execution;

        public TaskExecutedEvent(ScheduledTask task, TaskExecutionHistory execution) {
            super(TASK_EXECUTED);
            this.task = task;
            this.execution = execution;
        }
    }

    @Getter
    public static class TaskFailedEvent extends BaseEvent {
        private final ScheduledTask task;
        private final TaskExecutionHistory execution;
        private final String errorMessage;

        public TaskFailedEvent(ScheduledTask task, TaskExecutionHistory execution, String errorMessage) {
            super(TASK_FAILED);
            this.task = task;
            this.execution = execution;
            this.errorMessage = errorMessage;
        }
    }

    @Getter
    public static class TaskCompletedEvent extends BaseEvent {
        private final ScheduledTask task;
        private final TaskExecutionHistory execution;

        public TaskCompletedEvent(ScheduledTask task, TaskExecutionHistory execution) {
            super(TASK_COMPLETED);
            this.task = task;
            this.execution = execution;
        }
    }
}