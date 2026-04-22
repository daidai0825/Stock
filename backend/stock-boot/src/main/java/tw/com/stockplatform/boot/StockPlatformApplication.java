package tw.com.stockplatform.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Stock Platform 唯一啟動類（Modular Monolith）。
 * <p>
 * 透過 component scan 統一掃描所有業務 module 的 {@code tw.com.stockplatform.**} 套件。
 */
@SpringBootApplication
@ComponentScan(basePackages = "tw.com.stockplatform")
public class StockPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockPlatformApplication.class, args);
    }
}
