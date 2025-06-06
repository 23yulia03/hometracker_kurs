package org.example.hometracker_kurs.dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.hometracker_kurs.config.DatabaseConfig;
import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.model.TaskStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Реализация интерфейса {@link TaskDAO}, использующая встроенную базу данных H2
 * для хранения и управления задачами.
 * Поддерживает операции CRUD и фильтрацию задач.
 */
public class H2TaskDAO implements TaskDAO {
    private static final Logger logger = Logger.getLogger(H2TaskDAO.class.getName());
    private final Connection connection;

    /**
     * Конструктор, устанавливающий соединение с H2 и выполняющий инициализацию таблиц.
     *
     * @param dbConfig конфигурационный объект с параметрами подключения к H2
     * @throws SQLException если возникает ошибка при подключении
     */
    public H2TaskDAO(DatabaseConfig dbConfig) throws SQLException {
        this.connection = DriverManager.getConnection(
                dbConfig.getH2Url(),
                dbConfig.getH2User(),
                dbConfig.getH2Password()
        );
        createTable();
        migrateDatabase();
        updateOverdueTasks();
    }

    private void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                description TEXT,
                due_date DATE,
                priority INTEGER,
                assigned_to VARCHAR(50),
                status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED', 'POSTPONED', 'CANCELLED', 'OVERDUE')),
                last_completed DATE,
                type VARCHAR(50)
            )
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void migrateDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE tasks DROP COLUMN IF EXISTS frequency_days");
            stmt.execute("ALTER TABLE tasks DROP COLUMN IF EXISTS created_at");
            stmt.execute("ALTER TABLE tasks DROP COLUMN IF EXISTS updated_at");
            stmt.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS type VARCHAR(50)");
            stmt.execute("UPDATE tasks SET type = 'Домашние дела' WHERE type IS NULL");
        }
    }

    @Override
    public ObservableList<Task> getAllTasks() throws SQLException {
        ObservableList<Task> result = FXCollections.observableArrayList();
        String sql = "SELECT * FROM tasks ORDER BY due_date, priority DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(extractTaskFromResultSet(rs));
            }
        }
        return result;
    }

    @Override
    public ObservableList<Task> getFilteredTasks(String type, String status, String keyword, String sortField, boolean ascending) throws SQLException {
        ObservableList<Task> result = FXCollections.observableArrayList();
        StringBuilder sql = new StringBuilder("SELECT * FROM tasks WHERE 1=1");

        if (type != null && !type.isEmpty() && !type.equals("Все")) {
            sql.append(" AND type = ?");
        }

        if (status != null && !status.equals("Все")) {
            switch (status) {
                case "Активные":
                    sql.append(" AND status = 'ACTIVE'");
                    break;
                case "Выполненные":
                    sql.append(" AND status = 'COMPLETED'");
                    break;
                case "Просроченные":
                    sql.append(" AND status = 'OVERDUE'");
                    break;
            }
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(name) LIKE ? OR LOWER(description) LIKE ?)");
        }

        if (sortField != null && !sortField.isEmpty()) {
            sql.append(" ORDER BY ").append(sortField)
                    .append(ascending ? " ASC" : " DESC")
                    .append(", id ASC");
        } else {
            sql.append(" ORDER BY CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END, due_date, priority DESC, id ASC");
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            int index = 1;

            if (type != null && !type.isEmpty() && !type.equals("Все")) {
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
                rs.getDate("last_completed") != null ? rs.getDate("last_completed").toLocalDate() : null
        );
        task.setType(rs.getString("type"));
        return task;
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
        if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())) {
            task.setStatus(TaskStatus.OVERDUE);
        }

        validateTask(task);

        String sql = """
            INSERT INTO tasks (name, description, due_date, priority, assigned_to, status, last_completed, type)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, task.getName());
            stmt.setString(2, task.getDescription());
            stmt.setDate(3, task.getDueDate() != null ? Date.valueOf(task.getDueDate()) : null);
            stmt.setInt(4, task.getPriority());
            stmt.setString(5, task.getAssignedTo());
            stmt.setString(6, task.getStatus().name());
            stmt.setDate(7, task.getLastCompleted() != null ? Date.valueOf(task.getLastCompleted()) : null);
            stmt.setString(8, task.getType());

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    task.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void updateTask(Task task) throws SQLException {
        if (task.getDueDate() != null &&
                task.getDueDate().isBefore(LocalDate.now()) &&
                task.getStatus() != TaskStatus.COMPLETED) {
            task.setStatus(TaskStatus.OVERDUE);
        }

        validateTask(task);

        String sql = """
            UPDATE tasks SET 
            name = ?, description = ?, due_date = ?, priority = ?, 
            assigned_to = ?, status = ?, last_completed = ?, type = ?
            WHERE id = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, task.getName());
            stmt.setString(2, task.getDescription());
            stmt.setDate(3, task.getDueDate() != null ? Date.valueOf(task.getDueDate()) : null);
            stmt.setInt(4, task.getPriority());
            stmt.setString(5, task.getAssignedTo());
            stmt.setString(6, task.getStatus().name());
            stmt.setDate(7, task.getLastCompleted() != null ? Date.valueOf(task.getLastCompleted()) : null);
            stmt.setString(8, task.getType());
            stmt.setInt(9, task.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Task not found with id: " + task.getId());
            }
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
        }
    }

    @Override
    public void updateTaskStatus(int id, TaskStatus status) throws SQLException {
        if (status == null) throw new SQLException("Status cannot be null");

        Task task = getTaskById(id);

        if (!TaskStatus.isTransitionAllowed(task.getStatus(), status)) {
            throw new SQLException(String.format("Invalid status transition: %s -> %s",
                    task.getStatus().getDisplayName(), status.getDisplayName()));
        }

        String sql = """
            UPDATE tasks SET 
            status = ?, 
            last_completed = ?
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
        }
    }

    @Override
    public void markTaskAsCompleted(int id) throws SQLException {
        updateTaskStatus(id, TaskStatus.COMPLETED);
    }

    @Override
    public void postponeTask(Task task, int days) throws SQLException {

    }

    public void updateOverdueTasks() throws SQLException {
        String sql = """
            UPDATE tasks 
            SET status = 'OVERDUE'
            WHERE status IN ('ACTIVE', 'POSTPONED')
              AND due_date IS NOT NULL 
              AND due_date < CURRENT_DATE
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int updatedCount = stmt.executeUpdate();
            if (updatedCount > 0) {
                logger.log(Level.INFO, "Updated {0} tasks to OVERDUE status", updatedCount);
            }
        }
    }

    private void validateTask(Task task) throws SQLException {
        if (task == null) throw new SQLException("Task cannot be null");
        if (task.getName() == null || task.getName().trim().isEmpty())
            throw new SQLException("Task name cannot be empty");
        if (task.getStatus() == null)
            throw new SQLException("Task status cannot be null");
        if (task.getPriority() < 1 || task.getPriority() > 5)
            throw new SQLException("Priority must be between 1 and 5");
        if (task.getType() == null || task.getType().trim().isEmpty())
            throw new SQLException("Task type cannot be empty");
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}