package org.example.hometracker_kurs.dao;

import org.example.hometracker_kurs.config.DatabaseConfig;
import org.example.hometracker_kurs.config.ExcelConfig;

import java.sql.SQLException;

/**
 * Фабрика для создания DAO (Data Access Object) объектов задач.
 * Поддерживает различные типы хранилищ данных.
 */
public class TaskDAOFactory {

    /**
     * Создает и возвращает DAO объект для работы с задачами в зависимости от типа хранилища.
     *
     * @param type         тип хранилища данных ("postgres", "excel", "h2")
     * @param dbConfig     конфигурация для PostgreSQL или H2
     * @param excelConfig  конфигурация для Excel
     * @return реализацию TaskDAO
     * @throws SQLException если возникает ошибка подключения к БД
     */
    public static TaskDAO createTaskDAO(String type, DatabaseConfig dbConfig, ExcelConfig excelConfig) throws SQLException {
        switch (type.toLowerCase()) {
            case "postgres":
                return new PostgresTaskDAO(dbConfig);
            case "excel":
                return new ExcelTaskDAO(excelConfig);
            case "h2":
                return new H2TaskDAO(dbConfig);
            default:
                throw new IllegalArgumentException("Неизвестный тип DAO: " + type);
        }
    }
}
