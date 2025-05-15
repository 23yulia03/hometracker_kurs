package org.example.hometracker_kurs.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.model.TaskStatus;
import org.example.hometracker_kurs.service.TaskService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

public class MainController {
    private TaskService taskService;

    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, TaskStatus> statusColumn;
    @FXML private ComboBox<String> taskTypeComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TextField searchField;

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
        // Инициализация ComboBox
        priorityComboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        priorityComboBox.setValue(3); // Устанавливаем значение по умолчанию

        assigneeComboBox.setItems(FXCollections.observableArrayList("Мама", "Папа", "Ребенок", "Другое"));

        typeComboBox.setItems(FXCollections.observableArrayList("Домашние дела", "Работа", "Личное", "Семья", "Покупки", "Здоровье"));

        taskTypeComboBox.setItems(FXCollections.observableArrayList("Все", "Домашние дела", "Работа", "Личное", "Семья", "Покупки", "Здоровье"));
        taskTypeComboBox.setValue("Все");

        statusComboBox.setItems(FXCollections.observableArrayList("Все", "Активные", "Выполненные", "Просроченные"));
        statusComboBox.setValue("Все");
    }

    private void setupTableColumns() {
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        taskTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> {
                    if (newSel != null) fillFormWithSelectedTask(newSel);
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
                        case OVERDUE -> setTextFill(Color.RED);  // красный для просроченных
                    }
                }
            }
        });
    }

    private void setDataSource(String type) {
        try {
            String daoKey = switch (type) {
                case "PostgreSQL" -> "postgres";
                case "Excel" -> "excel";
                case "H2 Database" -> "h2";
                default -> throw new IllegalArgumentException("Неизвестный источник: " + type);
            };

            this.taskService = new TaskService(daoKey);

            // Обновляем метку с выбранным источником
            dataSourceLabel.setText("Выбранный источник данных: " + type);

            refreshData();
        } catch (Exception e) {
            showAlert("Ошибка источника", e.getMessage());
            taskTable.setItems(FXCollections.observableArrayList());
            dataSourceLabel.setText("Ошибка подключения");
        }
    }

    @FXML
    private void refreshData() {
        try {
            ObservableList<Task> tasks = taskService.getAllTasks();
            taskTable.setItems(tasks);
            updateStatistics();
            updateOverdueTasks();  // безопасно вызывать здесь
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
            taskService.addTask(task);
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
            taskService.updateTask(updated);
            refreshData();
        } catch (SQLException e) {
            showAlert("Ошибка обновления", e.getMessage());
        }
    }

    private Task getSelectedTaskOrAlert(String action) {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите задачу для " + action);
            return null;
        }
        return selected;
    }

    @FXML
    private void deleteTask() {
        Task selected = getSelectedTaskOrAlert("удаления");
        if (selected == null) return;

        try {
            taskService.deleteTask(selected.getId());
            refreshData();
        } catch (SQLException e) {
            showAlert("Ошибка удаления", e.getMessage());
        }
    }

    @FXML
    private void completeTask() {
        Task selected = getSelectedTaskOrAlert("выполнения");
        if (selected == null) return;

        try {
            taskService.completeTask(selected.getId());
            refreshData();
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

        // Проверяем, не является ли задача уже отложенной
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
                taskService.postponeTask(selected.getId(), daysToPostpone);
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
    private void cancelTask() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите задачу для отмены");
            return;
        }

        try {
            taskService.cancelTask(selected.getId());
            refreshData();
            taskTable.refresh();
            showAlert("Успех", "Задача отменена");
        } catch (SQLException e) {
            showAlert("Ошибка", "Не удалось отменить задачу: " + e.getMessage());
        }
    }

    @FXML
    private void reactivateTask() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите задачу для активации");
            return;
        }

        try {
            taskService.reactivateTask(selected.getId());
            refreshData();
            taskTable.refresh();
            showAlert("Успех", "Задача снова активна");
        } catch (SQLException e) {
            showAlert("Ошибка", "Не удалось активировать задачу: " + e.getMessage());
        }
    }

    @FXML
    private void applyFilters() {
        if (taskService == null) return;
        try {
            String type = taskTypeComboBox.getValue();
            String status = statusComboBox.getValue();
            String search = searchField.getText();

            ObservableList<Task> filtered = taskService.getFilteredTasks(
                    "Все".equals(type) ? null : type,
                    status,
                    search.isBlank() ? null : search
            );

            taskTable.setItems(filtered);
            updateStatistics();
        } catch (SQLException e) {
            showAlert("Ошибка фильтрации", e.getMessage());
        }
    }

    @FXML
    private void resetFilters() {
        // Сбрасываем значения фильтров к исходным
        taskTypeComboBox.setValue("Все");
        statusComboBox.setValue("Все");
        searchField.clear();

        // Обновляем данные (показываем все задачи без фильтрации)
        refreshData();
    }

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
                null,
                0
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

        totalTasksLabel.setText(String.valueOf(total));
        activeTasksLabel.setText(String.valueOf(active));
        completedTasksLabel.setText(String.valueOf(completed));
        overdueTasksLabel.setText(String.valueOf(overdue));
    }


    private void updateOverdueTasks() {
        try {
            ObservableList<Task> tasks = taskService.getAllTasks();

            boolean updated = false;

            for (Task task : tasks) {
                if (task.getStatus() == TaskStatus.ACTIVE && task.getDueDate() != null &&
                        task.getDueDate().isBefore(LocalDate.now())) {
                    task.setStatus(TaskStatus.OVERDUE);
                    taskService.updateTask(task);
                    updated = true;
                }
            }

            if (updated) {
                refreshData();
            }
        } catch (SQLException e) {
            showAlert("Ошибка обновления статусов", e.getMessage());
        }
    }

    @FXML
    private void switchDataSource() {
        String currentLabel = dataSourceLabel.getText();
        String currentSource = currentLabel.replace("Выбранный источник данных: ", "");

        switch (currentSource) {
            case "Excel" -> setDataSource("Excel");
            case "PostgreSQL" -> setDataSource("PostgreSQL");
            case "H2 Database" -> setDataSource("H2 Database");
            default -> setDataSource("Excel");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
