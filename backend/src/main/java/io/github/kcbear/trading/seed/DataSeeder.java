package io.github.kcbear.trading.seed;

import io.github.kcbear.trading.generated.model.Instrument;
import io.github.kcbear.trading.generated.model.Trade;
import io.github.kcbear.trading.generated.repository.InstrumentRepository;
import io.github.kcbear.trading.generated.repository.TradeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Purpose: fills the in-memory H2 database with deterministic SYNTHETIC global market data
 * (default 12,000 stocks/ETFs across 20 exchanges and 15,000 trades) so the UI can demonstrate
 * server-side paging, sorting and filtering at scale. Runs before the web server starts
 * (SmartInitializingSingleton), so the API never serves a half-seeded dataset.
 * All names, ISINs and prices are fictional.
 */
@Component
public class DataSeeder implements SmartInitializingSingleton {

    private record Exchange(String mic, String country, String currency) { }

    private static final Exchange[] EXCHANGES = {
        new Exchange("XNYS", "US", "USD"), new Exchange("XNAS", "US", "USD"), new Exchange("XLON", "GB", "GBP"),
        new Exchange("XTKS", "JP", "JPY"), new Exchange("XHKG", "HK", "HKD"), new Exchange("XSHG", "CN", "CNY"),
        new Exchange("XSHE", "CN", "CNY"), new Exchange("XPAR", "FR", "EUR"), new Exchange("XETR", "DE", "EUR"),
        new Exchange("XASX", "AU", "AUD"), new Exchange("XTSE", "CA", "CAD"), new Exchange("XSWX", "CH", "CHF"),
        new Exchange("XBOM", "IN", "INR"), new Exchange("XKRX", "KR", "KRW"), new Exchange("XSES", "SG", "SGD"),
        new Exchange("XAMS", "NL", "EUR"), new Exchange("XMIL", "IT", "EUR"), new Exchange("XMAD", "ES", "EUR"),
        new Exchange("XSTO", "SE", "SEK"), new Exchange("BVMF", "BR", "BRL")
    };
    private static final String[] SECTORS = {
        "Technology", "Financials", "Healthcare", "Energy", "Utilities", "Industrials", "Consumer Staples",
        "Consumer Discretionary", "Materials", "Real Estate", "Communication Services"
    };
    private static final String[] ADJ = {
        "Apex", "Summit", "Orion", "Nimbus", "Vertex", "Aurora", "Pioneer", "Atlas",
        "Zenith", "Meridian", "Falcon", "Cobalt", "Harbor", "Lumen", "Quantum", "Redwood"
    };
    private static final String[] NOUN = {
        "Dynamics", "Holdings", "Systems", "Industries", "Energy", "Pharma", "Logistics",
        "Networks", "Capital", "Foods", "Materials", "Semiconductors", "Motors", "Retail"
    };
    private static final String[] ETF_THEME = {
        "Global Equity", "Emerging Markets", "Technology", "Healthcare", "Clean Energy", "Dividend Growth",
        "Government Bond", "Corporate Bond", "Gold", "Real Estate", "Small Cap", "Value", "Momentum",
        "Semiconductor", "Infrastructure"
    };
    private static final String[] STATUSES = {"FILLED", "FILLED", "FILLED", "PARTIAL", "CANCELLED", "REJECTED"};

    private final InstrumentRepository instruments;
    private final TradeRepository trades;
    private final int instrumentCount;
    private final int tradeCount;

    public DataSeeder(InstrumentRepository instruments, TradeRepository trades,
                      @Value("${app.seed.instruments:12000}") int instrumentCount,
                      @Value("${app.seed.trades:15000}") int tradeCount) {
        this.instruments = instruments;
        this.trades = trades;
        this.instrumentCount = instrumentCount;
        this.tradeCount = tradeCount;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (instruments.count() == 0) {
            seedInstruments();
        }
        if (trades.count() == 0 && instrumentCount > 0) {
            seedTrades();
        }
    }

    private void seedInstruments() {
        Random rnd = new Random(42);
        List<Instrument> batch = new ArrayList<>(1000);
        for (int i = 0; i < instrumentCount; i++) {
            Exchange ex = EXCHANGES[rnd.nextInt(EXCHANGES.length)];
            boolean etf = rnd.nextInt(100) < 30;
            Instrument in = new Instrument();
            in.setSymbol(symbol(i));
            in.setName(etf
                    ? ADJ[rnd.nextInt(ADJ.length)] + " " + ETF_THEME[rnd.nextInt(ETF_THEME.length)] + " ETF"
                    : ADJ[rnd.nextInt(ADJ.length)] + " " + NOUN[rnd.nextInt(NOUN.length)] + " " + ex.country());
            in.setIsin(ex.country() + String.format("%09d", (i * 104_729L) % 1_000_000_000L) + (i % 10));
            in.setInstrumentType(etf ? "ETF" : "STOCK");
            in.setExchangeMic(ex.mic());
            in.setCountry(ex.country());
            in.setCurrency(ex.currency());
            in.setSector(etf && rnd.nextInt(3) == 0 ? "Broad Market" : SECTORS[rnd.nextInt(SECTORS.length)]);
            in.setLastPrice(BigDecimal.valueOf(1 + rnd.nextDouble() * 999).setScale(4, RoundingMode.HALF_UP));
            in.setMarketCap(50_000_000L + (long) (rnd.nextDouble() * rnd.nextDouble() * 2_000_000_000_000L));
            in.setExpenseRatio(etf
                    ? BigDecimal.valueOf(0.03 + rnd.nextDouble() * 0.9).setScale(4, RoundingMode.HALF_UP) : null);
            in.setListedDate(LocalDate.of(1980, 1, 1).plusDays(rnd.nextInt(45 * 365)));
            in.setActive(rnd.nextInt(100) < 97);
            batch.add(in);
            if (batch.size() == 1000) {
                instruments.saveAll(batch);
                batch.clear();
            }
        }
        instruments.saveAll(batch);
    }

    private void seedTrades() {
        Random rnd = new Random(7);
        List<Trade> batch = new ArrayList<>(1000);
        for (int i = 0; i < tradeCount; i++) {
            Exchange ex = EXCHANGES[rnd.nextInt(EXCHANGES.length)];
            Trade t = new Trade();
            t.setTradeTime(Instant.now().minusSeconds(rnd.nextInt(90 * 86_400)));
            t.setSymbol(symbol(rnd.nextInt(instrumentCount)));
            t.setSide(rnd.nextBoolean() ? "BUY" : "SELL");
            t.setQuantity(1 + rnd.nextInt(5000));
            t.setPrice(BigDecimal.valueOf(1 + rnd.nextDouble() * 999).setScale(4, RoundingMode.HALF_UP));
            t.setCurrency(ex.currency());
            t.setVenue(ex.mic());
            t.setStatus(STATUSES[rnd.nextInt(STATUSES.length)]);
            batch.add(t);
            if (batch.size() == 1000) {
                trades.saveAll(batch);
                batch.clear();
            }
        }
        trades.saveAll(batch);
    }

    /** Unique 4-letter ticker per index (7919 is coprime with 26^4, so the mapping is a bijection). */
    static String symbol(int i) {
        int v = (int) ((i * 7919L) % 456_976L);
        char[] c = new char[4];
        for (int k = 3; k >= 0; k--) {
            c[k] = (char) ('A' + v % 26);
            v /= 26;
        }
        return new String(c);
    }
}
