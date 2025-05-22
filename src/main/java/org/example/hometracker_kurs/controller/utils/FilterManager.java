package org.example.hometracker_kurs.controller.utils;

import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.service.TaskManagerService;

import java.sql.SQLException;

public class FilterManager {
    private final ComboBox<String> taskTypeComboBox;
    private final ComboBox<String> statusComboBox;
    private final TextField searchField;
    private final ComboBox<String> sortFieldComboBox;
    private final ComboBox<String> sortOrderComboBox;
    private final TaskManagerService taskManagerService;

    public FilterManager(ComboBox<String> taskTypeComboBox, ComboBox<String> statusComboBox,
                         TextField searchField, ComboBox<String> sortFieldComboBox,
                         ComboBox<String> sortOrderComboBox, TaskManagerService taskManagerService) {
        this.taskTypeComboBox = taskTypeComboBox;
        this.statusComboBox = statusComboBox;
        this.searchField = searchField;
        this.sortFieldComboBox = sortFieldComboBox;
        this.sortOrderComboBox = sortOrderComboBox;
        this.taskManagerService = taskManagerService;
    }

    public ObservableList<Task> applyFilters() throws SQLException {
        String type = "Все".equals(taskTypeComboBox.getValue()) ? null : taskTypeComboBox.getValue();
        String status = "Все".equals(statusComboBox.getValue()) ? null : statusComboBox.getValue();
        String keyword = searchField.getText().isBlank() ? null : searchField.getText();

        String sortField = switch (sortFieldComboBox.getValue()) {
            case "По дате" -> "due_date";
            case "По приоритету" -> "priority";
            case "По категории" -> "assigned_to";
            default -> null;
        };

        boolean ascending = "По возрастанию".equals(sortOrderComboBox.getValue());

        return taskManagerService.applyFilters(type, status, keyword, sortField, ascending);
    }

    public void resetFilters() {
        taskTypeComboBox.setValue("Все");
        statusComboBox.setValue("Все");
        searchField.clear();
        sortFieldComboBox.setValue("Без сортировки");
        sortOrderComboBox.setValue("По возрастанию");
    }
}