package org.example.hometracker_kurs.dao;

import javafx.collections.ObservableList;
import org.example.hometracker_kurs.model.Task;

import java.sql.SQLException;
import java.time.LocalDate;

public interface TaskDAO {

    // Получить все задачи
    ObservableList<Task> getAllTasks() throws SQLException;

    // Получить задачу по ID
    Task getTaskById(int id) throws SQLException;

    // Добавить новую задачу
    void addTask(Task task) throws SQLException;

    // Обновить существующую задачу
    void updateTask(Task task) throws SQLException;

    // Удалить задачу по ID
    void deleteTask(int id) throws SQLException;

    // Получить задачи, назначенные конкретному пользователю
    ObservableList<Task> getTasksByAssignee(String assignee) throws SQLException;

    // Получить задачи с дедлайнами между двумя датами
    ObservableList<Task> getTasksDueBetween(LocalDate start, LocalDate end) throws SQLException;

    ObservableList<Task> getFilteredTasks(String type, String status, String keyword) throws SQLException;

    // Обновить статус задачи (например: ACTIVE → COMPLETED)
    void updateTaskStatus(int id, Task.TaskStatus status) throws SQLException;

    // Пометить задачу как выполненную (удобный алиас)
    void markTaskAsCompleted(int id) throws SQLException;

    // Закрыть подключение или освободить ресурсы
    void close() throws SQLException;
}
