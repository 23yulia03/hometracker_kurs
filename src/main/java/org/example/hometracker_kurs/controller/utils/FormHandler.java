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
        if (nameField.getText().isEmpty()) {
            return false;
        }
        if (dueDatePicker.getValue() == null || dueDatePicker.getValue().isBefore(LocalDate.now())) {
            return false;
        }
        return assigneeComboBox.getValue() != null && typeComboBox.getValue() != null;
    }

    public Task createTaskFromForm() {
        return new Task(
                0,
                nameField.getText(),
                descriptionField.getText(),
                dueDatePicker.getValue(),
                priorityComboBox.getValue(),
                assigneeComboBox.getValue(),
                TaskStatus.ACTIVE,
                null
        );
    }

    public void clearForm() {
        nameField.clear();
        descriptionField.clear();
        dueDatePicker.setValue(null);
        priorityComboBox.setValue(3);
        assigneeComboBox.getSelectionModel().clearSelection();
        typeComboBox.getSelectionModel().clearSelection();
    }
}