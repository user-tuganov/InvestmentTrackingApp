package ru.tuganov.bot.callbacks.context;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.tuganov.bot.utils.Message;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AddInstrumentCallback implements ContextCallbackHandler {
    @Override
    public SendMessage handle(Update update, Map<Long, String> userContext) {
        var callBack = update.getCallbackQuery();
        var chatId = callBack.getMessage().getChatId();
        userContext.put(chatId, "getInstruments");
        return new SendMessage(String.valueOf(chatId), Message.inputInstrument);
    }
}
