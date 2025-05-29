package org.example.hometracker_kurs.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.hometracker_kurs.telegram.Scheduler;
import org.example.hometracker_kurs.model.Config;
import org.example.hometracker_kurs.service.TaskService;
import org.example.hometracker_kurs.telegram.TelegramReminderBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class HouseholdTrackerApp extends Application {
    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main-view.fxml"));
            loader.setControllerFactory(controllerClass -> {
                try {
                    return controllerClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Config config = new Config();
            TaskService taskService = new TaskService("postgres"); // или "excel", "h2"
            TelegramReminderBot bot = new TelegramReminderBot(config, taskService);

            try {
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                botsApi.registerBot(bot);
                Scheduler.scheduleDailyReminder(bot::sendDailyTasks);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.setTitle("Трекер домашних дел");
            stage.setWidth(1450);
            stage.setHeight(820);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
