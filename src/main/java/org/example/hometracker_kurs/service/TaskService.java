package org.example.hometracker_kurs.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.hometracker_kurs.config.DatabaseConfig;
import org.example.hometracker_kurs.config.ExcelConfig;
import org.example.hometracker_kurs.dao.TaskDAO;
import org.example.hometracker_kurs.dao.TaskDAOFactory;
import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.model.TaskStatus;
import org.example.hometracker_kurs.sync.PendingTaskQueue;
import org.example.hometracker_kurs.sync.QueuedTaskOperation;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

public class TaskService {
    private final TaskDAO taskDAO;
    private ScheduledExecutorService statusCheckScheduler;

    public TaskService(String daoType, DatabaseConfig dbConfig, ExcelConfig excelConfig) {
        try {
            this.taskDAO = TaskDAOFactory.createTaskDAO(daoType, dbConfig, excelConfig);
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
        try {
            validateTask(task);
            taskDAO.addTask(task);
        } catch (SQLException e) {
            if (isNetworkIssue(e)) {
                PendingTaskQueue.enqueue(task, "add");
                System.err.println("⛔ Соединение потеряно, задача добавлена в очередь: " + task.getName());
            } else throw e;
        }
    }

    public void updateTask(Task task) throws SQLException {
        try {
            validateTask(task);
            taskDAO.updateTask(task);
        } catch (SQLException e) {
            if (isNetworkIssue(e)) {
                PendingTaskQueue.enqueue(task, "update");
                System.err.println("⛔ Соединение потеряно, обновление сохранено локально: " + task.getName());
            } else throw e;
        }
    }

    public void deleteTask(Task task) throws SQLException {
        try {
            if (task != null) taskDAO.deleteTask(task.getId());
        } catch (SQLException e) {
            if (isNetworkIssue(e)) {
                PendingTaskQueue.enqueue(task, "delete");
                System.err.println("⛔ Соединение потеряно, удаление сохранено локально: " + task.getName());
            } else throw e;
        }
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

    private boolean isNetworkIssue(SQLException e) {
        String msg = e.getMessage().toLowerCase();
        return msg.contains("connection") || msg.contains("refused") || msg.contains("timeout");
    }

    public void trySyncPendingTasks() {
        List<QueuedTaskOperation> queue = PendingTaskQueue.loadQueue();
        if (queue.isEmpty()) {
            System.out.println("✅ Нет отложенных операций для синхронизации");
            return;
        }

        int success = 0;
        for (QueuedTaskOperation op : queue) {
            try {
                Task task = op.getTask();
                switch (op.getOperation()) {
                    case "add" -> taskDAO.addTask(task);
                    case "update" -> taskDAO.updateTask(task);
                    case "delete" -> taskDAO.deleteTask(task.getId());
                }
                success++;
            } catch (SQLException e) {
                System.err.println("❌ Ошибка при синхронизации: " + e.getMessage());
                return; // Прерываем — оставим в очереди
            }
        }

        PendingTaskQueue.clearQueue();
        System.out.println("✅ Синхронизировано операций: " + success);
    }
}
