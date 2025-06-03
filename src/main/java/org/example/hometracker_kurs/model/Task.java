package org.example.hometracker_kurs.model;

import java.time.LocalDate;

/**
 * Класс, представляющий задачу в системе управления задачами.
 * Содержит информацию о задаче, включая статус, приоритет, срок выполнения и другие атрибуты.
 */
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

    /**
     * Конструктор для создания новой задачи.
     *
     * @param id Уникальный идентификатор задачи
     * @param name Название задачи
     * @param description Подробное описание задачи
     * @param dueDate Срок выполнения задачи (может быть null)
     * @param priority Приоритет задачи (числовое значение)
     * @param assignedTo Ответственный за выполнение задачи
     * @param status Текущий статус задачи
     * @param lastCompleted Дата последнего выполнения задачи (может быть null)
     */
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

    /**
     * Возвращает уникальный идентификатор задачи.
     * @return идентификатор задачи
     */
    public int getId() { return id; }

    /**
     * Устанавливает уникальный идентификатор задачи.
     * @param id новый идентификатор задачи
     */
    public void setId(int id) { this.id = id; }

    /**
     * Возвращает название задачи.
     * @return название задачи
     */
    public String getName() { return name; }

    /**
     * Устанавливает название задачи.
     * @param name новое название задачи
     */
    public void setName(String name) { this.name = name; }

    /**
     * Возвращает описание задачи.
     * @return описание задачи
     */
    public String getDescription() { return description; }

    /**
     * Устанавливает описание задачи.
     * @param description новое описание задачи
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Возвращает срок выполнения задачи.
     * @return срок выполнения или null, если не установлен
     */
    public LocalDate getDueDate() { return dueDate; }

    /**
     * Устанавливает срок выполнения задачи.
     * @param dueDate новый срок выполнения (может быть null)
     */
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    /**
     * Возвращает приоритет задачи.
     * @return числовое значение приоритета
     */
    public int getPriority() { return priority; }

    /**
     * Устанавливает приоритет задачи.
     * @param priority новое значение приоритета
     */
    public void setPriority(int priority) { this.priority = priority; }

    /**
     * Возвращает ответственного за задачу.
     * @return имя ответственного
     */
    public String getAssignedTo() { return assignedTo; }

    /**
     * Устанавливает ответственного за задачу.
     * @param assignedTo новый ответственный
     */
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    /**
     * Возвращает текущий статус задачи.
     * @return статус задачи
     */
    public TaskStatus getStatus() { return status; }

    /**
     * Возвращает тип задачи.
     * @return тип задачи
     */
    public String getType() { return type; }

    /**
     * Устанавливает тип задачи.
     * @param type новый тип задачи
     */
    public void setType(String type) { this.type = type; }

    /**
     * Возвращает дату последнего выполнения задачи.
     * @return дата последнего выполнения или null
     */
    public LocalDate getLastCompleted() { return lastCompleted; }

    /**
     * Устанавливает дату последнего выполнения задачи.
     * @param lastCompleted новая дата выполнения (может быть null)
     */
    public void setLastCompleted(LocalDate lastCompleted) { this.lastCompleted = lastCompleted; }

    /**
     * Изменяет статус задачи с проверкой допустимости перехода.
     *
     * @param newStatus новый статус задачи
     * @throws IllegalArgumentException если переход между статусами недопустим
     */
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

    /**
     * Откладывает выполнение задачи на указанное количество дней.
     * Автоматически устанавливает статус "Отложено".
     *
     * @param days количество дней для переноса срока
     */
    public void postpone(int days) {
        if (dueDate != null) {
            this.dueDate = dueDate.plusDays(days);
        }
        if (this.status != TaskStatus.POSTPONED) {
            setStatus(TaskStatus.POSTPONED);
        }
    }

    /**
     * Сравнивает задачи по идентификатору.
     * @param obj объект для сравнения
     * @return true, если задачи имеют одинаковый ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Task task = (Task) obj;
        return id == task.id;
    }

    /**
     * Возвращает хэш-код задачи на основе ID.
     * @return хэш-код задачи
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    /**
     * Возвращает строковое представление задачи.
     * @return строка в формате "Название [Статус, Срок]"
     */
    @Override
    public String toString() {
        return String.format("%s [%s, %s]", name, status.getDisplayName(),
                dueDate != null ? dueDate.toString() : "нет срока");
    }
}