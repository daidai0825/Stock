import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { watchlistService } from '@/services/watchlistService';
import type { AddWatchlistRequest, RemoveWatchlistRequest } from '@/types/watchlist';

const WATCHLIST_KEY = ['watchlist'] as const;

export const useWatchlist = () => {
  return useQuery({
    queryKey: WATCHLIST_KEY,
    queryFn: () => watchlistService.list(),
    staleTime: 60_000,
  });
};

export const useAddWatchlist = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (req: AddWatchlistRequest) => watchlistService.add(req),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: WATCHLIST_KEY });
    },
  });
};

export const useRemoveWatchlist = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (req: RemoveWatchlistRequest) => watchlistService.remove(req),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: WATCHLIST_KEY });
    },
  });
};
