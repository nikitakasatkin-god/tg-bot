package org.example;

import io.github.cdimascio.dotenv.Dotenv;

public class BotConfig {
    private static final Dotenv dotenv = Dotenv.load();

    public static final String BOT_TOKEN;
    public static final String BOT_USERNAME;

    static {
        BOT_TOKEN = dotenv.get("BOT_TOKEN");
        BOT_USERNAME = dotenv.get("BOT_USERNAME");

        if (BOT_TOKEN == null || BOT_USERNAME == null) {
            System.err.println("\nОШИБКА: Не найдены необходимые переменные окружения!");
            System.err.println("Пожалуйста, создайте файл .env в корне проекта со следующим содержимым:");
            System.err.println("BOT_TOKEN=ваш_токен_бота");
            System.err.println("BOT_USERNAME=имя_бота_без_@");
            System.err.println("\nКак получить токен бота:");
            System.err.println("1. Создайте бота через @BotFather в Telegram");
            System.err.println("2. Скопируйте выданный токен");
            System.exit(1);
        }
    }
}