package org.example.hometracker_kurs.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.hometracker_kurs.dao.TaskDAO;
import org.example.hometracker_kurs.dao.TaskDAOFactory;
import org.example.hometracker_kurs.model.Config;
import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.model.TaskStatus;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.concurrent.*;

public class TaskService {
    private final TaskDAO taskDAO;
    private ScheduledExecutorService statusCheckScheduler;

    public TaskService(String daoType) {
        try {
            this.taskDAO = TaskDAOFactory.createTaskDAO(daoType, new Config());
            startStatusChecker();
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось создать DAO: " + e.getMessage(), e);
        }
    }

    private void startStatusChecker() {
        statusCheckScheduler = Executors.newSingleThreadScheduledExecutor();
        statusCheckScheduler.scheduleAtFixedRate(this::checkOverdueTasks, 0, 24, TimeUnit.HOURS);
    }

    public void checkOverdueTasks() {
        try {
            ObservableList<Task> tasks = taskDAO.getAllTasks();
            for (Task task : tasks) {
                if (task.getStatus() == TaskStatus.ACTIVE &&
                        task.getDueDate() != null &&
                        task.getDueDate().isBefore(LocalDate.now())) {
                    task.setStatus(TaskStatus.OVERDUE);
                    taskDAO.updateTask(task);
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка проверки просроченных задач: " + e.getMessage());
        }
    }

    public ObservableList<Task> getAllTasks() throws SQLException {
        return FXCollections.observableArrayList(taskDAO.getAllTasks());
    }

    public ObservableList<Task> getFilteredTasks(String type, String status, String searchText) throws SQLException {
        ObservableList<Task> allTasks = taskDAO.getAllTasks();
        return allTasks.filtered(task -> {
            boolean matches = true;
            if (type != null) matches &= type.equals(task.getType());
            if (status != null) {
                matches &= switch (status) {
                    case "Активные" -> task.getStatus() == TaskStatus.ACTIVE;
                    case "Выполненные" -> task.getStatus() == TaskStatus.COMPLETED;
                    case "Просроченные" -> task.getStatus() == TaskStatus.OVERDUE;
                    default -> true;
                };
            }
            if (searchText != null && !searchText.isBlank())
                matches &= task.getName().toLowerCase().contains(searchText.toLowerCase());

            return matches;
        });
    }

    public void addTask(Task task) throws SQLException {
        taskDAO.addTask(task);
    }

    public void updateTask(Task task) throws SQLException {
        taskDAO.updateTask(task);
    }

    public void deleteTask(int id) throws SQLException {
        taskDAO.deleteTask(id);
    }

    public void completeTask(int id) throws SQLException {
        taskDAO.markTaskAsCompleted(id);
    }

    public void postponeTask(int id, int days) throws SQLException {
        Task task = taskDAO.getTaskById(id);
        task.postpone(days);
        taskDAO.updateTask(task);
    }

    public void cancelTask(int id) throws SQLException {
        taskDAO.updateTaskStatus(id, TaskStatus.CANCELLED);
    }

    public void reactivateTask(int id) throws SQLException {
        taskDAO.updateTaskStatus(id, TaskStatus.ACTIVE);
    }

    public void shutdown() {
        if (statusCheckScheduler != null) statusCheckScheduler.shutdown();
        try {
            taskDAO.close();
        } catch (SQLException e) {
            System.err.println("Ошибка закрытия DAO: " + e.getMessage());
        }
    }
}
