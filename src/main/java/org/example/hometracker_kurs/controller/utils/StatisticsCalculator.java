package org.example.hometracker_kurs.controller.utils;

import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.model.TaskStatus;

/**
 * Класс {@code StatisticsCalculator} отвечает за вычисление и отображение статистики задач.
 * Обновляет значения на экране: общее количество задач, активные, выполненные и просроченные.
 */
public class StatisticsCalculator {

    private final Label totalTasksLabel;
    private final Label activeTasksLabel;
    private final Label completedTasksLabel;
    private final Label overdueTasksLabel;

    /**
     * Конструктор {@code StatisticsCalculator}.
     *
     * @param totalTasksLabel     метка для отображения общего количества задач
     * @param activeTasksLabel    метка для отображения количества активных задач
     * @param completedTasksLabel метка для отображения количества выполненных задач
     * @param overdueTasksLabel   метка для отображения количества просроченных задач
     */
    public StatisticsCalculator(Label totalTasksLabel, Label activeTasksLabel,
                                Label completedTasksLabel, Label overdueTasksLabel) {
        this.totalTasksLabel = totalTasksLabel;
        this.activeTasksLabel = activeTasksLabel;
        this.completedTasksLabel = completedTasksLabel;
        this.overdueTasksLabel = overdueTasksLabel;
    }

    /**
     * Обновляет значения меток статистики на основе переданного списка задач.
     *
     * @param tasks список задач
     */
    public void updateStatistics(ObservableList<Task> tasks) {
        int total = tasks == null ? 0 : tasks.size();
        int active = countByStatus(tasks, TaskStatus.ACTIVE);
        int completed = countByStatus(tasks, TaskStatus.COMPLETED);
        int overdue = countByStatus(tasks, TaskStatus.OVERDUE);

        totalTasksLabel.setText(String.valueOf(total));
        activeTasksLabel.setText(String.valueOf(active));
        completedTasksLabel.setText(String.valueOf(completed));
        overdueTasksLabel.setText(String.valueOf(overdue));
    }

    /**
     * Подсчитывает количество задач заданного статуса.
     *
     * @param tasks  список задач
     * @param status интересующий статус
     * @return количество задач с заданным статусом
     */
    private int countByStatus(ObservableList<Task> tasks, TaskStatus status) {
        if (tasks == null) return 0;
        return (int) tasks.stream()
                .filter(t -> t.getStatus() == status)
                .count();
    }
}
