package tw.com.stockplatform.domain.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票主檔 PO（對應 stock_info 表）。
 * <p>
 * Wave 3 新增：提供個股搜尋（中文 / 英文 / 代號）與主檔基本資訊。
 * stock_id 為股票代號（如 "2330"），非 UUID，由 TWSE / OTC 定義，最長 20 字元。
 * <p>
 * 時間欄位統一存 UTC（LocalDateTime），應用層渲染時轉 GMT+8。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockInfoPO {

    private String stockId;          // VARCHAR(20) 股票代號（Primary Key，非 UUID）
    private String stockName;        // VARCHAR(100) 中文名稱
    private String stockNameEn;      // VARCHAR(200) 英文名稱（可 null）
    private String market;           // VARCHAR(10)  市場別：TWSE / OTC
    private String industry;         // VARCHAR(50)  產業別（可 null）
    private LocalDate listedDate;    // DATE 上市 / 上櫃日期（可 null）
    private Boolean isActive;        // BOOLEAN 是否仍在交易（下市 / 下櫃為 false）
    private LocalDateTime createdAt; // TIMESTAMP 建立時間（UTC）
    private LocalDateTime updatedAt; // TIMESTAMP 最後更新時間（UTC，可 null）
}
