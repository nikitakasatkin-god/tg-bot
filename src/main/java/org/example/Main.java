package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        BotLogger.logInfo("Запуск бота CRIB...");

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new CRIBBot());
            BotLogger.logInfo("Бот успешно запущен и готов к работе!");
        } catch (TelegramApiException e) {
            BotLogger.logError("Ошибка при запуске бота", e);
        }
    }
}