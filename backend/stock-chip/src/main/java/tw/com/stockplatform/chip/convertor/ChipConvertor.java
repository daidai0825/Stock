package tw.com.stockplatform.chip.convertor;

import org.mapstruct.Mapper;
import tw.com.stockplatform.chip.dto.response.ChipDTO;
import tw.com.stockplatform.domain.po.StockChipPO;

/**
 * 籌碼 PO ↔ DTO 轉換（MapStruct）。
 */
@Mapper(componentModel = "spring")
public interface ChipConvertor {

    ChipDTO toDTO(StockChipPO po);
}
