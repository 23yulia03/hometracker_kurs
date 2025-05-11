package org.example.hometracker_kurs.dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.hometracker_kurs.model.Config;
import org.example.hometracker_kurs.model.Task;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class H2TaskDAO implements TaskDAO {
    private final Connection connection;
    private final List<Task> tasks = new ArrayList<>();
    private int nextId = 1;

    public H2TaskDAO(Config config) throws SQLException {
        this.connection = DriverManager.getConnection(config.getH2Url());
        createTable();
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS tasks (" +
                "id INT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "description TEXT, " +
                "due_date DATE, " +
                "priority INT, " +
                "assigned_to VARCHAR(50), " +
                "status VARCHAR(20), " +
                "last_completed DATE, " +
                "frequency_days INT)";

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

    private Task extractTaskFromResultSet(ResultSet rs) throws SQLException {
        return new Task(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getDate("due_date") != null ? rs.getDate("due_date").toLocalDate() : null,
                rs.getInt("priority"),
                rs.getString("assigned_to"),
                Task.TaskStatus.valueOf(rs.getString("status")),
                rs.getDate("last_completed") != null ? rs.getDate("last_completed").toLocalDate() : null,
                rs.getInt("frequency_days")
        );
    }

    @Override
    public Task getTaskById(int id) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? extractTaskFromResultSet(rs) : null;
            }
        }
    }

    @Override
    public void addTask(Task task) throws SQLException {
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
        }
    }

    @Override
    public void updateTask(Task task) throws SQLException {
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
        }
    }

    @Override
    public void deleteTask(int id) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public ObservableList<Task> getTasksByAssignee(String assignee) throws SQLException {
        ObservableList<Task> result = FXCollections.observableArrayList();
        String sql = "SELECT * FROM tasks WHERE assigned_to = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, assignee);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(extractTaskFromResultSet(rs));
                }
            }
        }
        return result;
    }

    @Override
    public ObservableList<Task> getTasksDueBetween(LocalDate start, LocalDate end) throws SQLException {
        ObservableList<Task> result = FXCollections.observableArrayList();
        String sql = "SELECT * FROM tasks WHERE due_date BETWEEN ? AND ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(extractTaskFromResultSet(rs));
                }
            }
        }
        return result;
    }

    @Override
    public void updateTaskStatus(int id, Task.TaskStatus status) throws SQLException {
        String sql = "UPDATE tasks SET status = ?, last_completed = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setDate(2, status == Task.TaskStatus.COMPLETED ? Date.valueOf(LocalDate.now()) : null);
            stmt.setInt(3, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public void markTaskAsCompleted(int id) throws SQLException {
        String sql = "UPDATE tasks SET status = ?, last_completed = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, Task.TaskStatus.COMPLETED.name());
            stmt.setDate(2, Date.valueOf(LocalDate.now()));
            stmt.setInt(3, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}