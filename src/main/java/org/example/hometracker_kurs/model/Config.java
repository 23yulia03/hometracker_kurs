package org.example.hometracker_kurs.model;

public class Config {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final String excelPath;
    private final String h2Url;

    public Config() {
        this(
                System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/household_tracker"),
                System.getenv().getOrDefault("DB_USER", "postgres"),
                System.getenv().getOrDefault("DB_PASSWORD", "123123123"),
                System.getenv().getOrDefault("EXCEL_PATH", "household_tasks.xlsx"),
                System.getenv().getOrDefault("H2_URL", "jdbc:h2:mem:household_tracker")
        );
    }

    public Config(String dbUrl, String dbUser, String dbPassword, String excelPath, String h2Url) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.excelPath = excelPath;
        this.h2Url = h2Url;
    }

    // Геттеры
    public String getDbUrl() { return dbUrl; }
    public String getDbUser() { return dbUser; }
    public String getDbPassword() { return dbPassword; }
    public String getExcelPath() { return excelPath; }
    public String getH2Url() { return h2Url; }
}