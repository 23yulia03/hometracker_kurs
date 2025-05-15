package org.example.hometracker_kurs.service;

import javafx.collections.ObservableList;
import org.example.hometracker_kurs.dao.TaskDAO;
import org.example.hometracker_kurs.dao.TaskDAOFactory;
import org.example.hometracker_kurs.model.Config;
import org.example.hometracker_kurs.model.Task;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.function.Predicate;

public class TaskService {
    private final TaskDAO taskDAO;

    public TaskService(String daoType) {
        Config config = new Config();
        try {
            this.taskDAO = TaskDAOFactory.createTaskDAO(daoType, config);
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось создать DAO: " + e.getMessage(), e);
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
                case "Активные" -> filter = filter.and(t -> t.getStatus() == Task.TaskStatus.ACTIVE);
                case "Выполненные" -> filter = filter.and(t -> t.getStatus() == Task.TaskStatus.COMPLETED);
                case "Просроченные" -> filter = filter.and(t ->
                        t.getStatus() == Task.TaskStatus.ACTIVE &&
                                t.getDueDate() != null &&
                                t.getDueDate().isBefore(LocalDate.now()));
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
}
