package tw.com.stockplatform.domain.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票籌碼（三大法人）PO（對應 stock_chip 表）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockChipPO {

    private String chipId;                         // VARCHAR(36) UUID
    private String stockId;                         // 股票代號
    private String stockName;                       // 股票名稱
    private String market;                          // 市場別：TWSE / OTC
    private LocalDate tradeDate;                    // 交易日期
    private BigDecimal foreignNetShares;            // 外資買賣超（股）
    private BigDecimal investmentTrustNetShares;    // 投信買賣超（股）
    private BigDecimal dealerNetShares;             // 自營商買賣超（股）
    private BigDecimal totalInstitutionalNet;       // 三大法人合計買賣超
    private LocalDateTime createdAt;               // 建立時間
}
