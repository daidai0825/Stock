package tw.com.stockplatform.chip.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * 三大法人籌碼 DTO（schema-lock v1.0 §5.3）。
 * <p>
 * 仲裁 D2：原 Flat 結構（foreignNetShares / investmentTrustNetShares / dealerNetShares）
 * 改為 {@code institutions} 陣列，與前端 {@code StockChip} 型別對齊。
 * <p>
 * {@code institutions} 順序固定：外資、投信、自營商（前端依此順序渲染，不得動態排序）。
 * {@code totalNetBuySell} = 三筆 netBuySell 加總，由後端計算（前端不得自行加總）。
 * {@code date}：原欄位名 tradeDate，重命名為 date。
 * {@code source}：資料來源（"TWSE" 或 "OTC"）。
 * <p>
 * BUG-QUINCY-001：移除 stockName 欄位；stockName 已由 /quote/get 與 /fundamental/get 提供，
 * /chip/get 職責僅限三大法人買賣超，不重複傳遞。
 */
public record ChipDTO(
    String stockId,
    LocalDate date,
    List<InstitutionItem> institutions,
    long totalNetBuySell,
    String source
) {}
