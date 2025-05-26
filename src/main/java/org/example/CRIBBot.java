package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

public class CRIBBot extends TelegramLongPollingBot {
    // Состояния диалога
    private enum State {
        START,
        WEIGHT,
        GESTATION,
        CONGENITAL,
        BASE_EXCESS,
        MIN_FIO2,
        MAX_FIO2,
        FINISH
    }

    private State currentState = State.START;
    private CRIBData cribData = new CRIBData();
    private Long currentChatId = null;

    @Override
    public String getBotUsername() {
        return BotConfig.BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BotConfig.BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            currentChatId = chatId;

            BotLogger.logInfo(String.format("[Chat %d] Received: %s", chatId, messageText));

            try {
                handleMessage(chatId, messageText);
            } catch (TelegramApiException e) {
                BotLogger.logError(String.format("[Chat %d] Message handling error", chatId), e);
                sendErrorMessage(chatId, "Произошла ошибка при обработке вашего сообщения");
            } catch (Exception e) {
                BotLogger.logError(String.format("[Chat %d] Unexpected error", chatId), e);
                sendErrorMessage(chatId, "Внутренняя ошибка бота");
            }
        }
    }

    private void handleMessage(long chatId, String messageText) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.enableMarkdown(true);

        // Справочная информация
        String infoMessage = buildInfoMessage();

        // Обработка команд
        if (messageText.equals("/start") || messageText.equals("/help") || messageText.equals("/info")) {
            message.setText(infoMessage);
            execute(message);
            BotLogger.logInfo(String.format("[Chat %d] Showed info", chatId));
            return;
        }

        // Начало расчета
        if (messageText.equals("/begin") && currentState == State.START) {
            startNewCalculation(chatId);
            return;
        }

        // Обработка состояний
        try {
            switch (currentState) {
                case START:
                    handleStartState(message, messageText);
                    break;
                case WEIGHT:
                    handleWeightState(message, messageText);
                    break;
                case GESTATION:
                    handleGestationState(message, messageText);
                    break;
                case CONGENITAL:
                    handleCongenitalState(message, messageText);
                    break;
                case BASE_EXCESS:
                    handleBaseExcessState(message, messageText);
                    break;
                case MIN_FIO2:
                    handleMinFio2State(message, messageText);
                    break;
                case MAX_FIO2:
                    handleMaxFio2State(message, messageText);
                    break;
                case FINISH:
                    handleFinishState(message, messageText);
                    break;
            }
        } catch (NumberFormatException e) {
            message.setText("Пожалуйста, введите корректное число.");
            BotLogger.logWarning(String.format("[Chat %d] Invalid number input", chatId));
        }

        execute(message);
    }

    private String buildInfoMessage() {
        return "👶 *Бот для расчета шкалы CRIB*\n\n" +
                "Этот бот помогает оценить тяжесть состояния новорожденных по шкале CRIB\n\n" +
                "*Учитываемые параметры:*\n" +
                "- Вес при рождении\n- Срок гестации\n- Врожденные пороки\n" +
                "- Кислотно-щелочное состояние\n- Потребность в кислороде\n\n" +
                "*Как использовать:*\n" +
                "1. Нажмите /info - показать эту справку\n" +
                "2. Нажмите /begin - начать расчет\n" +
                "3. Вводите запрашиваемые параметры\n" +
                "4. Получите результат\n\n" +
                "Для начала расчета нажмите /begin";
    }

    private void startNewCalculation(long chatId) throws TelegramApiException {
        cribData = new CRIBData();
        currentState = State.WEIGHT;
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Начинаем расчет CRIB!\n\nВведите вес ребенка при рождении (в граммах):");
        execute(message);
        BotLogger.logInfo(String.format("[Chat %d] Started new calculation", chatId));
    }

    private void handleStartState(SendMessage message, String messageText) {
        if (messageText.equals("/begin")) {
            message.setText("Начинаем расчет CRIB!\n\nВведите вес ребенка при рождении (в граммах):");
            currentState = State.WEIGHT;
        }
    }

    private void handleWeightState(SendMessage message, String messageText) {
        int weight = Integer.parseInt(messageText);
        if (weight <= 0) {
            message.setText("Вес должен быть положительным числом. Попробуйте еще раз:");
            return;
        }
        cribData.setWeight(weight);
        message.setText("Введите срок гестации (в неделях):");
        currentState = State.GESTATION;
        BotLogger.logInfo(String.format("[Chat %d] Weight entered: %d", currentChatId, weight));
    }

    private void handleGestationState(SendMessage message, String messageText) {
        int gestation = Integer.parseInt(messageText);
        if (gestation <= 0) {
            message.setText("Срок гестации должен быть положительным числом. Попробуйте еще раз:");
            return;
        }
        cribData.setGestation(gestation);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("нет");
        row.add("не опасные");
        row.add("опасные");
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        message.setReplyMarkup(keyboardMarkup);
        message.setText("Есть ли врожденные пороки?");
        currentState = State.CONGENITAL;
        BotLogger.logInfo(String.format("[Chat %d] Gestation entered: %d", currentChatId, gestation));
    }

    private void handleCongenitalState(SendMessage message, String messageText) {
        if (messageText.equalsIgnoreCase("нет") ||
                messageText.equalsIgnoreCase("не опасные") ||
                messageText.equalsIgnoreCase("опасные")) {

            cribData.setCongenital(messageText.toLowerCase());
            message.setText("Введите максимальный избыток оснований (ВЕ, ммоль/л):");
            message.setReplyMarkup(null);
            currentState = State.BASE_EXCESS;
            BotLogger.logInfo(String.format("[Chat %d] Congenital defects: %s", currentChatId, messageText));
        } else {
            message.setText("Пожалуйста, выберите один из вариантов:");
        }
    }

    private void handleBaseExcessState(SendMessage message, String messageText) {
        double be = Double.parseDouble(messageText);
        cribData.setBaseExcess(be);
        message.setText("Введите минимальный FiO2 (например, 0.21 для воздуха):");
        currentState = State.MIN_FIO2;
        BotLogger.logInfo(String.format("[Chat %d] Base excess: %.2f", currentChatId, be));
    }

    private void handleMinFio2State(SendMessage message, String messageText) {
        double minFio2 = Double.parseDouble(messageText);
        if (minFio2 < 0 || minFio2 > 1) {
            message.setText("FiO2 должен быть между 0 и 1. Попробуйте еще раз:");
            return;
        }
        cribData.setMinFio2(minFio2);
        message.setText("Введите максимальный FiO2:");
        currentState = State.MAX_FIO2;
        BotLogger.logInfo(String.format("[Chat %d] Min FiO2: %.2f", currentChatId, minFio2));
    }

    private void handleMaxFio2State(SendMessage message, String messageText) {
        double maxFio2 = Double.parseDouble(messageText);
        if (maxFio2 < 0 || maxFio2 > 1) {
            message.setText("FiO2 должен быть между 0 и 1. Попробуйте еще раз:");
            return;
        }
        cribData.setMaxFio2(maxFio2);

        int totalScore = calculateCRIBScore();
        String interpretation = interpretCRIBScore(totalScore);

        message.setText("*Результаты расчета шкалы CRIB:*\n\n" +
                "🔹 *Общий балл:* " + totalScore + "\n" +
                "🔹 " + interpretation + "\n\n" +
                "Для нового расчета введите /begin");
        currentState = State.FINISH;
        BotLogger.logInfo(String.format("[Chat %d] Calculation finished. Score: %d", currentChatId, totalScore));
    }

    private void handleFinishState(SendMessage message, String messageText) {
        if (messageText.equals("/begin")) {
            cribData = new CRIBData();
            message.setText("Введите вес ребенка при рождении (в граммах):");
            currentState = State.WEIGHT;
        } else {
            message.setText(buildInfoMessage());
        }
    }

    private void sendErrorMessage(long chatId, String errorText) {
        try {
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId);
            errorMessage.setText("⚠️ " + errorText);
            execute(errorMessage);
        } catch (TelegramApiException e) {
            BotLogger.logError("Failed to send error message", e);
        }
    }

    private int calculateCRIBScore() {
        int score = 0;

        // Вес при рождении
        if (cribData.getWeight() > 1350) score += 0;
        else if (cribData.getWeight() >= 851) score += 1;
        else if (cribData.getWeight() >= 701) score += 4;
        else score += 7;

        // Срок гестации
        score += (cribData.getGestation() > 24) ? 0 : 1;

        // Врожденные пороки
        switch (cribData.getCongenital()) {
            case "нет": score += 0; break;
            case "не опасные": score += 1; break;
            case "опасные": score += 3; break;
        }

        // Избыток оснований
        if (cribData.getBaseExcess() > -7.0) score += 0;
        else if (cribData.getBaseExcess() >= -10.0) score += 1;
        else if (cribData.getBaseExcess() >= -15.0) score += 2;
        else score += 3;

        // Минимальный FiO2
        if (cribData.getMinFio2() < 0.4) score += 0;
        else if (cribData.getMinFio2() <= 0.60) score += 2;
        else if (cribData.getMinFio2() <= 0.90) score += 3;
        else score += 4;

        // Максимальный FiO2
        if (cribData.getMaxFio2() < 0.4) score += 0;
        else if (cribData.getMaxFio2() <= 0.80) score += 1;
        else if (cribData.getMaxFio2() <= 0.90) score += 3;
        else score += 5;

        return score;
    }

    private String interpretCRIBScore(int score) {
        String mortality;
        String neuroDeficits;

        if (score <= 5) {
            mortality = "Летальность: 8%";
            neuroDeficits = "Наличие тяжелых психоневрологических дефицитов у выживших: 5%";
        }
        else if (score <= 10) {
            mortality = "Летальность: 38%";
            neuroDeficits = "Наличие тяжелых психоневрологических дефицитов у выживших: 12%";
        }
        else if (score <= 15) {
            mortality = "Летальность: 70-76%";
            neuroDeficits = "Наличие тяжелых психоневрологических дефицитов у выживших: 20%";
        }
        else {
            mortality = "Летальность: 85-90%";
            neuroDeficits = "Наличие тяжелых психоневрологических дефицитов у выживших: 20%";
        }

        return mortality + "\n" + neuroDeficits;
    }
}