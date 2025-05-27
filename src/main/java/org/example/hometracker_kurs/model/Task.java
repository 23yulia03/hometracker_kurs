package org.example.hometracker_kurs.model;

import java.time.LocalDate;

public class Task {
    private int id;
    private String name;
    private String description;
    private LocalDate dueDate;
    private int priority;
    private String assignedTo;
    private TaskStatus status;
    private String type;
    private LocalDate lastCompleted;

    public Task(int id, String name, String description, LocalDate dueDate,
                int priority, String assignedTo, TaskStatus status,
                LocalDate lastCompleted) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.assignedTo = assignedTo;
        this.status = status;
        this.lastCompleted = lastCompleted;
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public TaskStatus getStatus() { return status; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDate getLastCompleted() { return lastCompleted; }
    public void setLastCompleted(LocalDate lastCompleted) { this.lastCompleted = lastCompleted; }

    public void setStatus(TaskStatus newStatus) {
        if (this.status == newStatus) {
            return;
        }

        if (!TaskStatus.isTransitionAllowed(this.status, newStatus)) {
            throw new IllegalArgumentException(
                    String.format("Недопустимый переход статуса: %s -> %s",
                            this.status.getDisplayName(), newStatus.getDisplayName())
            );
        }
        this.status = newStatus;
    }

    public void postpone(int days) {
        if (dueDate != null) {
            this.dueDate = dueDate.plusDays(days);
        }
        if (this.status != TaskStatus.POSTPONED) {
            setStatus(TaskStatus.POSTPONED);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Task task = (Task) obj;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("%s [%s, %s]", name, status.getDisplayName(),
                dueDate != null ? dueDate.toString() : "нет срока");
    }
}