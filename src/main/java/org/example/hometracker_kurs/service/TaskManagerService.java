package org.example.hometracker_kurs.service;

import javafx.collections.ObservableList;
import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.model.TaskStatus;

import java.sql.SQLException;
import java.time.LocalDate;

public class TaskManagerService {
    private final TaskService taskService;

    public TaskManagerService(TaskService taskService) {
        this.taskService = taskService;
    }

    public ObservableList<Task> refreshData() throws SQLException {
        ObservableList<Task> tasks = taskService.getAllTasks();
        updateOverdueTasks();
        return tasks;
    }

    public void addTask(Task task) throws SQLException {
        taskService.addTask(task);
    }

    public void updateTask(Task task) throws SQLException {
        taskService.updateTask(task);
    }

    public void deleteTask(Task task) throws SQLException {
        if (task != null) {
            taskService.deleteTask(task);
        }
    }

    public void completeTask(Task task) throws SQLException {
        if (task != null) {
            taskService.completeTask(task.getId());
        }
    }

    public void postponeTask(Task task, int days) throws SQLException {
        if (task != null) {
            taskService.postponeTask(task.getId(), days);
        }
    }

    public void reactivateTask(Task task) throws SQLException {
        if (task != null) {
            taskService.reactivateTask(task.getId());
        }
    }

    public ObservableList<Task> applyFilters(
            String type,
            String status,
            String keyword,
            String sortField,
            boolean ascending) throws SQLException {

        return taskService.getFilteredTasks(
                type, status, keyword, sortField, ascending);
    }

    private void updateOverdueTasks() {
        try {
            ObservableList<Task> tasks = taskService.getAllTasks();
            boolean updated = false;

            for (Task task : tasks) {
                if (task.getStatus() == TaskStatus.ACTIVE &&
                        task.getDueDate() != null &&
                        task.getDueDate().isBefore(LocalDate.now()) &&
                        task.getStatus() != TaskStatus.OVERDUE) {

                    task.setStatus(TaskStatus.OVERDUE);
                    taskService.updateTask(task);
                    updated = true;
                }
            }

            if (updated) {
                System.out.println("Обновленны просроченные задачи");
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при обновлении просроченных задач: " + e.getMessage());
        }
    }
}