package org.example.hometracker_kurs.service;

import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.model.TaskStatus;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskServiceUnitTest {

    private static TaskService service;
    private static Task task;

    @BeforeAll
    static void init() {
        service = new TaskService("postgres"); // Используем postgres DAO для тестов
        task = new Task(0, "Тестовая задача", "Описание",
                LocalDate.now().plusDays(1), 3,
                "Мама", TaskStatus.ACTIVE, null);
        task.setType("Уборка");
    }

    @Test
    @Order(1)
    void testAddTask() throws SQLException {
        service.addTask(task);
        assertTrue(task.getId() > 0, "ID должен быть присвоен после добавления");
    }

    @Test
    @Order(2)
    void testGetAllTasks() throws SQLException {
        List<Task> tasks = service.getAllTasks();
        assertFalse(tasks.isEmpty(), "Список задач должен содержать хотя бы одну задачу");
    }

    @Test
    @Order(3)
    void testUpdateTask() throws SQLException {
        task.setDescription("Новое описание");
        service.updateTask(task);

        Task updated = service.getAllTasks().stream()
                .filter(t -> t.getId() == task.getId()).findFirst().orElseThrow();
        assertEquals("Новое описание", updated.getDescription());
    }

    @Test
    @Order(4)
    void testCompleteTask() throws SQLException {
        service.completeTask(task.getId());

        Task updated = service.getAllTasks().stream()
                .filter(t -> t.getId() == task.getId()).findFirst().orElseThrow();
        assertEquals(TaskStatus.COMPLETED, updated.getStatus());
    }

    @Test
    @Order(5)
    void testPostponeTask() throws SQLException {
        service.reactivateTask(task.getId());
        service.postponeTask(task.getId(), 3);

        Task postponed = service.getAllTasks().stream()
                .filter(t -> t.getId() == task.getId()).findFirst().orElseThrow();
        assertEquals(TaskStatus.POSTPONED, postponed.getStatus());
        assertEquals(LocalDate.now().plusDays(4), postponed.getDueDate());
    }

    @Test
    @Order(6)
    void testDeleteTask() throws SQLException {
        service.deleteTask(task.getId());

        boolean exists = service.getAllTasks().stream()
                .anyMatch(t -> t.getId() == task.getId());
        assertFalse(exists, "Задача должна быть удалена");
    }

    @AfterAll
    static void cleanup() throws SQLException {
        service.close();
    }
}
