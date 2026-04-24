/**
 * useWatchlist Hooks（Wave 3）
 *
 * 提供：
 *   useWatchlistQuery    — 取得自選股清單（with 樂觀更新支援）
 *   useAddStockMutation  — 新增自選股（樂觀更新 + rollback）
 *   useRemoveStockMutation — 移除自選股（樂觀更新 + rollback）
 *
 * 樂觀更新策略：
 *   - add：先在 cache 插入 placeholder item，API 成功後再 invalidate
 *   - remove：先從 cache 移除，API 失敗時 rollback 還原
 *
 * staleTime：30s（行情資料每 30s 允許 background refetch）
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { watchlistService } from '@/services/watchlistService';
import type {
  AddWatchlistRequest,
  RemoveWatchlistRequest,
  WatchlistListItem,
  WatchlistListResult,
} from '@/types/watchlist';

export const WATCHLIST_QUERY_KEY = ['watchlist', 'list'] as const;

/** 自選股清單 Query */
export const useWatchlistQuery = () =>
  useQuery({
    queryKey: WATCHLIST_QUERY_KEY,
    queryFn: () => watchlistService.list(),
    staleTime: 30_000,
    select: (data: WatchlistListResult) => data.items,
  });

/** 新增自選股 Mutation（樂觀更新） */
export const useAddStockMutation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (req: AddWatchlistRequest) => watchlistService.add(req),
    onMutate: async (req) => {
      await queryClient.cancelQueries({ queryKey: WATCHLIST_QUERY_KEY });
      const previous = queryClient.getQueryData<WatchlistListResult>(WATCHLIST_QUERY_KEY);

      // 樂觀插入 placeholder（不帶行情資料）
      const placeholder: WatchlistListItem = {
        itemId: `optimistic-${req.stockId}-${Date.now()}`,
        stockId: req.stockId,
        stockName: req.stockId,
        market: 'TWSE',
        createdAt: new Date().toISOString(),
        quote: null,
        quoteError: null,
      };

      if (previous !== undefined) {
        queryClient.setQueryData<WatchlistListResult>(WATCHLIST_QUERY_KEY, {
          items: [...previous.items, placeholder],
          total: previous.total + 1,
        });
      }

      return { previous };
    },
    onError: (_err, _req, context) => {
      if (context?.previous !== undefined) {
        queryClient.setQueryData(WATCHLIST_QUERY_KEY, context.previous);
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: WATCHLIST_QUERY_KEY });
    },
  });
};

/** 移除自選股 Mutation（樂觀更新） */
export const useRemoveStockMutation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (req: RemoveWatchlistRequest) => watchlistService.remove(req),
    onMutate: async (req) => {
      await queryClient.cancelQueries({ queryKey: WATCHLIST_QUERY_KEY });
      const previous = queryClient.getQueryData<WatchlistListResult>(WATCHLIST_QUERY_KEY);

      if (previous !== undefined) {
        queryClient.setQueryData<WatchlistListResult>(WATCHLIST_QUERY_KEY, {
          items: previous.items.filter((item) => item.stockId !== req.stockId),
          total: Math.max(0, previous.total - 1),
        });
      }

      return { previous };
    },
    onError: (_err, _req, context) => {
      if (context?.previous !== undefined) {
        queryClient.setQueryData(WATCHLIST_QUERY_KEY, context.previous);
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: WATCHLIST_QUERY_KEY });
    },
  });
};
