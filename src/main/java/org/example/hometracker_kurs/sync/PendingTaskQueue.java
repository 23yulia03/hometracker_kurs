package org.example.hometracker_kurs.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.hometracker_kurs.model.Task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PendingTaskQueue {
    private static final File queueFile = new File("pending_tasks.json");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static synchronized void enqueue(Task task, String operation) {
        List<QueuedTaskOperation> queue = loadQueue();
        queue.add(new QueuedTaskOperation(operation, task));
        saveQueue(queue);
    }

    public static synchronized List<QueuedTaskOperation> loadQueue() {
        if (!queueFile.exists()) return new ArrayList<>();
        try {
            return objectMapper.readValue(queueFile, new TypeReference<>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static synchronized void clearQueue() {
        queueFile.delete();
    }

    private static synchronized void saveQueue(List<QueuedTaskOperation> queue) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(queueFile, queue);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
