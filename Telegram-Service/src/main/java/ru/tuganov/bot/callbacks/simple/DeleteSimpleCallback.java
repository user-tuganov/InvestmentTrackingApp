package ru.tuganov.bot.callbacks.simple;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.tuganov.bot.utils.Message;
import ru.tuganov.broker.senders.DatabaseSender;

import static java.lang.Long.parseLong;

@Component
@RequiredArgsConstructor
public class DeleteSimpleCallback implements SimpleCallback<SendMessage> {
    private final DatabaseSender databaseSender;

    @Override
    public SendMessage handle(Update update) {
        var callBack = update.getCallbackQuery();
        var instrument = callBack.getData();
        var instrumentId = instrument.substring("simpleDICi".length());
        var isDeleted = databaseSender.deleteInstrument(parseLong(instrumentId));
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(callBack.getMessage().getChatId()));
        if (isDeleted) {
            sendMessage.setText(Message.instrumentDeleted);
        } else {
            sendMessage.setText(Message.deletedError);
        }
        return sendMessage;
    }
}
