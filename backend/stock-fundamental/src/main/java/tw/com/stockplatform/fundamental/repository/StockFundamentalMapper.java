package tw.com.stockplatform.fundamental.repository;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tw.com.stockplatform.domain.po.StockFundamentalPO;

import java.util.Optional;

/**
 * 基本面資料存取層（MyBatis）。
 */
@Mapper
public interface StockFundamentalMapper {

    @Select("""
        SELECT fundamental_id, stock_id, stock_name, eps, per_ratio, pbr_ratio, roe,
               report_year, report_quarter, updated_at, created_at
        FROM stock_fundamental
        WHERE stock_id = #{stockId}
        """)
    Optional<StockFundamentalPO> findByStockId(@Param("stockId") String stockId);

    @Insert("""
        INSERT INTO stock_fundamental
            (fundamental_id, stock_id, stock_name, eps, per_ratio, pbr_ratio, roe,
             report_year, report_quarter, updated_at, created_at)
        VALUES
            (#{fundamentalId}, #{stockId}, #{stockName}, #{eps}, #{perRatio}, #{pbrRatio}, #{roe},
             #{reportYear}, #{reportQuarter}, #{updatedAt}, #{createdAt})
        ON CONFLICT (stock_id) DO UPDATE
            SET eps            = EXCLUDED.eps,
                per_ratio      = EXCLUDED.per_ratio,
                pbr_ratio      = EXCLUDED.pbr_ratio,
                roe            = EXCLUDED.roe,
                report_year    = EXCLUDED.report_year,
                report_quarter = EXCLUDED.report_quarter,
                updated_at     = EXCLUDED.updated_at
        """)
    int upsert(StockFundamentalPO po);
}
