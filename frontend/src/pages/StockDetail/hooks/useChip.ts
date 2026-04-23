import { useQuery } from '@tanstack/react-query';
import { stockService } from '@/services/stockService';
import type { StockChip } from '@/types/stock';

/**
 * 籌碼（三大法人）資料 hook
 *
 * 每日盤後更新，staleTime 5 分鐘。
 * Round 2 起以 StockChip 為規範名稱（Chip 別名僅供向下相容）。
 */
export const useChip = (stockId: string) =>
  useQuery<StockChip, Error>({
    queryKey: ['chip', stockId],
    queryFn: () => stockService.getChip(stockId),
    enabled: stockId.length > 0,
    staleTime: 5 * 60_000,
  });
