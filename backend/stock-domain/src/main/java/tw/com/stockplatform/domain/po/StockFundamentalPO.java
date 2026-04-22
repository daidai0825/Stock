package tw.com.stockplatform.domain.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 股票基本面 PO（對應 stock_fundamental 表）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockFundamentalPO {

    private String fundamentalId;    // VARCHAR(36) UUID
    private String stockId;           // 股票代號
    private String stockName;         // 股票名稱
    private BigDecimal eps;           // 近四季合計 EPS
    private BigDecimal perRatio;      // 本益比
    private BigDecimal pbrRatio;      // 股價淨值比
    private BigDecimal roe;           // 股東權益報酬率（%）
    private int reportYear;           // 最新報告年度
    private int reportQuarter;        // 最新報告季度
    private LocalDateTime updatedAt;  // 最後更新時間
    private LocalDateTime createdAt;  // 建立時間
}
