package org.example.hometracker_kurs.dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.hometracker_kurs.model.Config;
import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.model.TaskStatus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExcelTaskDAO implements TaskDAO {
    private static final Logger logger = Logger.getLogger(ExcelTaskDAO.class.getName());
    private final String filePath;
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private int nextId = 1;

    public ExcelTaskDAO(Config config) {
        this.filePath = config.getExcelPath();
        loadTasks();
    }

    private void loadTasks() {
        File file = new File(filePath);
        if (!file.exists()) {
            logger.log(Level.INFO, "Excel file not found at {0}, will be created on first save", filePath);
            return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // Пропускаем заголовок
            if (rowIterator.hasNext()) rowIterator.next();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                try {
                    Task task = extractTaskFromRow(row);
                    if (task != null) {
                        tasks.add(task);
                        nextId = Math.max(nextId, task.getId() + 1);
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error parsing row {0}: {1}",
                            new Object[]{row.getRowNum(), e.getMessage()});
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading Excel file: " + e.getMessage(), e);
        }
    }

    private Task extractTaskFromRow(Row row) {
        try {
            int id = (int) row.getCell(0).getNumericCellValue();
            String name = getCellStringValue(row.getCell(1));
            String description = getCellStringValue(row.getCell(2));

            LocalDate dueDate = null;
            Cell dueDateCell = row.getCell(3);
            if (dueDateCell != null) {
                if (dueDateCell.getCellType() == CellType.NUMERIC) {
                    dueDate = dueDateCell.getLocalDateTimeCellValue().toLocalDate();
                }
            }

            int priority = (int) row.getCell(4).getNumericCellValue();
            String assignedTo = getCellStringValue(row.getCell(5));
            TaskStatus status = TaskStatus.valueOf(getCellStringValue(row.getCell(6)));

            LocalDate lastCompleted = null;
            Cell lastCompletedCell = row.getCell(7);
            if (lastCompletedCell != null && lastCompletedCell.getCellType() == CellType.NUMERIC) {
                lastCompleted = lastCompletedCell.getLocalDateTimeCellValue().toLocalDate();
            }

            int frequencyDays = (int) row.getCell(8).getNumericCellValue();

            return new Task(id, name, description, dueDate, priority,
                    assignedTo, status, lastCompleted, frequencyDays);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error extracting task from row: " + e.getMessage(), e);
            return null;
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf(cell.getNumericCellValue());
        }
        return "";
    }

    private void saveToFile() {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(filePath)) {

            Sheet sheet = workbook.createSheet("Tasks");

            // Стиль для заголовков
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Заголовки
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Name", "Description", "Due Date", "Priority",
                    "Assigned To", "Status", "Last Completed", "Frequency Days"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Данные
            for (int i = 0; i < tasks.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Task task = tasks.get(i);

                row.createCell(0).setCellValue(task.getId());
                row.createCell(1).setCellValue(task.getName());
                row.createCell(2).setCellValue(task.getDescription());

                if (task.getDueDate() != null) {
                    row.createCell(3).setCellValue(task.getDueDate().toString());
                }

                row.createCell(4).setCellValue(task.getPriority());
                row.createCell(5).setCellValue(task.getAssignedTo());
                row.createCell(6).setCellValue(task.getStatus().name());

                if (task.getLastCompleted() != null) {
                    row.createCell(7).setCellValue(task.getLastCompleted().toString());
                }

                row.createCell(8).setCellValue(task.getFrequencyDays());
            }

            // Автоподбор ширины колонок
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(fos);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error saving to Excel file: " + e.getMessage(), e);
            throw new RuntimeException("Failed to save tasks to Excel file", e);
        }
    }

    @Override
    public ObservableList<Task> getAllTasks() throws SQLException {
        return FXCollections.unmodifiableObservableList(tasks);
    }

    @Override
    public ObservableList<Task> getFilteredTasks(String type, String status, String keyword) throws SQLException {
        return tasks.stream()
                .filter(task -> {
                    boolean matchesType = (type == null || type.isEmpty() || type.equals(task.getType()));
                    boolean matchesStatus = switch (status) {
                        case "Все" -> true;
                        case null -> true;
                        case "Активные" -> task.getStatus() == TaskStatus.ACTIVE;
                        case "Выполненные" -> task.getStatus() == TaskStatus.COMPLETED;
                        case "Просроченные" -> task.getStatus() == TaskStatus.ACTIVE
                                && task.getDueDate() != null
                                && task.getDueDate().isBefore(LocalDate.now());
                        default -> false;
                    };
                    boolean matchesKeyword = (keyword == null || keyword.isBlank() ||
                            task.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                            task.getDescription().toLowerCase().contains(keyword.toLowerCase()));
                    return matchesType && matchesStatus && matchesKeyword;
                })
                .collect(FXCollections::observableArrayList, ObservableList::add, ObservableList::addAll);
    }

    @Override
    public Task getTaskById(int id) throws SQLException {
        return tasks.stream()
                .filter(task -> task.getId() == id)
                .findFirst()
                .orElseThrow(() -> new SQLException("Task not found with id: " + id));
    }

    @Override
    public void addTask(Task task) throws SQLException {
        if (task == null) {
            throw new SQLException("Task cannot be null");
        }

        if (tasks.stream().anyMatch(t -> t.getId() == task.getId())) {
            throw new SQLException("Task with id " + task.getId() + " already exists");
        }

        task.setId(nextId++);
        tasks.add(task);
        saveToFile();
    }

    @Override
    public void updateTask(Task task) throws SQLException {
        if (task == null) {
            throw new SQLException("Task cannot be null");
        }

        Task existing = getTaskById(task.getId());
        existing.setName(task.getName());
        existing.setDescription(task.getDescription());
        existing.setDueDate(task.getDueDate());
        existing.setPriority(task.getPriority());
        existing.setAssignedTo(task.getAssignedTo());
        existing.setStatus(task.getStatus()); // Просто устанавливаем новый статус
        existing.setLastCompleted(task.getLastCompleted());
        existing.setFrequencyDays(task.getFrequencyDays());

        saveToFile();
    }

    @Override
    public void deleteTask(int id) throws SQLException {
        if (!tasks.removeIf(task -> task.getId() == id)) {
            throw new SQLException("Task not found with id: " + id);
        }
        saveToFile();
    }

    @Override
    public ObservableList<Task> getTasksByAssignee(String assignee) throws SQLException {
        if (assignee == null || assignee.trim().isEmpty()) {
            throw new SQLException("Assignee cannot be empty");
        }

        return tasks.stream()
                .filter(task -> assignee.equals(task.getAssignedTo()))
                .collect(FXCollections::observableArrayList,
                        ObservableList::add,
                        ObservableList::addAll);
    }

    @Override
    public ObservableList<Task> getTasksDueBetween(LocalDate start, LocalDate end) throws SQLException {
        if (start == null || end == null) {
            throw new SQLException("Start and end dates cannot be null");
        }

        if (start.isAfter(end)) {
            throw new SQLException("Start date cannot be after end date");
        }

        return tasks.stream()
                .filter(task -> task.getDueDate() != null &&
                        !task.getDueDate().isBefore(start) &&
                        !task.getDueDate().isAfter(end))
                .collect(FXCollections::observableArrayList,
                        ObservableList::add,
                        ObservableList::addAll);
    }

    @Override
    public void updateTaskStatus(int id, TaskStatus status) throws SQLException {
        Task task = getTaskById(id);
        task.setStatus(status);

        if (status == TaskStatus.COMPLETED) {
            task.setLastCompleted(LocalDate.now());
        }

        saveToFile();
    }

    @Override
    public void markTaskAsCompleted(int id) throws SQLException {
        Task task = getTaskById(id);
        task.setStatus(TaskStatus.COMPLETED);
        task.setLastCompleted(LocalDate.now());
        saveToFile();
    }

    @Override
    public void close() throws SQLException {
        // Для совместимости с интерфейсом, в Excel DAO не требуется закрытие ресурсов
    }
}