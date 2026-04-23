package tw.com.stockplatform.search.repository;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tw.com.stockplatform.domain.po.StockInfoPO;

import java.util.List;
import java.util.Optional;

/**
 * Wave 3：股票主檔 MyBatis Mapper。
 * <p>
 * 對應資料表：stock_info（V3.0.0 migration 建立）
 * <p>
 * 搜尋優先序（依 SRS §3.1.2）：
 * <ol>
 *   <li>股票代號完全匹配（stock_id = ?）</li>
 *   <li>股票代號前綴匹配（stock_id LIKE 'xxx%'，idx_stock_info_id_prefix）</li>
 *   <li>中文名稱完全匹配（stock_name = ?）</li>
 *   <li>中文名稱前綴匹配（stock_name LIKE 'xxx%'，idx_stock_info_name）</li>
 *   <li>中文名稱部分匹配（stock_name % keyword，pg_trgm GIN）</li>
 *   <li>英文名稱前綴匹配（LOWER(stock_name_en) ILIKE 'xxx%'）</li>
 * </ol>
 * <p>
 * 注意：複合搜尋邏輯（6 種優先序合併）由 Service 層呼叫多次 Mapper 方法組合，
 * 或改用 XML 動態 SQL（Wave 3 W2 實作）。
 */
@Mapper
public interface StockInfoMapper {

    /**
     * 依股票代號查詢（精確匹配）。
     * 回傳 Optional 供 Service 判斷是否存在，不寫入 PO Optional 欄位。
     */
    @Select("SELECT stock_id, stock_name, stock_name_en, market, industry, listed_date, is_active, created_at, updated_at " +
            "FROM stock_info WHERE stock_id = #{stockId}")
    Optional<StockInfoPO> findByStockId(String stockId);

    /**
     * 依股票代號前綴搜尋（LIKE 'xxx%'）。
     * 使用 idx_stock_info_id_prefix（text_pattern_ops）加速。
     */
    @Select("SELECT stock_id, stock_name, stock_name_en, market, industry, listed_date, is_active, created_at, updated_at " +
            "FROM stock_info " +
            "WHERE stock_id LIKE #{prefix} " +
            "  AND is_active = TRUE " +
            "ORDER BY stock_id ASC " +
            "LIMIT #{limit}")
    List<StockInfoPO> findByStockIdPrefix(@Param("prefix") String prefix, @Param("limit") int limit);

    /**
     * 中文名稱完全匹配。
     */
    @Select("SELECT stock_id, stock_name, stock_name_en, market, industry, listed_date, is_active, created_at, updated_at " +
            "FROM stock_info " +
            "WHERE stock_name = #{stockName} AND is_active = TRUE " +
            "LIMIT #{limit}")
    List<StockInfoPO> findByStockNameExact(@Param("stockName") String stockName, @Param("limit") int limit);

    /**
     * 中文名稱前綴搜尋（LIKE 'xxx%'）。
     * 使用 idx_stock_info_name（text_pattern_ops）。
     */
    @Select("SELECT stock_id, stock_name, stock_name_en, market, industry, listed_date, is_active, created_at, updated_at " +
            "FROM stock_info " +
            "WHERE stock_name LIKE #{prefix} AND is_active = TRUE " +
            "ORDER BY stock_id ASC " +
            "LIMIT #{limit}")
    List<StockInfoPO> findByStockNamePrefix(@Param("prefix") String prefix, @Param("limit") int limit);

    /**
     * 中文名稱部分匹配（pg_trgm similarity operator %）。
     * <p>
     * 需 DBA 預先啟用 pg_trgm extension（決策 P5）。
     * 使用 idx_stock_info_name_trgm（GIN）加速。
     * <p>
     * 若 pg_trgm extension 未啟用，此查詢退化為 seq scan（效能差但可運作）。
     * Wave 3 W2 可改以 LIKE '%keyword%' 作降級處理。
     */
    @Select("SELECT stock_id, stock_name, stock_name_en, market, industry, listed_date, is_active, created_at, updated_at " +
            "FROM stock_info " +
            "WHERE stock_name % #{keyword} AND is_active = TRUE " +
            "ORDER BY similarity(stock_name, #{keyword}) DESC " +
            "LIMIT #{limit}")
    List<StockInfoPO> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);

    /**
     * 中文名稱 LIKE 部分匹配（不依賴 pg_trgm，效能較差但相容性高）。
     * 可作為 pg_trgm 降級方案。
     */
    @Select("SELECT stock_id, stock_name, stock_name_en, market, industry, listed_date, is_active, created_at, updated_at " +
            "FROM stock_info " +
            "WHERE stock_name LIKE #{pattern} AND is_active = TRUE " +
            "ORDER BY stock_id ASC " +
            "LIMIT #{limit}")
    List<StockInfoPO> searchByNameContains(@Param("pattern") String pattern, @Param("limit") int limit);

    /**
     * 英文名稱前綴搜尋（大小寫不敏感）。
     * 使用 idx_stock_info_name_en_lower（LOWER() 函式索引）。
     */
    @Select("SELECT stock_id, stock_name, stock_name_en, market, industry, listed_date, is_active, created_at, updated_at " +
            "FROM stock_info " +
            "WHERE LOWER(stock_name_en) LIKE LOWER(#{prefix}) AND is_active = TRUE " +
            "ORDER BY stock_id ASC " +
            "LIMIT #{limit}")
    List<StockInfoPO> findByStockNameEnPrefix(@Param("prefix") String prefix, @Param("limit") int limit);

    /**
     * 查詢所有有效股票（供排程全量同步用，一次批次取）。
     * 注意：資料量約 4000 筆，勿在高頻請求中使用。
     */
    @Select("SELECT stock_id, stock_name, stock_name_en, market, industry, listed_date, is_active, created_at, updated_at " +
            "FROM stock_info WHERE is_active = TRUE ORDER BY stock_id ASC")
    List<StockInfoPO> findAllActive();

    /**
     * 新增或更新股票主檔（upsert，供 StockInfoSyncService 每日同步使用）。
     */
    @Insert("INSERT INTO stock_info (stock_id, stock_name, stock_name_en, market, industry, listed_date, is_active, created_at, updated_at) " +
            "VALUES (#{stockId}, #{stockName}, #{stockNameEn}, #{market}, #{industry}, #{listedDate}, #{isActive}, #{createdAt}, #{updatedAt}) " +
            "ON CONFLICT (stock_id) DO UPDATE SET " +
            "  stock_name    = EXCLUDED.stock_name, " +
            "  stock_name_en = EXCLUDED.stock_name_en, " +
            "  market        = EXCLUDED.market, " +
            "  industry      = EXCLUDED.industry, " +
            "  listed_date   = EXCLUDED.listed_date, " +
            "  is_active     = EXCLUDED.is_active, " +
            "  updated_at    = EXCLUDED.updated_at")
    void upsert(StockInfoPO po);

    /**
     * 批次更新 is_active（下市 / 下櫃時使用）。
     */
    @Update("UPDATE stock_info SET is_active = #{isActive}, updated_at = #{updatedAt} " +
            "WHERE stock_id = #{stockId}")
    int updateActiveStatus(StockInfoPO po);

    /**
     * 依市場別查詢有效股票數量（監控 / 告警用）。
     */
    @Select("SELECT COUNT(*) FROM stock_info WHERE market = #{market} AND is_active = TRUE")
    long countActiveByMarket(String market);
}
