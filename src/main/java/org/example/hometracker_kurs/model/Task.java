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
    private int frequencyDays;

    public enum TaskStatus {
        ACTIVE("Активная"),
        COMPLETED("Выполнена"),
        POSTPONED("Отложена"),
        CANCELLED("Отменена");

        private final String displayName;

        TaskStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static boolean isTransitionAllowed(TaskStatus current, TaskStatus newStatus) {
            if (current == newStatus) return false;

            switch (newStatus) {
                case COMPLETED:
                    return current != CANCELLED;
                case CANCELLED:
                    return current != COMPLETED;
                default:
                    return true;
            }
        }
    }

    public Task(int id, String name, String description, LocalDate dueDate,
                int priority, String assignedTo, TaskStatus status,
                LocalDate lastCompleted, int frequencyDays) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.assignedTo = assignedTo;
        this.status = status;
        this.lastCompleted = lastCompleted;
        this.frequencyDays = frequencyDays;
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
    public int getFrequencyDays() { return frequencyDays; }
    public void setFrequencyDays(int frequencyDays) { this.frequencyDays = frequencyDays; }

    public void setStatus(TaskStatus newStatus) {
        // Разрешаем установку того же статуса (убираем проверку на равенство)
        if (this.status == newStatus) {
            return; // Просто выходим, если статус не изменился
        }

        // Проверяем допустимость перехода между разными статусами
        if (!TaskStatus.isTransitionAllowed(this.status, newStatus)) {
            throw new IllegalArgumentException(
                    String.format("Недопустимый переход статуса: %s -> %s",
                            this.status.getDisplayName(), newStatus.getDisplayName())
            );
        }
        this.status = newStatus;
    }

    public boolean isOverdue() {
        return status == TaskStatus.ACTIVE &&
                dueDate != null &&
                dueDate.isBefore(LocalDate.now());
    }

    public boolean needsCompletionReminder() {
        return status == TaskStatus.ACTIVE &&
                dueDate != null &&
                dueDate.isBefore(LocalDate.now().plusDays(3));
    }

    public void markCompleted() {
        setStatus(TaskStatus.COMPLETED);
        this.lastCompleted = LocalDate.now();
    }

    public void postpone(int days) {
        if (dueDate != null) {
            this.dueDate = dueDate.plusDays(days);
        }
        // Устанавливаем статус только если он действительно меняется
        if (this.status != TaskStatus.POSTPONED) {
            setStatus(TaskStatus.POSTPONED);
        }
    }

    public void cancel() {
        setStatus(TaskStatus.CANCELLED);
    }

    public void reactivate() {
        setStatus(TaskStatus.ACTIVE);
    }

    @Override
    public String toString() {
        return String.format("%s [%s, %s]", name, status.getDisplayName(),
                dueDate != null ? dueDate.toString() : "нет срока");
    }

    public Task copy() {
        return new Task(
                this.id,
                this.name,
                this.description,
                this.dueDate,
                this.priority,
                this.assignedTo,
                this.status,
                this.lastCompleted,
                this.frequencyDays
        );
    }
}