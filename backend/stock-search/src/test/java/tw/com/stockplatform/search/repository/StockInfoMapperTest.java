package tw.com.stockplatform.search.repository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import tw.com.stockplatform.domain.po.StockInfoPO;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wave 3：StockInfoMapper MyBatis 整合測試（Testcontainers PostgreSQL 16）。
 * <p>
 * 驗證：
 * <ul>
 *   <li>findByStockId - 精確匹配</li>
 *   <li>findByStockIdPrefix - 代號前綴搜尋</li>
 *   <li>findByStockNamePrefix - 中文名稱前綴搜尋</li>
 *   <li>searchByNameContains - 中文名稱部分匹配（LIKE '%keyword%'，不依賴 pg_trgm）</li>
 *   <li>findByStockNameEnPrefix - 英文名稱前綴搜尋</li>
 *   <li>upsert - 新增 / 更新衝突處理</li>
 *   <li>countActiveByMarket - 計數查詢</li>
 * </ul>
 * <p>
 * 注意：pg_trgm GIN index（searchByKeyword）在此測試中不啟用，
 * 因為 CREATE EXTENSION 需 superuser 權限（決策 P5）。
 * searchByNameContains（LIKE '%keyword%'）作為降級替代。
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Transactional
class StockInfoMapperTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("stockdb_test")
        .withUsername("stockuser")
        .withPassword("stockpass")
        .withInitScript("test-schema/stock_info_test.sql");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private StockInfoMapper stockInfoMapper;

    private static final LocalDateTime NOW_UTC = LocalDateTime.now(ZoneOffset.UTC);

    @BeforeAll
    static void requireDocker() {
        // 本機無 Docker daemon 時自動 skip，避免 pre-push hook 因環境問題 fail。
        // CI 必須有 Docker（會執行此測試）。
        boolean dockerAvailable;
        try {
            dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            dockerAvailable = false;
        }
        assumeTrue(dockerAvailable, "Docker daemon not available — skipping Testcontainers test");
    }

    @BeforeEach
    void setUp() {
        // 清空並插入測試資料
        // （由於 @Transactional，每個測試結束後自動 rollback）
        insertTestData("2330", "台積電", "TSMC", "TWSE", "半導體");
        insertTestData("2317", "鴻海", "Hon Hai", "TWSE", "電子零組件");
        insertTestData("2454", "聯發科", "MediaTek", "TWSE", "半導體");
        insertTestData("6488", "環球晶", "GlobalWafers", "OTC", "半導體");
        insertTestData("2412", "中華電", "Chunghwa Telecom", "TWSE", "電信");
        // 下市股（is_active = false）
        insertInactiveData("9999", "測試下市股", "Delisted Stock", "TWSE");
    }

    // ==========================================================================
    // findByStockId
    // ==========================================================================

    @Test
    @DisplayName("findByStockId - 精確匹配台積電 2330 → 回傳 Optional 包含 PO")
    void givenExistingStockId_whenFindById_thenReturnStockInfo() {
        Optional<StockInfoPO> result = stockInfoMapper.findByStockId("2330");

        assertThat(result).isPresent();
        assertThat(result.get().getStockId()).isEqualTo("2330");
        assertThat(result.get().getStockName()).isEqualTo("台積電");
        assertThat(result.get().getMarket()).isEqualTo("TWSE");
        assertThat(result.get().getIsActive()).isTrue();
    }

    @Test
    @DisplayName("findByStockId - 不存在的股票代號 → 回傳 Optional.empty()")
    void givenNonExistingStockId_whenFindById_thenReturnEmpty() {
        Optional<StockInfoPO> result = stockInfoMapper.findByStockId("9998");

        assertThat(result).isEmpty();
    }

    // ==========================================================================
    // findByStockIdPrefix
    // ==========================================================================

    @Test
    @DisplayName("findByStockIdPrefix - 輸入 '23' 應命中 2330/2317 等（is_active=true）")
    void givenPrefix23_whenFindByIdPrefix_thenReturnMatchingStocks() {
        List<StockInfoPO> results = stockInfoMapper.findByStockIdPrefix("23%", 10);

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(po -> po.getStockId().startsWith("23"));
        assertThat(results).allMatch(po -> Boolean.TRUE.equals(po.getIsActive()));
    }

    @Test
    @DisplayName("findByStockIdPrefix - 精確代號 '2330' 作為前綴 → 只回傳 2330")
    void givenExactStockIdAsPrefix_whenFindByIdPrefix_thenReturnExactMatch() {
        List<StockInfoPO> results = stockInfoMapper.findByStockIdPrefix("2330%", 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStockId()).isEqualTo("2330");
    }

    // ==========================================================================
    // findByStockNamePrefix
    // ==========================================================================

    @Test
    @DisplayName("findByStockNamePrefix - 輸入 '台' 應命中台積電、中華電等")
    void givenChinesePrefix_whenFindByNamePrefix_thenReturnMatchingStocks() {
        List<StockInfoPO> results = stockInfoMapper.findByStockNamePrefix("台%", 10);

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(po -> po.getStockName().startsWith("台"));
    }

    @Test
    @DisplayName("findByStockNamePrefix - 不存在的前綴 → 回傳空 List")
    void givenNonExistingPrefix_whenFindByNamePrefix_thenReturnEmpty() {
        List<StockInfoPO> results = stockInfoMapper.findByStockNamePrefix("不存在%", 10);

        assertThat(results).isEmpty();
    }

    // ==========================================================================
    // searchByNameContains（LIKE '%keyword%'）
    // ==========================================================================

    @Test
    @DisplayName("searchByNameContains - '積電' 部分匹配 → 命中台積電")
    void givenPartialKeyword_whenSearchByNameContains_thenReturnMatchingStocks() {
        List<StockInfoPO> results = stockInfoMapper.searchByNameContains("%積電%", 10);

        assertThat(results).isNotEmpty();
        assertThat(results).anyMatch(po -> "2330".equals(po.getStockId()));
    }

    @Test
    @DisplayName("searchByNameContains - 限制 limit=1 → 最多回傳 1 筆")
    void givenLimitOne_whenSearch_thenReturnAtMostOne() {
        List<StockInfoPO> results = stockInfoMapper.searchByNameContains("%電%", 1);

        assertThat(results).hasSizeLessThanOrEqualTo(1);
    }

    // ==========================================================================
    // findByStockNameEnPrefix
    // ==========================================================================

    @Test
    @DisplayName("findByStockNameEnPrefix - 輸入 'TSMC' 英文前綴（大寫）→ 命中台積電")
    void givenEnglishPrefix_whenFindByNameEnPrefix_thenReturnMatchingStocks() {
        List<StockInfoPO> results = stockInfoMapper.findByStockNameEnPrefix("TSMC%", 10);

        assertThat(results).isNotEmpty();
        assertThat(results).anyMatch(po -> "2330".equals(po.getStockId()));
    }

    @Test
    @DisplayName("findByStockNameEnPrefix - 小寫輸入 'tsmc' → 大小寫不敏感仍命中台積電")
    void givenLowerCasePrefix_whenFindByNameEnPrefix_thenReturnMatchingStocks() {
        List<StockInfoPO> results = stockInfoMapper.findByStockNameEnPrefix("tsmc%", 10);

        assertThat(results).isNotEmpty();
        assertThat(results).anyMatch(po -> "2330".equals(po.getStockId()));
    }

    // ==========================================================================
    // upsert
    // ==========================================================================

    @Test
    @DisplayName("upsert - 新插入不存在的股票 → 可查詢到")
    void givenNewStock_whenUpsert_thenCanBeFound() {
        StockInfoPO newStock = buildStockInfo("1234", "新測試股", "New Test", "OTC", true);
        stockInfoMapper.upsert(newStock);

        Optional<StockInfoPO> found = stockInfoMapper.findByStockId("1234");
        assertThat(found).isPresent();
        assertThat(found.get().getStockName()).isEqualTo("新測試股");
    }

    @Test
    @DisplayName("upsert - 已存在的股票 → 更新 stock_name（ON CONFLICT DO UPDATE）")
    void givenExistingStock_whenUpsert_thenNameUpdated() {
        StockInfoPO updated = buildStockInfo("2330", "台積電更新版", "TSMC Updated", "TWSE", true);
        stockInfoMapper.upsert(updated);

        Optional<StockInfoPO> found = stockInfoMapper.findByStockId("2330");
        assertThat(found).isPresent();
        assertThat(found.get().getStockName()).isEqualTo("台積電更新版");
    }

    // ==========================================================================
    // countActiveByMarket
    // ==========================================================================

    @Test
    @DisplayName("countActiveByMarket - TWSE 市場有效股票數量 ≥ 4（含 setUp 插入的）")
    void givenTWSEMarket_whenCount_thenReturnCorrectCount() {
        long count = stockInfoMapper.countActiveByMarket("TWSE");

        // setUp 插入 2330/2317/2454/2412 四筆 TWSE（9999 下市）
        assertThat(count).isGreaterThanOrEqualTo(4);
    }

    @Test
    @DisplayName("countActiveByMarket - OTC 市場有效股票數量 ≥ 1（6488）")
    void givenOTCMarket_whenCount_thenReturnCorrectCount() {
        long count = stockInfoMapper.countActiveByMarket("OTC");

        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    // ==========================================================================
    // 輔助方法
    // ==========================================================================

    private void insertTestData(String stockId, String stockName, String stockNameEn, String market, String industry) {
        StockInfoPO po = StockInfoPO.builder()
            .stockId(stockId)
            .stockName(stockName)
            .stockNameEn(stockNameEn)
            .market(market)
            .industry(industry)
            .isActive(true)
            .createdAt(NOW_UTC)
            .build();
        stockInfoMapper.upsert(po);
    }

    private void insertInactiveData(String stockId, String stockName, String stockNameEn, String market) {
        StockInfoPO po = StockInfoPO.builder()
            .stockId(stockId)
            .stockName(stockName)
            .stockNameEn(stockNameEn)
            .market(market)
            .isActive(false)
            .createdAt(NOW_UTC)
            .build();
        stockInfoMapper.upsert(po);
    }

    private StockInfoPO buildStockInfo(String stockId, String stockName, String stockNameEn,
                                       String market, boolean isActive) {
        return StockInfoPO.builder()
            .stockId(stockId)
            .stockName(stockName)
            .stockNameEn(stockNameEn)
            .market(market)
            .isActive(isActive)
            .createdAt(NOW_UTC)
            .updatedAt(NOW_UTC)
            .build();
    }
}
