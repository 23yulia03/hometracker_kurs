package org.example.hometracker_kurs.sync;

import org.example.hometracker_kurs.model.Task;

public class QueuedTaskOperation {
    private String operation; // "add", "update", "delete"
    private Task task;

    public QueuedTaskOperation() {} // для Jackson

    public QueuedTaskOperation(String operation, Task task) {
        this.operation = operation;
        this.task = task;
    }

    public String getOperation() {
        return operation;
    }

    public Task getTask() {
        return task;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public void setTask(Task task) {
        this.task = task;
    }
}
