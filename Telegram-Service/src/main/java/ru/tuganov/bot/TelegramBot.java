package ru.tuganov.bot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.tuganov.bot.callbacks.simple.GetInstrumentSimpleCallback;
import ru.tuganov.bot.handlers.CallbackHandler;
import ru.tuganov.bot.handlers.CommandHandler;
import ru.tuganov.bot.handlers.MessageHandler;
import ru.tuganov.bot.utils.Message;
import ru.tuganov.broker.senders.DatabaseSender;
import ru.tuganov.broker.senders.InvestmentSender;
import ru.tuganov.investment.AchievedInstrument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    @Value("${bot.name}")
    private String botName;
    @Getter
    @Value("${bot.token}")
    private String botToken;

    private final CommandHandler commandHandler;
    private final CallbackHandler callBackHandler;
    private final MessageHandler messageHandler;
    private final DatabaseSender databaseSender;
    private final InvestmentSender investmentSender;

    @NonFinal
    private final Map<Long, String> userContext = new HashMap<>();
    @NonFinal
    public final static List<AchievedInstrument> achievedInstruments = new ArrayList<>();

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            var message = update.getMessage();
            var chatId = message.getChatId();
            var context = userContext.get(chatId);
            if (context != null && !context.isBlank()) {
                sendMessage(messageHandler.handle(update, userContext));
            }
            if (message.getText().startsWith("/")) {
                sendMessage(commandHandler.handleCommands(update, userContext));
            }
        } else if (update.hasCallbackQuery()){
            if (update.getCallbackQuery().getData().startsWith("simpleGUS")) {
                SendPhoto photo;
//                log.info("not context {}", update.getCallbackQuery().getData());
                try {
                    photo = new GetInstrumentSimpleCallback(databaseSender, investmentSender).handle(update);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (photo != null) {
                    sendPhoto(photo);
                } else {
                    sendMessage(new SendMessage(
                            String.valueOf(update.getCallbackQuery().getMessage().getChatId()),
                            Message.deletedError));
                }
            } else {
                try {
                    sendMessage(callBackHandler.handleCallBack(update, userContext));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    public void sendPhoto(SendPhoto sendPhoto) {
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public void sendMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
}
