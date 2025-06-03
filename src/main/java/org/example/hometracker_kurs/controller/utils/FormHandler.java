package org.example.hometracker_kurs.controller.utils;

import javafx.scene.control.*;
import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.model.TaskStatus;

import java.time.LocalDate;

/**
 * Класс {@code FormHandler} отвечает за обработку формы задачи в пользовательском интерфейсе.
 * Предоставляет методы для валидации, создания объекта задачи и очистки полей формы.
 */
public class FormHandler {
    private final TextField nameField;
    private final TextArea descriptionField;
    private final DatePicker dueDatePicker;
    private final ComboBox<Integer> priorityComboBox;
    private final ComboBox<String> assigneeComboBox;
    private final ComboBox<String> typeComboBox;

    /**
     * Конструктор {@code FormHandler}.
     *
     * @param nameField          поле ввода названия задачи
     * @param descriptionField   поле ввода описания задачи
     * @param dueDatePicker      компонент выбора даты выполнения
     * @param priorityComboBox   выпадающий список для выбора приоритета
     * @param assigneeComboBox   выпадающий список исполнителей
     * @param typeComboBox       выпадающий список типов задач
     */
    public FormHandler(TextField nameField, TextArea descriptionField, DatePicker dueDatePicker,
                       ComboBox<Integer> priorityComboBox, ComboBox<String> assigneeComboBox,
                       ComboBox<String> typeComboBox) {
        this.nameField = nameField;
        this.descriptionField = descriptionField;
        this.dueDatePicker = dueDatePicker;
        this.priorityComboBox = priorityComboBox;
        this.assigneeComboBox = assigneeComboBox;
        this.typeComboBox = typeComboBox;
    }

    /**
     * Проверяет корректность заполнения формы задачи.
     *
     * @return {@code true}, если все обязательные поля заполнены корректно;
     *         {@code false}, если хотя бы одно поле некорректно
     */
    public boolean validateForm() {
        return !(nameField.getText().isBlank()
                || dueDatePicker.getValue() == null
                || dueDatePicker.getValue().isBefore(LocalDate.now())
                || assigneeComboBox.getValue() == null
                || typeComboBox.getValue() == null);
    }

    /**
     * Создает объект {@link Task} на основе данных, введённых в форму.
     *
     * @return новый объект задачи
     */
    public Task createTaskFromForm() {
        Task task = new Task(
                0,
                nameField.getText().trim(),
                descriptionField.getText().trim(),
                dueDatePicker.getValue(),
                priorityComboBox.getValue(),
                assigneeComboBox.getValue(),
                TaskStatus.ACTIVE,
                null
        );
        task.setType(typeComboBox.getValue());
        return task;
    }

    /**
     * Очищает все поля формы и сбрасывает значения по умолчанию.
     */
    public void clearForm() {
        nameField.clear();
        descriptionField.clear();
        dueDatePicker.setValue(null);
        priorityComboBox.setValue(3); // значение по умолчанию
        assigneeComboBox.getSelectionModel().clearSelection();
        typeComboBox.getSelectionModel().clearSelection();
    }
}
