package com.taipei.iot.workflow.event;

import com.taipei.iot.workflow.entity.WorkflowInstance;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class WorkflowTransitionEvent extends ApplicationEvent {

    private final WorkflowInstance instance;
    private final String targetStep;
    private final String action;

    public WorkflowTransitionEvent(Object source, WorkflowInstance instance,
                                    String targetStep, String action) {
        super(source);
        this.instance = instance;
        this.targetStep = targetStep;
        this.action = action;
    }
}
