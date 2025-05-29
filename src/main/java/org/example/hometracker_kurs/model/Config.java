package org.example.hometracker_kurs.model;

public class Config {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final String excelPath;
    private final String h2User;
    private final String h2Password;
    private final String h2Url;
    private final String telegramBotToken;
    private final String telegramChatId;

    public Config() {
        this(
                System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/household_tracker"),
                System.getenv().getOrDefault("DB_USER", "postgres"),
                System.getenv().getOrDefault("DB_PASSWORD", "123123123"),
                System.getenv().getOrDefault("EXCEL_PATH", "household_tasks.xlsx"),
                System.getenv().getOrDefault("H2_USER", "sa"),
                System.getenv().getOrDefault("H2_PASSWORD", "123123123"),
                System.getenv().getOrDefault("H2_URL", "jdbc:h2:tcp://localhost/~/test"),
                System.getenv().getOrDefault("TELEGRAM_BOT_TOKEN", "7655027951:AAH4TIY89jH7fwvCEXJSW7G2oOwd03MIogI"),
                System.getenv().getOrDefault("TELEGRAM_CHAT_ID", "824371933")
        );
    }

    public Config(String dbUrl, String dbUser, String dbPassword,
                  String excelPath, String h2User, String h2Password, String h2Url, String telegramBotToken, String telegramChatId) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.excelPath = excelPath;
        this.h2User = h2User;
        this.h2Password = h2Password;
        this.h2Url = h2Url;
        this.telegramBotToken = telegramBotToken;
        this.telegramChatId = telegramChatId;
    }

    public String getDbUrl() { return dbUrl; }
    public String getDbUser() { return dbUser; }
    public String getDbPassword() { return dbPassword; }
    public String getExcelPath() { return excelPath; }
    public String getH2User() { return h2User; }
    public String getH2Password() { return h2Password; }
    public String getH2Url() { return h2Url; }
    public String getTelegramBotToken() { return telegramBotToken; }
    public String getTelegramChatId() { return telegramChatId; }
}