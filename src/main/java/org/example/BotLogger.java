package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BotLogger {
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = "crib_bot.log";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        try {
            // Создаем директорию для логов, если ее нет
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectory(logDir);
            }
        } catch (IOException e) {
            System.err.println("Не удалось создать директорию для логов: " + e.getMessage());
        }
    }

    public static void logInfo(String message) {
        log("INFO", message);
    }

    public static void logWarning(String message) {
        log("WARNING", message);
    }

    public static void logError(String message) {
        log("ERROR", message);
    }

    public static void logError(String message, Throwable throwable) {
        log("ERROR", message + "\n" + getStackTrace(throwable));
    }

    private static void log(String level, String message) {
        String logEntry = String.format("[%s] [%s] %s%n",
                LocalDateTime.now().format(DATE_FORMAT),
                level,
                message);

        // Вывод в консоль
        System.out.print(logEntry);

        // Запись в файл
        try {
            Path logFile = Paths.get(LOG_DIR, LOG_FILE);
            Files.write(logFile, logEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Ошибка при записи в лог-файл: " + e.getMessage());
        }
    }

    private static String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\t").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}