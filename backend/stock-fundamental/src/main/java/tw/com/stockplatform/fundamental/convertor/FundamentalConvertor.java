package tw.com.stockplatform.fundamental.convertor;

import org.mapstruct.Mapper;
import tw.com.stockplatform.domain.po.StockFundamentalPO;
import tw.com.stockplatform.fundamental.dto.response.FundamentalDTO;

/**
 * 基本面 PO ↔ DTO 轉換（schema-lock v1.0 §4.3 對齊）。
 * <p>
 * 仲裁 D2：
 * <ul>
 *   <li>PO.perRatio → DTO.per（欄位名去掉 Ratio 字尾）</li>
 *   <li>PO.pbrRatio → DTO.pbr（欄位名去掉 Ratio 字尾）</li>
 *   <li>新增 source 固定值 "MOPS"</li>
 *   <li>新增 updatedAt 來自 PO.updatedAt</li>
 * </ul>
 * 由於欄位命名不一致，無法讓 MapStruct 自動映射，改用 default 手寫轉換。
 */
@Mapper(componentModel = "spring")
public interface FundamentalConvertor {

    default FundamentalDTO toDTO(StockFundamentalPO po) {
        return new FundamentalDTO(
            po.getStockId(),
            po.getStockName(),
            po.getEps(),
            po.getPerRatio(),       // perRatio → per
            po.getPbrRatio(),       // pbrRatio → pbr
            po.getRoe(),
            po.getReportYear(),
            po.getReportQuarter(),
            po.getUpdatedAt(),
            "MOPS"                  // source 固定值
        );
    }
}
