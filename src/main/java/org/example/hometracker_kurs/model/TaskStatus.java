package org.example.hometracker_kurs.model;

/**
 * Перечисление статусов задачи с русскоязычными названиями
 */
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

    /**
     * Проверяет допустимость перехода между статусами
     */
    public static boolean isTransitionAllowed(TaskStatus current, TaskStatus newStatus) {
        // Разрешаем оставить тот же статус
        if (current == newStatus) {
            return true;
        }

        // Логика разрешенных переходов между разными статусами
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