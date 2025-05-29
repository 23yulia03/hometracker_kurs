package org.example.hometracker_kurs.telegram;

import org.example.hometracker_kurs.model.Config;
import org.example.hometracker_kurs.model.Task;
import org.example.hometracker_kurs.model.TaskStatus;
import org.example.hometracker_kurs.service.TaskService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TelegramReminderBot extends TelegramLongPollingBot {
    private final TaskService taskService;
    private final String token;
    private String chatId;

    public TelegramReminderBot(Config config, TaskService taskService) {
        this.token = config.getTelegramBotToken();
        this.chatId = config.getTelegramChatId(); // можно оставить null — будет обновляться
        this.taskService = taskService;
    }

    @Override
    public String getBotUsername() {
        return "MyFamTaskBot";
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            this.chatId = update.getMessage().getChatId().toString();
            String message = update.getMessage().getText().trim().toLowerCase();

            switch (message) {
                case "/start" -> sendWelcomeMessage();
                case "/напомни" -> sendDailyTasks();
                default -> sendMessage("❗ Неизвестная команда. Нажми /start для помощи.");
            }

        } else if (update.hasCallbackQuery()) {
            this.chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            String callback = update.getCallbackQuery().getData();

            if ("/напомни".equals(callback)) {
                sendDailyTasks();
            }
        }
    }

    private void sendWelcomeMessage() {
        String text = """
                👋 *Привет! Я бот для напоминаний о домашних делах.*
                
                Вот что я умею:
                ✅ `/напомни` — показать задачи на сегодня
                ⏰ Я сам напомню о задачах каждый день в *8:00*

                Просто нажми кнопку внизу или введи команду.
                """;

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.enableMarkdown(true);

        // Кнопки под сообщением
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton remindBtn = new InlineKeyboardButton();
        remindBtn.setText("🔔 Напомни задачи на сегодня");
        remindBtn.setCallbackData("/напомни");

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(remindBtn);
        rows.add(row);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendDailyTasks() {
        try {
            List<Task> todayTasks = taskService.getAllTasks().stream()
                    .filter(task -> task.getDueDate() != null
                            && task.getDueDate().isEqual(LocalDate.now())
                            && task.getStatus() == TaskStatus.ACTIVE)
                    .collect(Collectors.toList());

            if (todayTasks.isEmpty()) {
                sendMessage("✅ На сегодня нет активных задач.");
                return;
            }

            StringBuilder sb = new StringBuilder("📅 *Задачи на сегодня:*\n\n");

            for (Task task : todayTasks) {
                sb.append("🔹 *").append(task.getName()).append("*\n")
                        .append("📌 ").append(task.getDescription()).append("\n")
                        .append("👤 ").append(task.getAssignedTo()).append("\n")
                        .append("🏷️ ").append(task.getType()).append("\n")
                        .append("📅 ").append(task.getDueDate()).append("\n\n");
            }

            sendMessage(sb.toString());

        } catch (Exception e) {
            sendMessage("❗ Ошибка при получении задач: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendMessage(String text) {
        if (chatId == null) {
            System.err.println("❗ chatId is null. Cannot send message.");
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.enableMarkdown(true);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
