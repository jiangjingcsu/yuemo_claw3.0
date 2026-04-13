package com.yuemo.demo.workflow.event;

import com.yuemo.demo.common.event.BaseEvent;
import com.yuemo.demo.workflow.definition.WorkflowDefinition;
import com.yuemo.demo.workflow.definition.WorkflowStep;
import lombok.Getter;

@Getter
public class WorkflowEvents {

    public static final String WORKFLOW_STARTED = "workflow.started";
    public static final String WORKFLOW_STEP_STARTED = "workflow.step.started";
    public static final String WORKFLOW_STEP_COMPLETED = "workflow.step.completed";
    public static final String WORKFLOW_COMPLETED = "workflow.completed";
    public static final String WORKFLOW_FAILED = "workflow.failed";

    private WorkflowEvents() {
    }

    @Getter
    public static class WorkflowStartedEvent extends BaseEvent {
        private final WorkflowDefinition workflow;
        private final String executionId;

        public WorkflowStartedEvent(WorkflowDefinition workflow, String executionId) {
            super(WORKFLOW_STARTED);
            this.workflow = workflow;
            this.executionId = executionId;
        }
    }

    @Getter
    public static class WorkflowStepStartedEvent extends BaseEvent {
        private final String executionId;
        private final WorkflowDefinition workflow;
        private final WorkflowStep step;
        private final int stepIndex;

        public WorkflowStepStartedEvent(String executionId, WorkflowDefinition workflow,
                                       WorkflowStep step, int stepIndex) {
            super(WORKFLOW_STEP_STARTED);
            this.executionId = executionId;
            this.workflow = workflow;
            this.step = step;
            this.stepIndex = stepIndex;
        }
    }

    @Getter
    public static class WorkflowStepCompletedEvent extends BaseEvent {
        private final String executionId;
        private final WorkflowDefinition workflow;
        private final WorkflowStep step;
        private final int stepIndex;
        private final Object result;

        public WorkflowStepCompletedEvent(String executionId, WorkflowDefinition workflow,
                                         WorkflowStep step, int stepIndex, Object result) {
            super(WORKFLOW_STEP_COMPLETED);
            this.executionId = executionId;
            this.workflow = workflow;
            this.step = step;
            this.stepIndex = stepIndex;
            this.result = result;
        }
    }

    @Getter
    public static class WorkflowCompletedEvent extends BaseEvent {
        private final String executionId;
        private final WorkflowDefinition workflow;
        private final Object finalResult;

        public WorkflowCompletedEvent(String executionId, WorkflowDefinition workflow, Object finalResult) {
            super(WORKFLOW_COMPLETED);
            this.executionId = executionId;
            this.workflow = workflow;
            this.finalResult = finalResult;
        }
    }

    @Getter
    public static class WorkflowFailedEvent extends BaseEvent {
        private final String executionId;
        private final WorkflowDefinition workflow;
        private final String errorMessage;

        public WorkflowFailedEvent(String executionId, WorkflowDefinition workflow, String errorMessage) {
            super(WORKFLOW_FAILED);
            this.executionId = executionId;
            this.workflow = workflow;
            this.errorMessage = errorMessage;
        }
    }
}