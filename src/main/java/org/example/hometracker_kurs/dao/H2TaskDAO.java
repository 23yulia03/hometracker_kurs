package org.example.hometracker_kurs.dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.hometracker_kurs.model.Config;
import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.model.TaskStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class H2TaskDAO implements TaskDAO {
    private static final Logger logger = Logger.getLogger(H2TaskDAO.class.getName());
    private final Connection connection;

    public H2TaskDAO(Config config) throws SQLException {
        this.connection = DriverManager.getConnection(
                config.getH2Url(),
                config.getH2User(),
                config.getH2Password()
        );
        createTable();
    }

    private void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS tasks (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                description TEXT,
                due_date DATE,
                priority INTEGER,
                assigned_to VARCHAR(50),
                status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED', 'POSTPONED', 'CANCELLED', 'OVERDUE')),
                last_completed DATE,
                frequency_days INTEGER
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    @Override
    public ObservableList<Task> getAllTasks() throws SQLException {
        ObservableList<Task> result = FXCollections.observableArrayList();
        String sql = "SELECT * FROM tasks";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(extractTaskFromResultSet(rs));
            }
        }
        return result;
    }

    @Override
    public ObservableList<Task> getFilteredTasks(
            String type,
            String status,
            String keyword,
            String sortField,
            boolean ascending) throws SQLException {

        ObservableList<Task> result = FXCollections.observableArrayList();
        StringBuilder sql = new StringBuilder("SELECT * FROM tasks WHERE 1=1");

        if (type != null && !type.isEmpty()) {
            sql.append(" AND assigned_to = ?");
        }

        if (status != null && !status.equals("Все")) {
            switch (status) {
                case "Активные" -> sql.append(" AND status = 'ACTIVE'");
                case "Выполненные" -> sql.append(" AND status = 'COMPLETED'");
                case "Просроченные" -> sql.append(" AND status = 'OVERDUE'");
                default -> {}
            }
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(name) LIKE ? OR LOWER(description) LIKE ?)");
        }

        if (sortField != null && !sortField.isEmpty()) {
            sql.append(" ORDER BY ").append(sortField);
            sql.append(ascending ? " ASC" : " DESC");
        } else {
            sql.append(" ORDER BY due_date, priority DESC");
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            int index = 1;

            if (type != null && !type.isEmpty()) {
                stmt.setString(index++, type);
            }

            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.toLowerCase() + "%";
                stmt.setString(index++, kw);
                stmt.setString(index++, kw);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(extractTaskFromResultSet(rs));
                }
            }
        }

        return result;
    }

    private Task extractTaskFromResultSet(ResultSet rs) throws SQLException {
        Task task = new Task(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getDate("due_date") != null ? rs.getDate("due_date").toLocalDate() : null,
                rs.getInt("priority"),
                rs.getString("assigned_to"),
                TaskStatus.valueOf(rs.getString("status")),
                rs.getDate("last_completed") != null ? rs.getDate("last_completed").toLocalDate() : null,
                rs.getInt("frequency_days")
        );

        // Автоматическая установка статуса "OVERDUE" при необходимости
        if (task.getDueDate() != null &&
                task.getDueDate().isBefore(LocalDate.now()) &&
                task.getStatus() == TaskStatus.ACTIVE) {

            task.setStatus(TaskStatus.OVERDUE);
            forceUpdateStatus(task.getId(), TaskStatus.OVERDUE);
        }

        return task;
    }

    private void forceUpdateStatus(int id, TaskStatus status) throws SQLException {
        String sql = "UPDATE tasks SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public Task getTaskById(int id) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractTaskFromResultSet(rs);
                }
                throw new SQLException("Task not found with id: " + id);
            }
        }
    }

    @Override
    public void addTask(Task task) throws SQLException {
        validateTask(task);
        String sql = "INSERT INTO tasks VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, task.getId());
            stmt.setString(2, task.getName());
            stmt.setString(3, task.getDescription());
            stmt.setDate(4, task.getDueDate() != null ? Date.valueOf(task.getDueDate()) : null);
            stmt.setInt(5, task.getPriority());
            stmt.setString(6, task.getAssignedTo());
            stmt.setString(7, task.getStatus().name());
            stmt.setDate(8, task.getLastCompleted() != null ? Date.valueOf(task.getLastCompleted()) : null);
            stmt.setInt(9, task.getFrequencyDays());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error adding task", e);
            throw e;
        }
    }

    @Override
    public void updateTask(Task task) throws SQLException {
        validateTask(task);
        String sql = "UPDATE tasks SET name = ?, description = ?, due_date = ?, priority = ?, " +
                "assigned_to = ?, status = ?, last_completed = ?, frequency_days = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, task.getName());
            stmt.setString(2, task.getDescription());
            stmt.setDate(3, task.getDueDate() != null ? Date.valueOf(task.getDueDate()) : null);
            stmt.setInt(4, task.getPriority());
            stmt.setString(5, task.getAssignedTo());
            stmt.setString(6, task.getStatus().name());
            stmt.setDate(7, task.getLastCompleted() != null ? Date.valueOf(task.getLastCompleted()) : null);
            stmt.setInt(8, task.getFrequencyDays());
            stmt.setInt(9, task.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating task", e);
            throw e;
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
        if (task.getPriority() < 1 || task.getPriority() > 5) {
            throw new SQLException("Приоритет должен быть между 1 и 5");
        }
    }

    @Override
    public void deleteTask(int id) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Task not found with id: " + id);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting task", e);
            throw e;
        }
    }

    @Override
    public void updateTaskStatus(int id, TaskStatus status) throws SQLException {
        if (status == null) {
            throw new SQLException("Status cannot be null");
        }

        Task task = getTaskById(id);
        if (!TaskStatus.isTransitionAllowed(task.getStatus(), status)) {
            throw new SQLException(String.format("Invalid status transition: %s -> %s",
                    task.getStatus().getDisplayName(), status.getDisplayName()));
        }

        String sql = "UPDATE tasks SET status = ?, last_completed = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setDate(2, status == TaskStatus.COMPLETED ? Date.valueOf(LocalDate.now()) : null);
            stmt.setInt(3, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public void markTaskAsCompleted(int id) throws SQLException {
        updateTaskStatus(id, TaskStatus.COMPLETED);
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
