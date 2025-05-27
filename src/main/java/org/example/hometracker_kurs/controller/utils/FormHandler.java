package org.example.hometracker_kurs.controller.utils;

import javafx.scene.control.*;
import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.model.TaskStatus;

import java.time.LocalDate;

public class FormHandler {
    private final TextField nameField;
    private final TextArea descriptionField;
    private final DatePicker dueDatePicker;
    private final ComboBox<Integer> priorityComboBox;
    private final ComboBox<String> assigneeComboBox;
    private final ComboBox<String> typeComboBox;

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

    public boolean validateForm() {
        return !(nameField.getText().isBlank()
                || dueDatePicker.getValue() == null
                || dueDatePicker.getValue().isBefore(LocalDate.now())
                || assigneeComboBox.getValue() == null
                || typeComboBox.getValue() == null);
    }

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

    public void clearForm() {
        nameField.clear();
        descriptionField.clear();
        dueDatePicker.setValue(null);
        priorityComboBox.setValue(3); // значение по умолчанию
        assigneeComboBox.getSelectionModel().clearSelection();
        typeComboBox.getSelectionModel().clearSelection();
    }
}