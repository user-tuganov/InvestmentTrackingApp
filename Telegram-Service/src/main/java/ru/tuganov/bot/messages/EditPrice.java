package ru.tuganov.bot.messages;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.tuganov.bot.utils.InstrumentsMarkup;
import ru.tuganov.bot.utils.Message;
import ru.tuganov.broker.senders.DatabaseSender;
import ru.tuganov.dto.InstrumentDBDto;
import ru.tuganov.dto.MarkupDataDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Long.parseLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class EditPrice implements MessageHandler {

    private final DatabaseSender databaseSender;
    public SendMessage handle(Update update, Map<Long, String> userContext) {

        var message = update.getMessage();
        var chatId = message.getChatId();
        var data = userContext.get(chatId).substring("saveInstrument".length());
        var type = data.substring(0, 2);
        data = data.substring(2);

        String figi;
        var instrumentId = 0L;
        InstrumentDBDto instrumentDB;
        double buyPrice = 0;
        double sellPrice = 0;

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (type.charAt(1) == 'i') {
            instrumentId = parseLong(data);
            instrumentDB = databaseSender.getInstrument(instrumentId);
            if (instrumentDB.getFigi().isBlank()) {
                sendMessage.setText(Message.deletedError);
                return sendMessage;
            }
            figi = instrumentDB.getFigi();
            buyPrice = instrumentDB.getBuyPrice();
            sellPrice = instrumentDB.getSellPrice();

            sendMessage.setText(Message.instrumentSaved);
        } else {
            figi = data;
            sendMessage.setText(Message.chooseBuyOrSell);
        }

        var usersPrice = Double.parseDouble(message.getText());

        if (type.startsWith("s")){
            sellPrice = usersPrice;
        } else if (type.startsWith("b")){
            buyPrice = usersPrice;
        }
        var instrumentDBDto = new InstrumentDBDto(instrumentId, chatId, figi, sellPrice, buyPrice);

        var id = databaseSender.saveInstrument(instrumentDBDto);
        if (type.charAt(1) == 'f') {
            List<MarkupDataDto> markupDataDtoList = new ArrayList<>();
            switch (type) {
                case "sf" ->
                    markupDataDtoList.add(new MarkupDataDto(Message.chooseBuy, "contextSPbi" + id));
                case "bf" ->
                    markupDataDtoList.add(new MarkupDataDto(Message.chooseSell, "contextSPsi" + id));
            }
            InstrumentsMarkup.setMenu(sendMessage, markupDataDtoList);
        }
        userContext.put(chatId, "");
        return sendMessage;
    }
}
