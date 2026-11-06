package io.github.kcbear.trading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Purpose: Spring Boot entry point of the global stock/ETF trading backend. It hosts the
 * generated REST controllers (see build/generated/sources/schema) and the demo data seeder.
 */
@SpringBootApplication
public class TradingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingApplication.class, args);
    }
}
