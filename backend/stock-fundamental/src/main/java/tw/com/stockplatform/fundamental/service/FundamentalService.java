package tw.com.stockplatform.fundamental.service;

import tw.com.stockplatform.fundamental.dto.request.FundamentalGetRequest;
import tw.com.stockplatform.fundamental.dto.response.FundamentalDTO;

/**
 * M-FUND 基本面服務介面。
 */
public interface FundamentalService {

    /**
     * 查詢個股 EPS / PER / PBR / ROE（含 Redis 快取，TTL 1 天）。
     */
    FundamentalDTO getFundamental(FundamentalGetRequest request);
}
