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

public class PostgresTaskDAO implements TaskDAO {
    private static final Logger logger = Logger.getLogger(PostgresTaskDAO.class.getName());
    private final Config config;
    private Connection connection;

    public PostgresTaskDAO(Config config) {
        this.config = config;
        initialize();
    }

    private void initialize() {
        try {
            this.connection = getConnection();
            createTableIfNotExists();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize database connection", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(
                    config.getDbUrl(),
                    config.getDbUser(),
                    config.getDbPassword()
            );
        }
        return connection;
    }

    private void createTableIfNotExists() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS tasks (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                description TEXT,
                due_date DATE,
                priority INTEGER,
                assigned_to VARCHAR(50),
                status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED', 'POSTPONED', 'CANCELLED')),
                last_completed DATE,
                frequency_days INTEGER,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            CREATE INDEX IF NOT EXISTS idx_tasks_assigned_to ON tasks(assigned_to);
            CREATE INDEX IF NOT EXISTS idx_tasks_due_date ON tasks(due_date);
            CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    @Override
    public ObservableList<Task> getAllTasks() throws SQLException {
        ObservableList<Task> tasks = FXCollections.observableArrayList();
        String sql = "SELECT * FROM tasks ORDER BY due_date, priority DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                tasks.add(extractTaskFromResultSet(rs));
            }
        }
        return tasks;
    }

    private Task extractTaskFromResultSet(ResultSet rs) throws SQLException {
        return new Task(
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
        String sql = """
            INSERT INTO tasks 
            (name, description, due_date, priority, assigned_to, status, last_completed, frequency_days)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, created_at, updated_at
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setTaskParameters(stmt, task);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    task.setId(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error adding task", e);
            throw e;
        }
    }

    @Override
    public void updateTask(Task task) throws SQLException {
        validateTask(task);
        String sql = """
            UPDATE tasks SET 
            name = ?, description = ?, due_date = ?, priority = ?, 
            assigned_to = ?, status = ?, last_completed = ?, frequency_days = ?,
            updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setTaskParameters(stmt, task);
            stmt.setInt(9, task.getId());
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Task not found with id: " + task.getId());
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating task", e);
            throw e;
        }
    }

    private void setTaskParameters(PreparedStatement stmt, Task task) throws SQLException {
        stmt.setString(1, task.getName());
        stmt.setString(2, task.getDescription());
        stmt.setDate(3, task.getDueDate() != null ? Date.valueOf(task.getDueDate()) : null);
        stmt.setInt(4, task.getPriority());
        stmt.setString(5, task.getAssignedTo());
        stmt.setString(6, task.getStatus().name());
        stmt.setDate(7, task.getLastCompleted() != null ? Date.valueOf(task.getLastCompleted()) : null);
        stmt.setInt(8, task.getFrequencyDays());
    }

    private void validateTask(Task task) throws SQLException {
        if (task == null) {
            throw new SQLException("Task cannot be null");
        }
        if (task.getName() == null || task.getName().trim().isEmpty()) {
            throw new SQLException("Task name cannot be empty");
        }
        if (task.getStatus() == null) {
            throw new SQLException("Task status cannot be null");
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
    public ObservableList<Task> getTasksByAssignee(String assignee) throws SQLException {
        if (assignee == null || assignee.trim().isEmpty()) {
            throw new SQLException("Assignee cannot be empty");
        }

        ObservableList<Task> tasks = FXCollections.observableArrayList();
        String sql = "SELECT * FROM tasks WHERE assigned_to = ? ORDER BY due_date";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, assignee);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(extractTaskFromResultSet(rs));
                }
            }
        }
        return tasks;
    }

    @Override
    public ObservableList<Task> getTasksDueBetween(LocalDate start, LocalDate end) throws SQLException {
        if (start == null || end == null) {
            throw new SQLException("Start and end dates cannot be null");
        }
        if (start.isAfter(end)) {
            throw new SQLException("Start date cannot be after end date");
        }

        ObservableList<Task> tasks = FXCollections.observableArrayList();
        String sql = "SELECT * FROM tasks WHERE due_date BETWEEN ? AND ? ORDER BY due_date";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(extractTaskFromResultSet(rs));
                }
            }
        }
        return tasks;
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

        String sql = """
            UPDATE tasks SET 
            status = ?, 
            last_completed = ?,
            updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setDate(2, status == TaskStatus.COMPLETED ? Date.valueOf(LocalDate.now()) : null);
            stmt.setInt(3, id);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Task not found with id: " + id);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating task status", e);
            throw e;
        }
    }

    @Override
    public void markTaskAsCompleted(int id) throws SQLException {
        updateTaskStatus(id, TaskStatus.COMPLETED);
    }

    @Override
    public ObservableList<Task> getFilteredTasks(String type, String status, String keyword) throws SQLException {
        ObservableList<Task> result = FXCollections.observableArrayList();
        StringBuilder sql = new StringBuilder("SELECT * FROM tasks WHERE 1=1");

        if (type != null && !type.isEmpty()) {
            sql.append(" AND assigned_to = ?");
        }

        if (status != null && !status.equals("Все")) {
            switch (status) {
                case "Активные" -> sql.append(" AND status = 'ACTIVE'");
                case "Выполненные" -> sql.append(" AND status = 'COMPLETED'");
                case "Просроченные" ->
                        sql.append(" AND status = 'ACTIVE' AND due_date < CURRENT_DATE");
            }
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(name) LIKE ? OR LOWER(description) LIKE ?)");
        }

        sql.append(" ORDER BY due_date, priority DESC");

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

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}