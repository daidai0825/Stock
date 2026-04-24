import { useQuery } from '@tanstack/react-query';
import { stockService } from '@/services/stockService';
import type { StockFundamental } from '@/types/stock';

/**
 * 基本面資料 hook
 *
 * 基本面資料每季更新，staleTime 5 分鐘即可。
 * Round 2 起以 StockFundamental 為規範名稱（Fundamental 別名僅供向下相容）。
 */
export const useFundamental = (stockId: string) =>
  useQuery<StockFundamental, Error>({
    queryKey: ['fundamental', stockId],
    queryFn: () => stockService.getFundamental(stockId),
    enabled: stockId.length > 0,
    staleTime: 5 * 60_000,
  });
