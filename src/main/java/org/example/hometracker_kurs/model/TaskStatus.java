package org.example.hometracker_kurs.model;

public enum TaskStatus {
    ACTIVE("Активная"),
    COMPLETED("Выполнена"),
    POSTPONED("Отложена"),
    CANCELLED("Отменена"),
    OVERDUE("Просрочена");  // Добавили статус OVERDUE

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static boolean isTransitionAllowed(TaskStatus current, TaskStatus newStatus) {
        if (current == newStatus) return false;

        return switch (newStatus) {
            case COMPLETED -> current != CANCELLED;
            case CANCELLED -> current != COMPLETED;
            case ACTIVE -> true; // <- разрешаем возврат в ACTIVE
            default -> true;
        };
    }
}
