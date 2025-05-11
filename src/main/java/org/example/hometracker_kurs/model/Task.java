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
    private String type;  // Поле для типа задачи
    private LocalDate lastCompleted;
    private int frequencyDays;

    public enum TaskStatus {
        ACTIVE, COMPLETED, POSTPONED, CANCELLED
    }

    // Обновленный конструктор с параметром type
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
        this.type = type;  // Инициализация поля type
        this.lastCompleted = lastCompleted;
        this.frequencyDays = frequencyDays;
    }

    // Геттеры и сеттеры
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDate getLastCompleted() {
        return lastCompleted;
    }

    public void setLastCompleted(LocalDate lastCompleted) {
        this.lastCompleted = lastCompleted;
    }

    public int getFrequencyDays() {
        return frequencyDays;
    }

    public void setFrequencyDays(int frequencyDays) {
        this.frequencyDays = frequencyDays;
    }

    // Дополнительные методы
    public boolean isOverdue() {
        return dueDate != null && dueDate.isBefore(LocalDate.now())
                && status != TaskStatus.COMPLETED;
    }

    public boolean needsCompletionReminder() {
        return status == TaskStatus.ACTIVE
                && dueDate != null
                && dueDate.isBefore(LocalDate.now().plusDays(3));
    }

    public void markCompleted() {
        this.status = TaskStatus.COMPLETED;
        this.lastCompleted = LocalDate.now();
    }

    public void postpone(int days) {
        if (dueDate != null) {
            this.dueDate = dueDate.plusDays(days);
        }
        this.status = TaskStatus.POSTPONED;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +  // Добавлено type в вывод
                ", dueDate=" + dueDate +
                ", assignedTo='" + assignedTo + '\'' +
                ", status=" + status +
                '}';
    }

    // Метод для создания копии задачи
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