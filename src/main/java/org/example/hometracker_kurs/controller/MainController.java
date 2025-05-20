package org.example.hometracker_kurs.controller;

import javafx.application.Platform;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private TaskService taskService;

    // Элементы управления таблицей и фильтрами
    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, TaskStatus> statusColumn;
    @FXML private ComboBox<String> taskTypeComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortFieldComboBox;
    @FXML private ComboBox<String> sortOrderComboBox;
    @FXML private ComboBox<String> dataSourceComboBox;

    // Элементы формы редактирования
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker dueDatePicker;
    @FXML private ComboBox<Integer> priorityComboBox;
    @FXML private ComboBox<String> assigneeComboBox;
    @FXML private ComboBox<String> typeComboBox;

    // Элементы статистики
    @FXML private Label totalTasksLabel;
    @FXML private Label activeTasksLabel;
    @FXML private Label completedTasksLabel;
    @FXML private Label overdueTasksLabel;

    // Информация об источнике данных
    @FXML private Label dataSourceLabel;

    @FXML
    public void initialize() {
        initializeComboBoxes();
        setupTableColumns();
        setupStatusColumn();
        initializeSortingControls();
    }

    private void initializeComboBoxes() {
        // Инициализация ComboBox для приоритета
        priorityComboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        priorityComboBox.setValue(3);

        // Инициализация ComboBox для исполнителей
        assigneeComboBox.setItems(FXCollections.observableArrayList(
                "Мама", "Папа", "Ребенок", "Другое"));
        assigneeComboBox.setValue("Мама");

        // Инициализация ComboBox для типов задач
        typeComboBox.setItems(FXCollections.observableArrayList(
                "Уборка", "Покупки", "Приготовление еды", "Сад и огород", "Ремонт и обслуживание", "Финансы", "Здоровье", "Хобби и личное", "Прочее"));
        typeComboBox.setValue("Уборка");

        // Инициализация ComboBox для фильтрации по типу
        taskTypeComboBox.setItems(FXCollections.observableArrayList(
                "Уборка", "Покупки", "Приготовление еды", "Сад и огород", "Ремонт и обслуживание", "Финансы", "Здоровье", "Хобби и личное", "Прочее"));
        taskTypeComboBox.setValue("Все");

        // Инициализация ComboBox для фильтрации по статусу
        statusComboBox.setItems(FXCollections.observableArrayList(
                "Все", "Активные", "Выполненные", "Просроченные"));
        statusComboBox.setValue("Все");
    }

    private void initializeSortingControls() {
        // Инициализация ComboBox для выбора поля сортировки
        sortFieldComboBox.setItems(FXCollections.observableArrayList(
                "Без сортировки", "По дате", "По приоритету", "По категории"));
        sortFieldComboBox.setValue("Без сортировки");

        // Инициализация ComboBox для выбора направления сортировки
        sortOrderComboBox.setItems(FXCollections.observableArrayList(
                "По возрастанию", "По убыванию"));
        sortOrderComboBox.setValue("По возрастанию");
    }

    private void setupTableColumns() {
        // Настройка политики изменения размера столбцов
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Обработчик выбора задачи в таблице
        taskTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> {
                    if (newSel != null) {
                        fillFormWithSelectedTask(newSel);
                    }
                });
    }

    private void setupStatusColumn() {
        // Настройка отображения статуса с цветами
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

    private void setDataSource(String type) {
        try {
            String daoKey = switch (type) {
                case "PostgreSQL" -> "postgres";
                case "Excel" -> "excel";
                case "H2 Database" -> "h2";
                default -> throw new IllegalArgumentException("Неизвестный источник: " + type);
            };

            this.taskService = new TaskService(daoKey);
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
            taskTable.refresh();
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
            // Получаем параметры фильтрации
            String type = "Все".equals(taskTypeComboBox.getValue()) ? null : taskTypeComboBox.getValue();
            String status = "Все".equals(statusComboBox.getValue()) ? null : statusComboBox.getValue();
            String keyword = searchField.getText().isBlank() ? null : searchField.getText();

            // Преобразуем параметры сортировки в поля базы данных
            String sortField = switch (sortFieldComboBox.getValue()) {
                case "По дате" -> "due_date";
                case "По приоритету" -> "priority";
                case "По категории" -> "assigned_to";
                default -> null;
            };

            boolean ascending = "По возрастанию".equals(sortOrderComboBox.getValue());

            // Получаем отфильтрованные и отсортированные задачи
            ObservableList<Task> filtered = taskService.getFilteredTasks(
                    type, status, keyword, sortField, ascending);

            // Обновляем таблицу
            taskTable.setItems(filtered);
            updateStatistics();
        } catch (SQLException e) {
            showAlert("Ошибка фильтрации", e.getMessage());
        }
    }

    @FXML
    private void resetFilters() {
        // Сбрасываем все фильтры
        taskTypeComboBox.setValue("Все");
        statusComboBox.setValue("Все");
        searchField.clear();
        sortFieldComboBox.setValue("Без сортировки");
        sortOrderComboBox.setValue("По возрастанию");

        // Обновляем данные
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
                if (task.getStatus() == TaskStatus.ACTIVE &&
                        task.getDueDate() != null &&
                        task.getDueDate().isBefore(LocalDate.now()) &&
                        task.getStatus() != TaskStatus.OVERDUE) {

                    task.setStatus(TaskStatus.OVERDUE);
                    taskService.updateTask(task);
                    updated = true;
                }
            }

            if (updated) {
                Platform.runLater(() -> {
                    refreshData();
                    showAlert("Информация", "Статусы задач были обновлены (найдены просроченные)");
                });
            }
        } catch (SQLException e) {
            Platform.runLater(() ->
                    showAlert("Ошибка обновления статусов", e.getMessage()));
        }
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

            // Закрываем предыдущее подключение, если оно есть
            if (this.taskService != null) {
                try {
                    this.taskService.close();
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Ошибка при закрытии подключения", e);
                }
            }

            // Создаем новый сервис с выбранным DAO
            this.taskService = new TaskService(daoKey);
            dataSourceLabel.setText("Выбранный источник данных: " + selectedSource);
            refreshData();
        } catch (Exception e) {
            showAlert("Ошибка источника", e.getMessage());
            taskTable.setItems(FXCollections.observableArrayList());
            dataSourceLabel.setText("Ошибка подключения");
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
