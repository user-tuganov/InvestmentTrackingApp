package ru.tuganov.bot.handlers;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.tuganov.bot.callbacks.context.AddInstrumentCallback;
import ru.tuganov.bot.callbacks.context.ContextCallbackHandler;
import ru.tuganov.bot.callbacks.context.NewPriceCallback;
import ru.tuganov.bot.callbacks.context.EditPriceCallback;
import ru.tuganov.bot.callbacks.simple.*;
import ru.tuganov.bot.utils.Message;
import ru.tuganov.bot.utils.Metrics;
import ru.tuganov.broker.senders.DatabaseSender;
import ru.tuganov.broker.senders.InvestmentSender;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallbackHandler {
    private final DatabaseSender databaseSender;
    private final InvestmentSender investmentSender;

    private Map<String, SimpleCallback<?>> callBacks;

    @PostConstruct
    private void init() {
        callBacks = Map.of(
                "simpleGUS", new GetInstrumentSimpleCallback(databaseSender, investmentSender),
                "simpleGUI", new GetInstrumentsSimpleCallback(databaseSender, investmentSender),
                "simpleDIC", new DeleteSimpleCallback(databaseSender)
        );
    }

    @NonFinal
    private final Map<String, ContextCallbackHandler> contextCallBackHandler = Map.of (
      "contextAI", new AddInstrumentCallback(),
            "contextSN", new NewPriceCallback(),
            "contextSP", new EditPriceCallback()
    );

    public SendMessage handleCallBack(Update update, Map<Long, String> userContext) throws IOException {
        var callBackData = update.getCallbackQuery().getData();
        log.info(callBackData);
        if (callBackData.startsWith("context")) {
            userContext.put(update.getCallbackQuery().getMessage().getChatId(), "");
            var callBack = contextCallBackHandler.get(callBackData.substring(0, Metrics.contextCallBackLength));
            if (callBack == null)
                return new SendMessage(String.valueOf(update.getCallbackQuery().getMessage().getChatId()), Message.unknownCommand);
            else
                return callBack.handle(update, userContext);
        } else {
            var callBack = callBacks.get(callBackData.substring(0, Metrics.simpleCallBackLength));
            if (callBack == null)
                return new SendMessage(String.valueOf(update.getCallbackQuery().getMessage().getChatId()), Message.unknownCommand);
            else
                return (SendMessage) callBack.handle(update);
        }
    }
}
