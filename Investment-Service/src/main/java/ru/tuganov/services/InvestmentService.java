package ru.tuganov.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InstrumentsService;
import ru.tinkoff.piapi.core.MarketDataService;
import ru.tuganov.dto.InstrumentDto;
import ru.tuganov.dto.PricesDto;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestmentService {
    private final InstrumentsService instrumentsService;
    private final MarketDataService marketDataService;

    public InstrumentDto getInstrument(String instrumentQuery) {
        var instrument = instrumentsService.findInstrument(instrumentQuery).join().getFirst();
        return dtoParser(instrument);
    }

    public ArrayList<InstrumentDto> getInstruments(String instrumentQuery) {
        var instrumentShorts = instrumentsService.findInstrument(instrumentQuery).join();
        return dtoListParser(instrumentShorts);
    }

    public ArrayList<PricesDto> getInstrumentPrices(String instrumentQuery) {
        Instant now = Instant.now();
        return dtoPricesParser(marketDataService.getCandles(instrumentQuery,
                now.minus(1, ChronoUnit.DAYS),
                now,
                CandleInterval.CANDLE_INTERVAL_15_MIN).join());
    }

    private ArrayList<PricesDto> dtoPricesParser(List<HistoricCandle> historicCandles) {
        ArrayList<PricesDto> instruments = new ArrayList<>();
        for (int i = 0; i < historicCandles.size(); i++) {
            var candle = historicCandles.get(i);
//            var high = candle.getHigh().getUnits() + candle.getHigh().getNano() / Math.pow(10.0, 9);
//            var low = candle.getLow().getUnits() + candle.getLow().getNano() / Math.pow(10.0, 9);
//            var open = candle.getOpen().getUnits() + candle.getOpen().getNano() / Math.pow(10.0, 9);
            var close = candle.getClose();
            var closePrice = close.getUnits() + close.getNano() / Math.pow(10.0, 9);
            var time = candle.getTime();
            var closeTime = Instant.ofEpochSecond(time.getSeconds(), time.getNanos());
            instruments.add(
                    new PricesDto(
                            closePrice,
                            closeTime
                    )
            );
        }
        return instruments;
    }

    private InstrumentDto dtoParser(InstrumentShort instrumentShort) {
        var figi = instrumentShort.getFigi();
        var instrument = instrumentsService.getInstrumentByFigi(figi).join();
        var status = instrument.getTradingStatus();
        var type = instrument.getInstrumentType();
        if (type.equals("share") && figi.startsWith("BBG") && (status == SecurityTradingStatus.SECURITY_TRADING_STATUS_NORMAL_TRADING ||
            status == SecurityTradingStatus.SECURITY_TRADING_STATUS_BREAK_IN_TRADING ||
            status == SecurityTradingStatus.SECURITY_TRADING_STATUS_DEALER_BREAK_IN_TRADING ||
            status == SecurityTradingStatus.SECURITY_TRADING_STATUS_DEALER_NORMAL_TRADING)) {
            GetOrderBookResponse orderBook = marketDataService.getOrderBook(figi, 1).join();
            if (orderBook != null && !orderBook.getAsksList().isEmpty()) {
                var price = orderBook.getAsks(0).getPrice();
                var sellPrice = price.getUnits() + price.getNano() / Math.pow(10.0, 9);
                price = orderBook.getBids(0).getPrice();
                var buyPrice = price.getUnits() + price.getNano() / Math.pow(10.0, 9);
                if (sellPrice != 0 && buyPrice != 0) {
                    return new InstrumentDto(instrumentShort.getName(),
                            instrumentShort.getFigi(),
                            sellPrice,
                            buyPrice);
                }
            }
        }
        return null;
    }

    private ArrayList<InstrumentDto> dtoListParser(List<InstrumentShort> instrumentShorts) {
        ArrayList<InstrumentDto> instrumentDtos = new ArrayList<>();
        for (InstrumentShort instrumentShort : instrumentShorts) {
            var instrumentDto = dtoParser(instrumentShort);
            if (instrumentDto != null) {
                instrumentDtos.add(instrumentDto);
            }
        }
        return instrumentDtos;
    }
}
