import { useQuery } from '@tanstack/react-query';
import { stockService } from '@/services/stockService';
import type { StockQuote } from '@/types/stock';

/**
 * 即時行情 hook
 *
 * staleTime / refetchInterval 固定 30s（Wave B 簡化版，不判斷盤中/盤後）。
 * Wave C 可依市場時間動態切換 refetchInterval。
 */
export const useStockQuote = (stockId: string) =>
  useQuery<StockQuote, Error>({
    queryKey: ['stockQuote', stockId],
    queryFn: () => stockService.getQuote(stockId),
    enabled: stockId.length > 0,
    staleTime: 30_000,
    refetchInterval: 30_000,
  });
