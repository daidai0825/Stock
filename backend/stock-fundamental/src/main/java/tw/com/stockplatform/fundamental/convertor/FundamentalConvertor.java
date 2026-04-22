package tw.com.stockplatform.fundamental.convertor;

import org.mapstruct.Mapper;
import tw.com.stockplatform.domain.po.StockFundamentalPO;
import tw.com.stockplatform.fundamental.dto.response.FundamentalDTO;

/**
 * 基本面 PO ↔ DTO 轉換（MapStruct）。
 */
@Mapper(componentModel = "spring")
public interface FundamentalConvertor {

    FundamentalDTO toDTO(StockFundamentalPO po);
}
