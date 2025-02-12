package ru.tuganov.bot.callbacks.context;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;

public interface ContextCallbackHandler {
    SendMessage handle(Update update, Map<Long, String> userContext);
}
