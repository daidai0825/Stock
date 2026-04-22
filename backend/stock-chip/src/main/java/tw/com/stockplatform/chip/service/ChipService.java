package tw.com.stockplatform.chip.service;

import tw.com.stockplatform.chip.dto.request.ChipGetRequest;
import tw.com.stockplatform.chip.dto.response.ChipDTO;

/**
 * M-CHIP 籌碼服務介面。
 */
public interface ChipService {

    /**
     * 查詢個股最新三大法人買賣超（含 Redis 快取，TTL 1 天）。
     */
    ChipDTO getChip(ChipGetRequest request);
}
