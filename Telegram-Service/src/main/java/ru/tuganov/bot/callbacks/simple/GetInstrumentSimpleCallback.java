package ru.tuganov.bot.callbacks.simple;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.tuganov.bot.utils.InstrumentsMarkup;
import ru.tuganov.bot.utils.Message;
import ru.tuganov.broker.senders.DatabaseSender;
import ru.tuganov.broker.senders.InvestmentSender;
import ru.tuganov.dto.InstrumentDBDto;
import ru.tuganov.dto.InstrumentDto;
import ru.tuganov.dto.MarkupDataDto;
import ru.tuganov.dto.PricesDto;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Long.parseLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetInstrumentSimpleCallback implements SimpleCallback<SendPhoto> {
    private final DatabaseSender databaseSender;
    private final InvestmentSender investmentSender;

    @Override
    public SendPhoto handle(Update update) throws IOException {
        var callBack = update.getCallbackQuery();

        var data = callBack.getData().substring("simpleGUS".length());
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(String.valueOf(callBack.getMessage().getChatId()));
        List<MarkupDataDto> markupDataDtoList = new ArrayList<>();
        String figi;
        InstrumentDto instrumentInvestment;
        InstrumentDBDto instrumentDB = new InstrumentDBDto(-1L, -1L, "", -1.0, -1.0);
        if (data.charAt(0) == 'i') {
            var instrumentId = parseLong(data.substring(1));
            instrumentDB = databaseSender.getInstrument(instrumentId);
            if (instrumentDB.getFigi().isBlank()) {
//                sendMessage.setText(Message.deletedError);
                return null;
            }
            figi = instrumentDB.getFigi();
            instrumentInvestment = investmentSender.getInstrument(instrumentDB.getFigi());
            markupDataDtoList.add(new MarkupDataDto(Message.deleteInstrument, "simpleDIC" + data));
//            sendPhoto.setCaption(String.format(Message.instrumentInfo,
//                                                instrumentInvestment.name(),
//                                                instrumentInvestment.sellPrice(),
//                                                instrumentInvestment.buyPrice(),
//                                                instrumentDB.getSellPrice(),
//                                                instrumentDB.getBuyPrice()));
        } else {
            figi = data;
            data = 'f' + data;
            instrumentInvestment = investmentSender.getInstrument(figi);
//            sendPhoto.setCaption(Message.instrumentInfo + Message.chooseBuyOrSell);
        }

        markupDataDtoList.addAll(List.of(
                new MarkupDataDto(Message.chooseBuy, "contextSNb" + data),
                new MarkupDataDto(Message.chooseSell, "contextSNs" + data))
        );

        sendPhoto.setCaption(String.format(Message.instrumentInfo,
                instrumentInvestment.name(),
                instrumentInvestment.sellPrice(),
                instrumentInvestment.buyPrice(),
                instrumentDB.getSellPrice(),
                instrumentDB.getBuyPrice()));
        InstrumentsMarkup.setMenu(sendPhoto, markupDataDtoList);
        renderGraphic(figi, sendPhoto);
        return sendPhoto;
    }


    private void renderGraphic(String figi, SendPhoto sendPhoto) throws IOException {
        List<PricesDto> pricesDtos = investmentSender.getPrices(figi);
        log.info("size: {}", pricesDtos.size());
        var series = new TimeSeries("");
        for (int i = 0; i < pricesDtos.size(); i++) {
            var priceDto = pricesDtos.get(i);
            var closeTime = priceDto.closeTime();
            var closePrice = priceDto.close();
            var dateTime = closeTime.atZone(ZoneId.of("UTC"));
            series.addOrUpdate(
                    new Minute(dateTime.getMinute(),
                            dateTime.getHour(),
                            dateTime.getDayOfMonth(),
                            dateTime.getMonthValue(),
                            dateTime.getYear()),
                    closePrice);
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection(series);
        JFreeChart chart = ChartFactory.createTimeSeriesChart("",
                "", "",
                dataset,
                false, true, false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getXYPlot().setBackgroundPaint(new GradientPaint(0, 0, Color.WHITE, 0, 1000, Color.LIGHT_GRAY));

        XYPlot plot = chart.getXYPlot();
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);

        XYItemRenderer renderer = plot.getRenderer();
        renderer.setSeriesPaint(0, Color.BLACK);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));

        plot.setOutlinePaint(Color.BLACK);
        plot.setOutlineStroke(new BasicStroke(1.0f));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(chart.createBufferedImage(800, 600), "png", baos);
        baos.flush();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(baos.toByteArray());
        sendPhoto.setPhoto(new InputFile(inputStream, "chart.png"));

        baos.close();
        inputStream.close();
    }
}
