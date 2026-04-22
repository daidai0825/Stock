import { useQuery } from '@tanstack/react-query';
import { stockService } from '@/services/stockService';
import type { Fundamental } from '@/types/stock';

/**
 * 基本面資料 hook
 *
 * 基本面資料每季更新，staleTime 5 分鐘即可。
 */
export const useFundamental = (stockId: string) =>
  useQuery<Fundamental, Error>({
    queryKey: ['fundamental', stockId],
    queryFn: () => stockService.getFundamental(stockId),
    enabled: stockId.length > 0,
    staleTime: 5 * 60_000,
  });
