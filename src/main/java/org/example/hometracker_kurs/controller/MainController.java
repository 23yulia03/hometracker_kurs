package org.example.hometracker_kurs.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.model.TaskStatus;
import org.example.hometracker_kurs.service.TaskManagerService;
import org.example.hometracker_kurs.service.TaskService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private TaskService taskService;
    private TaskManagerService taskManagerService;

    // FXML элементы остаются теми же
    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, TaskStatus> statusColumn;
    @FXML private ComboBox<String> taskTypeComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortFieldComboBox;
    @FXML private ComboBox<String> sortOrderComboBox;
    @FXML private ComboBox<String> dataSourceComboBox;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker dueDatePicker;
    @FXML private ComboBox<Integer> priorityComboBox;
    @FXML private ComboBox<String> assigneeComboBox;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private Label totalTasksLabel;
    @FXML private Label activeTasksLabel;
    @FXML private Label completedTasksLabel;
    @FXML private Label overdueTasksLabel;
    @FXML private Label dataSourceLabel;

    @FXML
    public void initialize() {
        initializeComboBoxes();
        setupTableColumns();
        setupStatusColumn();
    }

    private void initializeComboBoxes() {
        // Инициализация ComboBox (остается без изменений)
        priorityComboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        priorityComboBox.setValue(3);
        assigneeComboBox.setItems(FXCollections.observableArrayList("Мама", "Папа", "Ребенок", "Другое"));
        assigneeComboBox.setValue("Мама");
        typeComboBox.setItems(FXCollections.observableArrayList(
                "Уборка", "Покупки", "Приготовление еды", "Сад и огород",
                "Ремонт и обслуживание", "Финансы", "Здоровье", "Хобби и личное", "Прочее"));
        typeComboBox.setValue("Уборка");
        taskTypeComboBox.setItems(FXCollections.observableArrayList(
                "Все", "Уборка", "Покупки", "Приготовление еды", "Сад и огород",
                "Ремонт и обслуживание", "Финансы", "Здоровье", "Хобби и личное", "Прочее"));
        taskTypeComboBox.setValue("Все");
        statusComboBox.setItems(FXCollections.observableArrayList("Все", "Активные", "Выполненные", "Просроченные"));
        statusComboBox.setValue("Все");
        sortFieldComboBox.setItems(FXCollections.observableArrayList(
                "Без сортировки", "По дате", "По приоритету", "По категории"));
        sortFieldComboBox.setValue("Без сортировки");
        sortOrderComboBox.setItems(FXCollections.observableArrayList("По возрастанию", "По убыванию"));
        sortOrderComboBox.setValue("По возрастанию");
    }

    private void setupTableColumns() {
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        taskTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> {
                    if (newSel != null) {
                        fillFormWithSelectedTask(newSel);
                    }
                });
    }

    private void setupStatusColumn() {
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(TaskStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(status.getDisplayName());
                    switch (status) {
                        case ACTIVE -> setTextFill(Color.GREEN);
                        case COMPLETED -> setTextFill(Color.BLUE);
                        case POSTPONED -> setTextFill(Color.ORANGE);
                        case CANCELLED -> setTextFill(Color.GRAY);
                        case OVERDUE -> setTextFill(Color.RED);
                    }
                }
            }
        });
    }

    @FXML
    private void refreshData() {
        try {
            ObservableList<Task> tasks = taskManagerService.refreshData();
            taskTable.setItems(tasks);
            updateStatistics();
        } catch (SQLException e) {
            showAlert("Ошибка загрузки задач", e.getMessage());
            taskTable.setItems(FXCollections.observableArrayList());
        }
    }

    @FXML
    private void addTask() {
        if (!validateForm()) return;
        try {
            Task task = createTaskFromForm();
            taskManagerService.addTask(task);
            refreshData();
            clearForm();
        } catch (SQLException e) {
            showAlert("Ошибка добавления", e.getMessage());
        }
    }

    @FXML
    private void updateTask() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите задачу для обновления");
            return;
        }

        if (!validateForm()) return;

        try {
            Task updated = createTaskFromForm();
            updated.setId(selected.getId());
            taskManagerService.updateTask(updated);
            refreshData();
        } catch (SQLException e) {
            showAlert("Ошибка обновления", e.getMessage());
        }
    }

    @FXML
    private void deleteTask() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите задачу для удаления");
            return;
        }

        try {
            taskManagerService.deleteTask(selected);
            refreshData();
        } catch (SQLException e) {
            showAlert("Ошибка удаления", e.getMessage());
        }
    }

    @FXML
    private void completeTask() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите задачу для выполнения");
            return;
        }

        try {
            taskManagerService.completeTask(selected);
            refreshData();
            taskTable.refresh();
            updateStatistics();
        } catch (SQLException e) {
            showAlert("Ошибка выполнения", e.getMessage());
        }
    }

    @FXML
    private void postponeTask() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите задачу для откладывания");
            return;
        }

        if (selected.getStatus() == TaskStatus.POSTPONED) {
            showAlert("Информация", "Задача уже отложена");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("7");
        dialog.setTitle("Отложить задачу");
        dialog.setHeaderText("На сколько дней отложить задачу?");
        dialog.setContentText("Дней:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(days -> {
            try {
                int daysToPostpone = Integer.parseInt(days);
                taskManagerService.postponeTask(selected, daysToPostpone);
                refreshData();
                taskTable.refresh();
                showAlert("Успех", "Задача успешно отложена на " + days + " дней");
            } catch (NumberFormatException e) {
                showAlert("Ошибка", "Введите корректное число дней");
            } catch (SQLException e) {
                showAlert("Ошибка", "Не удалось отложить задачу: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                showAlert("Ошибка", e.getMessage());
            }
        });
    }

    @FXML
    private void reactivateTask() {
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

    @FXML
    private void applyFilters() {
        try {
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

            ObservableList<Task> filtered = taskManagerService.applyFilters(
                    type, status, keyword, sortField, ascending);

            taskTable.setItems(filtered);
            updateStatistics();
        } catch (SQLException e) {
            showAlert("Ошибка фильтрации", e.getMessage());
        }
    }

    @FXML
    private void resetFilters() {
        taskTypeComboBox.setValue("Все");
        statusComboBox.setValue("Все");
        searchField.clear();
        sortFieldComboBox.setValue("Без сортировки");
        sortOrderComboBox.setValue("По возрастанию");
        refreshData();
    }

    @FXML
    private void switchDataSource() {
        String selectedSource = dataSourceComboBox.getValue();
        if (selectedSource == null) {
            showAlert("Ошибка", "Выберите источник данных");
            return;
        }

        try {
            String daoKey = switch (selectedSource) {
                case "PostgreSQL" -> "postgres";
                case "Excel" -> "excel";
                case "H2 Database" -> "h2";
                default -> throw new IllegalArgumentException("Неизвестный источник: " + selectedSource);
            };

            if (this.taskService != null) {
                try {
                    this.taskService.close();
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Ошибка при закрытии подключения", e);
                }
            }

            this.taskService = new TaskService(daoKey);
            this.taskManagerService = new TaskManagerService(taskService);
            dataSourceLabel.setText("Выбранный источник данных: " + selectedSource);
            refreshData();
        } catch (Exception e) {
            showAlert("Ошибка источника", e.getMessage());
            taskTable.setItems(FXCollections.observableArrayList());
            dataSourceLabel.setText("Ошибка подключения");
        }
    }

    // Остальные вспомогательные методы остаются без изменений
    private boolean validateForm() {
        if (nameField.getText().isEmpty()) {
            showAlert("Ошибка", "Введите название задачи");
            return false;
        }
        if (dueDatePicker.getValue() == null || dueDatePicker.getValue().isBefore(LocalDate.now())) {
            showAlert("Ошибка", "Срок выполнения некорректен");
            return false;
        }
        if (assigneeComboBox.getValue() == null || typeComboBox.getValue() == null) {
            showAlert("Ошибка", "Заполните все обязательные поля");
            return false;
        }
        return true;
    }

    private Task createTaskFromForm() {
        Task task = new Task(
                0,
                nameField.getText(),
                descriptionField.getText(),
                dueDatePicker.getValue(),
                priorityComboBox.getValue(),
                assigneeComboBox.getValue(),
                TaskStatus.ACTIVE,
                null
        );
        task.setType(typeComboBox.getValue());
        return task;
    }

    private void fillFormWithSelectedTask(Task t) {
        nameField.setText(t.getName());
        descriptionField.setText(t.getDescription());
        dueDatePicker.setValue(t.getDueDate());
        priorityComboBox.setValue(t.getPriority());
        assigneeComboBox.setValue(t.getAssignedTo());
        typeComboBox.setValue(t.getType());
    }

    private void clearForm() {
        nameField.clear();
        descriptionField.clear();
        dueDatePicker.setValue(null);
        priorityComboBox.setValue(3);
        assigneeComboBox.getSelectionModel().clearSelection();
        typeComboBox.getSelectionModel().clearSelection();
    }

    private void updateStatistics() {
        ObservableList<Task> tasks = taskTable.getItems();
        if (tasks == null) tasks = FXCollections.observableArrayList();

        int total = tasks.size();
        int active = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.ACTIVE).count();
        int completed = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        int overdue = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.OVERDUE).count();

        Platform.runLater(() -> {
            totalTasksLabel.setText(String.valueOf(total));
            activeTasksLabel.setText(String.valueOf(active));
            completedTasksLabel.setText(String.valueOf(completed));
            overdueTasksLabel.setText(String.valueOf(overdue));
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}