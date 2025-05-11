package org.example.hometracker_kurs.dao;

import org.example.hometracker_kurs.model.Config;

import java.sql.SQLException;

public class TaskDAOFactory {
    public static TaskDAO createTaskDAO(String type, Config config) throws SQLException, SQLException {
        switch (type.toLowerCase()) {
            case "postgres":
                return new PostgresTaskDAO(config);
            case "excel":
                return new ExcelTaskDAO(config);
            case "h2":
                return new H2TaskDAO(config);
            default:
                throw new IllegalArgumentException("Unknown DAO type: " + type);
        }
    }
}