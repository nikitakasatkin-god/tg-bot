package org.example;

import io.github.cdimascio.dotenv.Dotenv;

public class BotConfig {
    private static final Dotenv dotenv = Dotenv.load();

    public static final String BOT_TOKEN = dotenv.get("BOT_TOKEN");
    public static final String BOT_USERNAME = dotenv.get("BOT_USERNAME");
}