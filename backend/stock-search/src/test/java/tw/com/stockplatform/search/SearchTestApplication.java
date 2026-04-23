package tw.com.stockplatform.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 測試專用啟動類別。
 * 讓 @MybatisTest 能在 stock-search 模組內找到 @SpringBootConfiguration。
 * 不會在生產環境被載入（位於 test scope）。
 */
@SpringBootApplication
public class SearchTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchTestApplication.class, args);
    }
}
