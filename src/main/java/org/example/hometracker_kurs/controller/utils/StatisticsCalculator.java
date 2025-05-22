package org.example.hometracker_kurs.controller.utils;

import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.model.TaskStatus;

public class StatisticsCalculator {
    private final Label totalTasksLabel;
    private final Label activeTasksLabel;
    private final Label completedTasksLabel;
    private final Label overdueTasksLabel;

    public StatisticsCalculator(Label totalTasksLabel, Label activeTasksLabel,
                                Label completedTasksLabel, Label overdueTasksLabel) {
        this.totalTasksLabel = totalTasksLabel;
        this.activeTasksLabel = activeTasksLabel;
        this.completedTasksLabel = completedTasksLabel;
        this.overdueTasksLabel = overdueTasksLabel;
    }

    public void updateStatistics(ObservableList<Task> tasks) {
        if (tasks == null) return;

        int total = tasks.size();
        int active = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.ACTIVE).count();
        int completed = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        int overdue = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.OVERDUE).count();

        totalTasksLabel.setText(String.valueOf(total));
        activeTasksLabel.setText(String.valueOf(active));
        completedTasksLabel.setText(String.valueOf(completed));
        overdueTasksLabel.setText(String.valueOf(overdue));
    }
}