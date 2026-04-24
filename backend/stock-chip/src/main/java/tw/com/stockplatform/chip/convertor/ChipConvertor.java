package tw.com.stockplatform.chip.convertor;

import org.mapstruct.Mapper;
import tw.com.stockplatform.chip.dto.response.ChipDTO;
import tw.com.stockplatform.chip.dto.response.InstitutionItem;
import tw.com.stockplatform.domain.po.StockChipPO;

import java.util.List;

/**
 * 籌碼 PO ↔ DTO 轉換（schema-lock v1.0 §5.3 對齊）。
 * <p>
 * 仲裁 D2：將 PO 的 flat 欄位結構重構為 institutions 陣列。
 * <p>
 * 欄位對應：
 * <ul>
 *   <li>PO.tradeDate → DTO.date（重命名）</li>
 *   <li>PO.foreignNetShares → institutions[0].netBuySell（name="外資"）</li>
 *   <li>PO.investmentTrustNetShares → institutions[1].netBuySell（name="投信"）</li>
 *   <li>PO.dealerNetShares → institutions[2].netBuySell（name="自營商"）</li>
 *   <li>buy/sell 暫填 0（PO 未存明細，Wave 3 TD 補充）</li>
 *   <li>PO.totalInstitutionalNet → DTO.totalNetBuySell（重命名）</li>
 *   <li>source 來自 PO.market（TWSE/OTC）</li>
 * </ul>
 * institutions 順序固定：外資、投信、自營商（前端依此順序渲染）。
 */
@Mapper(componentModel = "spring")
public interface ChipConvertor {

    default ChipDTO toDTO(StockChipPO po) {
        long foreignNet = toLong(po.getForeignNetShares());
        long investmentNet = toLong(po.getInvestmentTrustNetShares());
        long dealerNet = toLong(po.getDealerNetShares());

        // buy/sell 暫填 0（PO 僅存 netShares，Wave 3 補充明細欄位）
        List<InstitutionItem> institutions = List.of(
            new InstitutionItem("外資",      0L, 0L, foreignNet),
            new InstitutionItem("投信",      0L, 0L, investmentNet),
            new InstitutionItem("自營商",    0L, 0L, dealerNet)
        );

        long total = toLong(po.getTotalInstitutionalNet());

        return new ChipDTO(
            po.getStockId(),
            po.getTradeDate(),      // tradeDate → date
            institutions,
            total,                  // totalInstitutionalNet → totalNetBuySell
            po.getMarket()          // source = market
        );
    }

    private static long toLong(java.math.BigDecimal value) {
        return value == null ? 0L : value.longValue();
    }
}
