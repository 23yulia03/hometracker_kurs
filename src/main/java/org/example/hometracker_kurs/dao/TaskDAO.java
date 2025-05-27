package org.example.hometracker_kurs.dao;

import javafx.collections.ObservableList;
import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.model.TaskStatus;

import java.sql.SQLException;

public interface TaskDAO {
    ObservableList<Task> getAllTasks() throws SQLException;
    Task getTaskById(int id) throws SQLException;
    void addTask(Task task) throws SQLException;
    void updateTask(Task task) throws SQLException;
    void deleteTask(int id) throws SQLException;

    ObservableList<Task> getFilteredTasks(
            String type,
            String status,
            String keyword,
            String sortField,
            boolean ascending
    ) throws SQLException;

    void updateTaskStatus(int id, TaskStatus status) throws SQLException;
    void markTaskAsCompleted(int id) throws SQLException;

    void postponeTask(Task task, int days) throws SQLException;

    void close() throws SQLException;
}