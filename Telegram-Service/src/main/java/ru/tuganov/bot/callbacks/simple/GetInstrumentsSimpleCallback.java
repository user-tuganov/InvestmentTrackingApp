package ru.tuganov.bot.callbacks.simple;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.tuganov.bot.utils.InstrumentsMarkup;
import ru.tuganov.bot.utils.Message;
import ru.tuganov.broker.senders.DatabaseSender;
import ru.tuganov.broker.senders.InvestmentSender;
import ru.tuganov.dto.InstrumentDBDto;
import ru.tuganov.dto.InstrumentDto;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetInstrumentsSimpleCallback implements SimpleCallback<SendMessage> {
    private final DatabaseSender databaseSender;
    private final InvestmentSender investmentSender;

    @Override
    public SendMessage handle(Update update) {
        var instrumentDBDto = databaseSender.getInstruments(update.getCallbackQuery().getMessage().getChatId());
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
        if (instrumentDBDto == null || instrumentDBDto.isEmpty()) {
            sendMessage.setText(Message.noInstruments);
        } else {
            var usersInstruments = parserDto(instrumentDBDto);
            sendMessage.setText(Message.instrumentList);
            InstrumentsMarkup.addInstruments(sendMessage, usersInstruments, "simpleGUSi");
        }
        return sendMessage;
    }

    private List<InstrumentDto> parserDto(List<InstrumentDBDto> instrumentDBDtoList) {
        List<InstrumentDto> usersInstruments = new ArrayList<>();
        if (instrumentDBDtoList != null) {
            for (InstrumentDBDto instrumentDBDto : instrumentDBDtoList) {
                var instrumentId = instrumentDBDto.getInstrumentId();
                var instrument = investmentSender.getInstrument(instrumentDBDto.getFigi());
                usersInstruments.add(new InstrumentDto(instrument.name(),
                        instrumentId.toString(),
                        instrumentDBDto.getBuyPrice(),
                        instrumentDBDto.getSellPrice()));
            }
        }
        return usersInstruments;
    }
}
