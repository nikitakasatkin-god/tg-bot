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

            try {
                handleMessage(chatId, messageText);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleMessage(long chatId, String messageText) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.enableMarkdown(true);

        // Справочная информация, которая показывается перед началом работы
        // Справочная информация
        String infoMessage = "👶 *Бот для расчета шкалы CRIB*\n\n" +
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

        // Обработка команд
        if (messageText.equals("/start") || messageText.equals("/info")) {
            message.setText(infoMessage);
            execute(message);
            return;
        }

        // Начало расчета
        if (messageText.equals("/begin") && currentState == State.START) {
            message.setText("Начинаем расчет CRIB!\n\nВведите вес ребенка при рождении (в граммах):");
            currentState = State.WEIGHT;
            execute(message);
            return;
        }

        switch (currentState) {
            case START:
                if (messageText.equals("/start")) {
                    message.setText(infoMessage + "\n\n*Готовы начать?*\nВведите вес ребенка при рождении (в граммах):");
                    currentState = State.WEIGHT;
                }
                break;

            case WEIGHT:
                try {
                    int weight = Integer.parseInt(messageText);
                    if (weight <= 0) {
                        message.setText("Вес должен быть положительным числом. Попробуйте еще раз:");
                        break;
                    }
                    cribData.setWeight(weight);
                    message.setText("Введите срок гестации (в неделях):");
                    currentState = State.GESTATION;
                } catch (NumberFormatException e) {
                    message.setText("Пожалуйста, введите число. Попробуйте еще раз:");
                }
                break;

            case GESTATION:
                try {
                    int gestation = Integer.parseInt(messageText);
                    if (gestation <= 0) {
                        message.setText("Срок гестации должен быть положительным числом. Попробуйте еще раз:");
                        break;
                    }
                    cribData.setGestation(gestation);

                    // Создаем клавиатуру для выбора врожденных пороков
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
                } catch (NumberFormatException e) {
                    message.setText("Пожалуйста, введите число. Попробуйте еще раз:");
                }
                break;

            case CONGENITAL:
                if (messageText.equalsIgnoreCase("нет") ||
                        messageText.equalsIgnoreCase("не опасные") ||
                        messageText.equalsIgnoreCase("опасные")) {

                    cribData.setCongenital(messageText.toLowerCase());
                    message.setText("Введите максимальный избыток оснований (ВЕ, ммоль/л):");
                    message.setReplyMarkup(null); // Убираем клавиатуру
                    currentState = State.BASE_EXCESS;
                } else {
                    message.setText("Пожалуйста, выберите один из вариантов:");
                }
                break;

            case BASE_EXCESS:
                try {
                    double be = Double.parseDouble(messageText);
                    cribData.setBaseExcess(be);
                    message.setText("Введите минимальный FiO2 (например, 0.21 для воздуха):");
                    currentState = State.MIN_FIO2;
                } catch (NumberFormatException e) {
                    message.setText("Пожалуйста, введите число. Попробуйте еще раз:");
                }
                break;

            case MIN_FIO2:
                try {
                    double minFio2 = Double.parseDouble(messageText);
                    if (minFio2 < 0 || minFio2 > 1) {
                        message.setText("FiO2 должен быть между 0 и 1. Попробуйте еще раз:");
                        break;
                    }
                    cribData.setMinFio2(minFio2);
                    message.setText("Введите максимальный FiO2:");
                    currentState = State.MAX_FIO2;
                } catch (NumberFormatException e) {
                    message.setText("Пожалуйста, введите число. Попробуйте еще раз:");
                }
                break;

            case MAX_FIO2:
                try {
                    double maxFio2 = Double.parseDouble(messageText);
                    if (maxFio2 < 0 || maxFio2 > 1) {
                        message.setText("FiO2 должен быть между 0 и 1. Попробуйте еще раз:");
                        break;
                    }
                    cribData.setMaxFio2(maxFio2);

                    // Расчет результатов
                    int totalScore = calculateCRIBScore();
                    String interpretation = interpretCRIBScore(totalScore);

                    message.setText("*Результаты расчета шкалы CRIB:*\n\n" +
                            "🔹 *Общий балл:* " + totalScore + "\n" +
                            "🔹 " + interpretation + "\n\n" +
                            "Для нового расчета введите /start");
                    currentState = State.FINISH;
                } catch (NumberFormatException e) {
                    message.setText("Пожалуйста, введите число. Попробуйте еще раз:");
                }
                break;

            case FINISH:
                if (messageText.equals("/start")) {
                    cribData = new CRIBData();
                    message.setText("Введите вес ребенка при рождении (в граммах):");
                    currentState = State.WEIGHT;
                } else {
                    message.setText(infoMessage);
                }
                break;
        }

        execute(message);
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