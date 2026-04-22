package tw.com.stockplatform.domain.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票每日行情 PO（對應 stock_quote 表）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockQuotePO {

    private String quoteId;         // VARCHAR(36) UUID
    private String stockId;          // 股票代號
    private String stockName;        // 股票名稱
    private String market;           // 市場別：TWSE / OTC
    private BigDecimal openPrice;    // 開盤價
    private BigDecimal highPrice;    // 最高價
    private BigDecimal lowPrice;     // 最低價
    private BigDecimal closePrice;   // 收盤價
    private Long volume;             // 成交量（股）
    private LocalDate quoteDate;     // 交易日期
    private LocalDateTime createdAt; // 資料建立時間
}
