package org.example.hometracker_kurs.telegram;

import java.time.*;
import java.util.concurrent.*;

public class Scheduler {
    public static void scheduleDailyReminder(Runnable task) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        long initialDelay = computeInitialDelay(8); // 8:00
        long period = TimeUnit.DAYS.toMillis(1);
        scheduler.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.MILLISECONDS);
    }

    private static long computeInitialDelay(int hour) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(hour).withMinute(0).withSecond(0).withNano(0);
        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }
        return Duration.between(now, nextRun).toMillis();
    }
}
