package org.example.hometracker_kurs.service;

import javafx.collections.ObservableList;
import org.example.hometracker_kurs.dao.TaskDAO;
import org.example.hometracker_kurs.dao.TaskDAOFactory;
import org.example.hometracker_kurs.model.Config;
import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.model.Task.TaskStatus;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class TaskService {
    private final TaskDAO taskDAO;
    private ScheduledExecutorService statusCheckScheduler;

    public TaskService(String daoType) {
        Config config = new Config();
        try {
            this.taskDAO = TaskDAOFactory.createTaskDAO(daoType, config);
            startStatusChecker();
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось создать DAO: " + e.getMessage(), e);
        }
    }

    private void startStatusChecker() {
        statusCheckScheduler = Executors.newSingleThreadScheduledExecutor();
        statusCheckScheduler.scheduleAtFixedRate(this::checkOverdueTasks,
                0, 24, TimeUnit.HOURS);
    }

    public void checkOverdueTasks() {
        try {
            ObservableList<Task> tasks = taskDAO.getAllTasks();
            tasks.stream()
                    .filter(Task::isOverdue)
                    .forEach(task -> {
                        System.out.println("Просрочена задача: " + task);
                    });
        } catch (SQLException e) {
            System.err.println("Ошибка проверки просроченных задач: " + e.getMessage());
        }
    }

    public ObservableList<Task> getAllTasks() throws SQLException {
        return taskDAO.getAllTasks();
    }

    public ObservableList<Task> getFilteredTasks(String type, String status, String searchText) throws SQLException {
        ObservableList<Task> allTasks = taskDAO.getAllTasks();
        Predicate<Task> filter = t -> true;

        if (type != null) {
            filter = filter.and(t -> type.equals(t.getType()));
        }

        if (status != null) {
            switch (status) {
                case "Активные" -> filter = filter.and(t -> t.getStatus() == TaskStatus.ACTIVE);
                case "Выполненные" -> filter = filter.and(t -> t.getStatus() == TaskStatus.COMPLETED);
                case "Просроченные" -> filter = filter.and(t -> t.isOverdue());
            }
        }

        if (searchText != null && !searchText.isBlank()) {
            filter = filter.and(t -> t.getName().toLowerCase().contains(searchText.toLowerCase()));
        }

        return allTasks.filtered(filter::test);
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
        if (statusCheckScheduler != null) {
            statusCheckScheduler.shutdown();
        }
        try {
            taskDAO.close();
        } catch (SQLException e) {
            System.err.println("Ошибка закрытия DAO: " + e.getMessage());
        }
    }
}