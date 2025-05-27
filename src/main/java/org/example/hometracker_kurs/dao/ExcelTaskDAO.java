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
import java.time.format.DateTimeParseException;
import java.util.Comparator;
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

            if (rowIterator.hasNext()) rowIterator.next(); // Skip header

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

    @Override
    public ObservableList<Task> getFilteredTasks(
            String type,
            String status,
            String keyword,
            String sortField,
            boolean ascending) throws SQLException {

        Comparator<Task> comparator = getComparator(sortField, ascending);

        return tasks.stream()
                .filter(task -> {
                    boolean matchesType = (type == null || type.isEmpty() || type.equals(task.getType()));
                    boolean matchesStatus = switch (status) {
                        case "Все" -> true;
                        case null -> true;
                        case "Активные" -> task.getStatus() == TaskStatus.ACTIVE;
                        case "Выполненные" -> task.getStatus() == TaskStatus.COMPLETED;
                        case "Просроченные" -> task.getStatus() == TaskStatus.OVERDUE;
                        default -> false;
                    };
                    boolean matchesKeyword = (keyword == null || keyword.isBlank() ||
                            task.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                            task.getDescription().toLowerCase().contains(keyword.toLowerCase()));
                    return matchesType && matchesStatus && matchesKeyword;
                })
                .sorted(comparator)
                .collect(FXCollections::observableArrayList, ObservableList::add, ObservableList::addAll);
    }

    private Comparator<Task> getComparator(String sortField, boolean ascending) {
        Comparator<Task> comparator;

        switch (sortField != null ? sortField : "") {
            case "due_date":
                comparator = Comparator.comparing(
                        Task::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                );
                break;

            case "priority":
                comparator = Comparator.comparingInt(Task::getPriority);
                break;

            case "assigned_to":
                comparator = Comparator.comparing(
                        Task::getAssignedTo,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                );
                break;

            default:
                comparator = Comparator.comparing(
                        Task::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).thenComparingInt(Task::getPriority);
        }

        return ascending ? comparator : comparator.reversed();
    }

    private Task extractTaskFromRow(Row row) {
        try {
            int id = getCellIntValue(row.getCell(0));
            String name = getCellStringValue(row.getCell(1));
            String description = getCellStringValue(row.getCell(2));

            LocalDate dueDate = getCellDateValue(row.getCell(3));
            int priority = getCellIntValue(row.getCell(4));
            String assignedTo = getCellStringValue(row.getCell(5));
            TaskStatus status = TaskStatus.valueOf(getCellStringValue(row.getCell(6)));
            LocalDate lastCompleted = getCellDateValue(row.getCell(7));
            String type = getCellStringValue(row.getCell(8));

            Task task = new Task(id, name, description, dueDate, priority,
                    assignedTo, status, lastCompleted);
            task.setType(type);
            return task;
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
            return String.valueOf((int) cell.getNumericCellValue());
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        return "";
    }

    private int getCellIntValue(Cell cell) {
        if (cell == null) return 0;
        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return 0;
                }
            default:
                return 0;
        }
    }

    private LocalDate getCellDateValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return LocalDate.parse(cell.getStringCellValue().trim());
            } catch (DateTimeParseException e) {
                return null;
            }
        }
        return null;
    }

    private void saveToFile() {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(filePath)) {

            Sheet sheet = workbook.createSheet("Tasks");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Name", "Description", "Due Date", "Priority",
                    "Assigned To", "Status", "Last Completed", "Type"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

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

                row.createCell(8).setCellValue(task.getType());
            }

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
        return tasks;
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
        validateTask(task);

        if (tasks.stream().anyMatch(t -> t.getId() == task.getId())) {
            throw new SQLException("Task with id " + task.getId() + " already exists");
        }

        task.setId(nextId++);
        tasks.add(task);
        saveToFile();
    }

    private void validateTask(Task task) throws SQLException {
        if (task == null) {
            throw new SQLException("Task cannot be null");
        }
        if (task.getName() == null || task.getName().trim().isEmpty()) {
            throw new SQLException("Task name cannot be empty");
        }
        if (task.getStatus() == null) {
            throw new SQLException("Task status cannot be null");
        }
        if (task.getPriority() < 1 || task.getPriority() > 5) {
            throw new SQLException("Priority must be between 1 and 5");
        }
        if (task.getType() == null || task.getType().trim().isEmpty()) {
            throw new SQLException("Task type cannot be empty");
        }
    }

    @Override
    public void updateTask(Task task) throws SQLException {
        validateTask(task);

        Task existing = getTaskById(task.getId());
        existing.setName(task.getName());
        existing.setDescription(task.getDescription());
        existing.setDueDate(task.getDueDate());
        existing.setPriority(task.getPriority());
        existing.setAssignedTo(task.getAssignedTo());
        existing.setStatus(task.getStatus());
        existing.setLastCompleted(task.getLastCompleted());
        existing.setType(task.getType());

        int index = tasks.indexOf(task);
        if (index >= 0) {
            tasks.set(index, task); // это заставит UI обновить строку
        }

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
    public void updateTaskStatus(int id, TaskStatus status) throws SQLException {
        Task task = getTaskById(id);
        task.setStatus(status);

        if (status == TaskStatus.COMPLETED) {
            task.setLastCompleted(LocalDate.now());
        }

        saveToFile();

        // Явно обновляем данные в списке
        int index = tasks.indexOf(task);
        if (index >= 0) {
            tasks.set(index, task);
        }
    }

    @Override
    public void markTaskAsCompleted(int id) throws SQLException {
        updateTaskStatus(id, TaskStatus.COMPLETED);
    }

    @Override
    public void postponeTask(Task task, int days) throws SQLException {
        if (task.getStatus() != TaskStatus.ACTIVE && task.getStatus() != TaskStatus.OVERDUE) {
            throw new SQLException("Можно откладывать только активные или просроченные задачи");
        }

        task.setStatus(TaskStatus.POSTPONED);
        task.setDueDate(task.getDueDate().plusDays(days));

        // Явное обновление в списке
        int index = tasks.indexOf(task);
        if (index >= 0) {
            tasks.set(index, task);
        }

        saveToFile();
    }

    @Override
    public void close() throws SQLException {
        // No resources to close for Excel
    }
}