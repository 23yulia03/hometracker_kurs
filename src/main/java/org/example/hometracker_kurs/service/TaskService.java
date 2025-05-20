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
            boolean needsRefresh = false;

            for (Task task : tasks) {
                if (task.getStatus() == TaskStatus.ACTIVE &&
                        task.getDueDate() != null &&
                        task.getDueDate().isBefore(LocalDate.now())) {

                    if (task.getStatus() != TaskStatus.OVERDUE) {
                        task.setStatus(TaskStatus.OVERDUE);
                        taskDAO.updateTask(task);
                        needsRefresh = true;
                    }
                }
            }

            if (needsRefresh) {
                System.out.println("Обновлены статусы просроченных задач");
            }
        } catch (SQLException e) {
            System.err.println("Ошибка проверки просроченных задач: " + e.getMessage());
        }
    }

    public ObservableList<Task> getAllTasks() throws SQLException {
        return FXCollections.observableArrayList(taskDAO.getAllTasks());
    }

    public ObservableList<Task> getFilteredTasks(
            String type,
            String status,
            String searchText,
            String sortField,
            boolean ascending) throws SQLException {

        return taskDAO.getFilteredTasks(type, status, searchText, sortField, ascending);
    }

    public void addTask(Task task) throws SQLException {
        validateTask(task);
        taskDAO.addTask(task);
    }

    public void updateTask(Task task) throws SQLException {
        validateTask(task);
        taskDAO.updateTask(task);
    }

    private void validateTask(Task task) throws SQLException {
        if (task == null) {
            throw new SQLException("Задача не может быть null");
        }
        if (task.getName() == null || task.getName().trim().isEmpty()) {
            throw new SQLException("Название задачи не может быть пустым");
        }
        if (task.getStatus() == null) {
            throw new SQLException("Статус задачи не может быть null");
        }
        if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())) {
            throw new SQLException("Дата выполнения не может быть в прошлом");
        }
        if (task.getPriority() < 1 || task.getPriority() > 5) {
            throw new SQLException("Приоритет должен быть между 1 и 5");
        }
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

    public void reactivateTask(int id) throws SQLException {
        taskDAO.updateTaskStatus(id, TaskStatus.ACTIVE);
    }

    public void close() throws SQLException {
        if (taskDAO != null) {
            taskDAO.close();
        }
        if (statusCheckScheduler != null) {
            statusCheckScheduler.shutdown();
        }
    }
}