package org.example.hometracker_kurs.model;

/**
 * Перечисление, представляющее возможные статусы задач.
 * Определяет допустимые переходы между статусами и их отображаемые названия.
 */
public enum TaskStatus {
    /**
     * Задача активна и находится в работе
     */
    ACTIVE("Активная"),

    /**
     * Задача успешно выполнена
     */
    COMPLETED("Выполнена"),

    /**
     * Выполнение задачи отложено
     */
    POSTPONED("Отложена"),

    /**
     * Задача была отменена
     */
    CANCELLED("Отменена"),

    /**
     * Задача просрочена (не выполнена в срок)
     */
    OVERDUE("Просрочена");

    private final String displayName;

    /**
     * Конструктор статуса задачи.
     * @param displayName Отображаемое название статуса
     */
    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Возвращает удобочитаемое название статуса.
     * @return Отображаемое название статуса
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Проверяет, допустим ли переход между статусами.
     *
     * @param current Текущий статус задачи
     * @param newStatus Новый статус, на который требуется изменить
     * @return true, если переход допустим, false в противном случае
     * @throws IllegalArgumentException если проверяемый переход не предусмотрен логикой
     */
    public static boolean isTransitionAllowed(TaskStatus current, TaskStatus newStatus) {
        if (current == newStatus) return false;

        switch (newStatus) {
            case COMPLETED:
                return current != TaskStatus.CANCELLED;
            case CANCELLED:
                return current != TaskStatus.COMPLETED;
            case ACTIVE:
                return true; // разрешаем возврат в ACTIVE
            default:
                return true;
        }
    }
}