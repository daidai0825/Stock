package tw.com.stockplatform.chip.repository;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tw.com.stockplatform.domain.po.StockChipPO;

import java.util.Optional;

/**
 * 籌碼資料存取層（MyBatis）。
 */
@Mapper
public interface StockChipMapper {

    @Select("""
        SELECT chip_id, stock_id, stock_name, market, trade_date,
               foreign_net_shares, investment_trust_net_shares, dealer_net_shares,
               total_institutional_net, created_at
        FROM stock_chip
        WHERE stock_id = #{stockId}
        ORDER BY trade_date DESC
        LIMIT 1
        """)
    Optional<StockChipPO> findLatestByStockId(@Param("stockId") String stockId);

    @Insert("""
        INSERT INTO stock_chip
            (chip_id, stock_id, stock_name, market, trade_date,
             foreign_net_shares, investment_trust_net_shares, dealer_net_shares,
             total_institutional_net, created_at)
        VALUES
            (#{chipId}, #{stockId}, #{stockName}, #{market}, #{tradeDate},
             #{foreignNetShares}, #{investmentTrustNetShares}, #{dealerNetShares},
             #{totalInstitutionalNet}, #{createdAt})
        ON CONFLICT (stock_id, trade_date) DO UPDATE
            SET foreign_net_shares            = EXCLUDED.foreign_net_shares,
                investment_trust_net_shares   = EXCLUDED.investment_trust_net_shares,
                dealer_net_shares             = EXCLUDED.dealer_net_shares,
                total_institutional_net       = EXCLUDED.total_institutional_net
        """)
    int upsert(StockChipPO po);
}
