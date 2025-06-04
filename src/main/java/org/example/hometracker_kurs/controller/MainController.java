// Импорты
package org.example.hometracker_kurs.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import org.example.hometracker_kurs.config.DatabaseConfig;
import org.example.hometracker_kurs.config.ExcelConfig;
import org.example.hometracker_kurs.controller.utils.FilterManager;
import org.example.hometracker_kurs.controller.utils.FormHandler;
import org.example.hometracker_kurs.controller.utils.StatisticsCalculator;
import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.model.TaskStatus;
import org.example.hometracker_kurs.service.TaskManagerService;
import org.example.hometracker_kurs.service.TaskService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MainController {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private TaskService taskService;
    private TaskManagerService taskManagerService;
    private FormHandler formHandler;
    private FilterManager filterManager;
    private StatisticsCalculator statisticsCalculator;

    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, TaskStatus> statusColumn;
    @FXML private TableColumn<Task, LocalDate> dueDateColumn;
    @FXML private ComboBox<String> taskTypeComboBox, statusComboBox, sortFieldComboBox, sortOrderComboBox, dataSourceComboBox, assigneeComboBox, typeComboBox;
    @FXML private TextField searchField, nameField;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker dueDatePicker;
    @FXML private ComboBox<Integer> priorityComboBox;
    @FXML private Label totalTasksLabel, activeTasksLabel, completedTasksLabel, overdueTasksLabel, dataSourceLabel;
    @FXML private Label syncStatusLabel;

    @FXML
    public void initialize() {
        initializeComboBoxes();
        setupTableColumns();
        setupStatusColumn();

        formHandler = new FormHandler(nameField, descriptionField, dueDatePicker, priorityComboBox, assigneeComboBox, typeComboBox);
        filterManager = new FilterManager(taskTypeComboBox, statusComboBox, searchField, sortFieldComboBox, sortOrderComboBox, taskManagerService);
        statisticsCalculator = new StatisticsCalculator(totalTasksLabel, activeTasksLabel, completedTasksLabel, overdueTasksLabel);
    }

    private void initializeComboBoxes() {
        priorityComboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        priorityComboBox.setValue(3);
        assigneeComboBox.setItems(FXCollections.observableArrayList("Мама", "Папа", "Ребенок", "Другое"));
        assigneeComboBox.setValue("Мама");
        typeComboBox.setItems(FXCollections.observableArrayList("Уборка", "Покупки", "Приготовление еды", "Сад и огород", "Ремонт и обслуживание", "Финансы", "Здоровье", "Хобби и личное", "Прочее"));
        typeComboBox.setValue("Уборка");
        taskTypeComboBox.setItems(FXCollections.observableArrayList("Все", "Уборка", "Покупки", "Приготовление еды", "Сад и огород", "Ремонт и обслуживание", "Финансы", "Здоровье", "Хобби и личное", "Прочее"));
        taskTypeComboBox.setValue("Все");
        statusComboBox.setItems(FXCollections.observableArrayList("Все", "Активные", "Выполненные", "Просроченные"));
        statusComboBox.setValue("Все");
        sortFieldComboBox.setItems(FXCollections.observableArrayList("Без сортировки", "По дате", "По приоритету", "По категории"));
        sortFieldComboBox.setValue("Без сортировки");
        sortOrderComboBox.setItems(FXCollections.observableArrayList("По возрастанию", "По убыванию"));
        sortOrderComboBox.setValue("По возрастанию");
    }

    private void setupTableColumns() {
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        taskTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) fillFormWithSelectedTask(newSel);
        });

        dueDateColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate dueDate, boolean empty) {
                super.updateItem(dueDate, empty);
                if (empty || dueDate == null) {
                    setText(null);
                } else {
                    // Форматируем дату в дд.мм.гггг
                    String formattedDate = String.format("%02d.%02d.%d",
                            dueDate.getDayOfMonth(),
                            dueDate.getMonthValue(),
                            dueDate.getYear());

                    long count = taskTable.getItems().stream()
                            .filter(t -> dueDate.equals(t.getDueDate()))
                            .count();

                    if (count > 1) {
                        setText("⚠ " + formattedDate);
                        setTextFill(Color.ORANGERED);
                    } else {
                        setText(formattedDate);
                        setTextFill(Color.BLACK);
                    }
                }
            }
        });
    }

    private void setupStatusColumn() {
        statusColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getStatus()));
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(TaskStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setTextFill(Color.BLACK);
                } else {
                    setText(status.getDisplayName());
                    setTextFill(switch (status) {
                        case ACTIVE -> Color.GREEN;
                        case COMPLETED -> Color.BLUE;
                        case POSTPONED -> Color.ORANGE;
                        case CANCELLED -> Color.GRAY;
                        case OVERDUE -> Color.RED;
                        default -> Color.BLACK;
                    });
                }
            }
        });
    }

    @FXML private void refreshData() {
        reloadAndRefresh();
    }

    @FXML private void addTask() {
        if (!formHandler.validateForm()) {
            showAlert("Ошибка", "Заполните все обязательные поля");
            return;
        }

        LocalDate date = dueDatePicker.getValue();
        if (isDateConflict(date, null)) {
            showAlert("Ошибка", "На выбранную дату уже есть задача.");
            return;
        }

        try {
            taskManagerService.addTask(formHandler.createTaskFromForm());
            formHandler.clearForm();
            reloadAndRefresh();
        } catch (SQLException e) {
            showAlert("Ошибка добавления", e.getMessage());
        }
    }

    @FXML private void updateTask() {
        Task selected = getSelectedTaskOrAlert("обновления");
        if (selected == null || !formHandler.validateForm()) return;

        try {
            Task updated = formHandler.createTaskFromForm();
            updated.setId(selected.getId());

            if (isDateConflict(updated.getDueDate(), (int) selected.getId())) {
                showAlert("Ошибка", "На эту дату уже есть другая задача.");
                return;
            }

            taskManagerService.updateTask(updated);
            taskTable.refresh();
            reloadAndRefresh();
        } catch (SQLException e) {
            showAlert("Ошибка обновления", e.getMessage());
        }
    }

    @FXML private void deleteTask() {
        Task selected = getSelectedTaskOrAlert("удаления");
        if (selected == null) return;

        try {
            taskManagerService.deleteTask(selected);
            reloadAndRefresh();
        } catch (SQLException e) {
            showAlert("Ошибка удаления", e.getMessage());
        }
    }

    @FXML private void completeTask() {
        Task selected = getSelectedTaskOrAlert("выполнения");
        if (selected == null) return;

        try {
            taskManagerService.completeTask(selected);
            reloadAndRefresh();
            taskTable.refresh();
        } catch (SQLException e) {
            showAlert("Ошибка выполнения", e.getMessage());
        }
    }

    @FXML private void postponeTask() {
        Task selected = getSelectedTaskOrAlert("откладывания");
        if (selected == null || selected.getStatus() == TaskStatus.POSTPONED) {
            showAlert("Информация", "Задача уже отложена");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("7");
        dialog.setTitle("Отложить задачу");
        dialog.setHeaderText("На сколько дней отложить?");
        dialog.setContentText("Дней:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                int daysToPostpone = Integer.parseInt(result.get());
                taskManagerService.postponeTask(selected, daysToPostpone);
                taskTable.refresh();
                reloadAndRefresh();
                showAlert("Успех", "Задача отложена на " + daysToPostpone + " дней");
            } catch (NumberFormatException e) {
                showAlert("Ошибка", "Введите корректное число дней");
            } catch (SQLException e) {
                showAlert("Ошибка", "Не удалось отложить задачу: " + e.getMessage());
            }
        }
    }

    @FXML private void reactivateTask() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите задачу для активации");
            return;
        }

        try {
            taskManagerService.reactivateTask(selected);
            refreshData();
            taskTable.refresh();
            showAlert("Успех", "Задача снова активна");
        } catch (SQLException e) {
            showAlert("Ошибка", "Не удалось активировать задачу: " + e.getMessage());
        }
    }

    @FXML private void applyFilters() {
        try {
            ObservableList<Task> filtered = filterManager.applyFilters();
            taskTable.setItems(filtered);
            statisticsCalculator.updateStatistics(filtered);
        } catch (SQLException e) {
            showAlert("Ошибка фильтрации", e.getMessage());
        }
    }

    @FXML private void resetFilters() {
        filterManager.resetFilters();
        refreshData();
    }

    @FXML private void switchDataSource() {
        String selectedSource = dataSourceComboBox.getValue();
        if (selectedSource == null) {
            showAlert("Ошибка", "Выберите источник данных");
            return;
        }

        try {
            if (taskService != null) taskService.close();

            String daoKey = switch (selectedSource) {
                case "PostgreSQL" -> "postgres";
                case "Excel" -> "excel";
                case "H2 Database" -> "h2";
                default -> throw new IllegalArgumentException("Неизвестный источник: " + selectedSource);
            };

            DatabaseConfig dbConfig = new DatabaseConfig();
            ExcelConfig excelConfig = new ExcelConfig();
            this.taskService = new TaskService(daoKey, dbConfig, excelConfig);
            this.taskManagerService = new TaskManagerService(taskService);
            this.filterManager = new FilterManager(taskTypeComboBox, statusComboBox, searchField,
                    sortFieldComboBox, sortOrderComboBox, taskManagerService);

            dataSourceLabel.setText("Источник: " + selectedSource);
            reloadAndRefresh();
            taskService.trySyncPendingTasks();
            updateSyncStatusLabel("всё в порядке", Color.GREEN);

        } catch (Exception e) {
            showAlert("Ошибка подключения", e.getMessage());
            updateSyncStatusLabel("ошибка синхронизации", Color.ORANGE);
        }
    }

    private Task getSelectedTaskOrAlert(String context) {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите задачу для " + context);
        }
        return selected;
    }

    private void reloadAndRefresh() {
        try {
            ObservableList<Task> tasks = taskManagerService.refreshData();
            taskTable.setItems(tasks);
            statisticsCalculator.updateStatistics(tasks);
        } catch (SQLException e) {
            showAlert("Ошибка обновления", e.getMessage());
        }
    }

    private void fillFormWithSelectedTask(Task task) {
        nameField.setText(task.getName());
        descriptionField.setText(task.getDescription());
        dueDatePicker.setValue(task.getDueDate());
        priorityComboBox.setValue(task.getPriority());
        assigneeComboBox.setValue(task.getAssignedTo());
        typeComboBox.setValue(task.getType());
    }

    private void updateSyncStatusLabel(String message, Color color) {
        Platform.runLater(() -> {
            syncStatusLabel.setText("Синхронизация: " + message);
            syncStatusLabel.setTextFill(color);
        });
    }

    private boolean isDateConflict(LocalDate date, Integer ignoreId) {
        if (date == null) return false;
        return taskTable.getItems().stream()
                .filter(t -> date.equals(t.getDueDate()))
                .filter(t -> ignoreId == null || !Objects.equals(t.getId(), ignoreId))
                .count() > 0;
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setContentText(message);
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }
}
