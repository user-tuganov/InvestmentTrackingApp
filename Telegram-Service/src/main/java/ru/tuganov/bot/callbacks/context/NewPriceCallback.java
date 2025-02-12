package ru.tuganov.bot.callbacks.context;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.tuganov.bot.utils.Message;
import ru.tuganov.bot.utils.Metrics;

import java.util.Map;

@Component
public class NewPriceCallback implements ContextCallbackHandler {

    @Override
    public SendMessage handle(Update update, Map<Long, String> userContext) {
        var callBack = update.getCallbackQuery();
        var chatId = callBack.getMessage().getChatId();
        var data = callBack.getData();
        var figi = data.substring(Metrics.simpleCallBackLength);

        var sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(Message.setPrice);
        userContext.put(chatId, "saveInstrument" + figi);
        return sendMessage;
    }
}
