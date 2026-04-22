package tw.com.stockplatform.infrastructure.client.mops;

import java.util.List;

/**
 * 公開資訊觀測站（MOPS）外部資料 Client 介面。
 * <p>
 * 提供基本面資料：EPS、季報、年報。
 * <p>
 * B-BE-W2-04 修正：新增 market 參數，對應 MOPS TYPEK：
 * <ul>
 *   <li>TWSE（上市）→ {@code sii}</li>
 *   <li>OTC（上櫃）→ {@code otc}</li>
 * </ul>
 */
public interface MOPSClient {

    /**
     * 查詢個股近四季 EPS。
     *
     * @param stockId 股票代號
     * @param market  市場別（"TWSE" 或 "OTC"）
     * @return EPS 資料清單（最多 4 筆，依季度降序）
     */
    List<MOPSEpsDTO> fetchEps(String stockId, String market);

    /**
     * 查詢個股財務摘要（PER、PBR、ROE）。
     *
     * @param stockId 股票代號
     * @param market  市場別（"TWSE" 或 "OTC"）
     * @return 財務摘要
     */
    MOPSFinancialSummaryDTO fetchFinancialSummary(String stockId, String market);
}
