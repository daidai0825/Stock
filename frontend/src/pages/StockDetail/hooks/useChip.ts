import { useQuery } from '@tanstack/react-query';
import { stockService } from '@/services/stockService';
import type { Chip } from '@/types/stock';

/**
 * 籌碼（三大法人）資料 hook
 *
 * 每日盤後更新，staleTime 5 分鐘。
 */
export const useChip = (stockId: string) =>
  useQuery<Chip, Error>({
    queryKey: ['chip', stockId],
    queryFn: () => stockService.getChip(stockId),
    enabled: stockId.length > 0,
    staleTime: 5 * 60_000,
  });
