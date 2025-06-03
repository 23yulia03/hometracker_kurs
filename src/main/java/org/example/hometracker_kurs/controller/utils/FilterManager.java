package org.example.hometracker_kurs.controller.utils;

import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.service.TaskManagerService;

import java.sql.SQLException;
import java.util.Map;

/**
 * Класс {@code FilterManager} управляет фильтрацией и сортировкой задач
 * на основе пользовательского ввода через графические компоненты.
 * Предоставляет методы применения и сброса фильтров.
 */
public class FilterManager {
    private final ComboBox<String> taskTypeComboBox;
    private final ComboBox<String> statusComboBox;
    private final TextField searchField;
    private final ComboBox<String> sortFieldComboBox;
    private final ComboBox<String> sortOrderComboBox;
    private final TaskManagerService taskManagerService;

    /**
     * Карта отображения названий полей сортировки из интерфейса в имена столбцов базы данных.
     */
    private static final Map<String, String> SORT_MAP = Map.of(
            "По дате", "due_date",
            "По приоритету", "priority",
            "По категории", "assigned_to"
    );

    /**
     * Конструктор {@code FilterManager}.
     *
     * @param taskTypeComboBox   выпадающий список с типами задач
     * @param statusComboBox     выпадающий список со статусами задач
     * @param searchField        поле поиска по названию
     * @param sortFieldComboBox  выпадающий список с критериями сортировки
     * @param sortOrderComboBox  выпадающий список с направлением сортировки
     * @param taskManagerService сервис для получения задач из хранилища
     */
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

    /**
     * Применяет фильтры и сортировку к списку задач.
     *
     * @return отфильтрованный и отсортированный список задач
     * @throws SQLException если происходит ошибка при получении данных
     */
    public ObservableList<Task> applyFilters() throws SQLException {
        String type = "Все".equals(taskTypeComboBox.getValue()) ? null : taskTypeComboBox.getValue();
        String status = "Все".equals(statusComboBox.getValue()) ? null : statusComboBox.getValue();
        String keyword = searchField.getText().isBlank() ? null : searchField.getText();

        String sortField = SORT_MAP.getOrDefault(sortFieldComboBox.getValue(), null);
        boolean ascending = "По возрастанию".equals(sortOrderComboBox.getValue());

        return taskManagerService.applyFilters(type, status, keyword, sortField, ascending);
    }

    /**
     * Сбрасывает значения всех фильтров и полей сортировки к значениям по умолчанию.
     */
    public void resetFilters() {
        taskTypeComboBox.setValue("Все");
        statusComboBox.setValue("Все");
        searchField.clear();
        sortFieldComboBox.setValue("Без сортировки");
        sortOrderComboBox.setValue("По возрастанию");
    }
}
