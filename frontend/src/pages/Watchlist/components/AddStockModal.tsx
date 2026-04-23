/**
 * AddStockModal：搜尋並加入自選股的 Modal
 *
 * 功能：
 *   1. 搜尋框（debounce 250ms，呼叫 /api/v1/stock/search）
 *   2. 下拉搜尋結果（最多 10 筆，含股票代號、中文名、市場別）
 *   3. 點選股票 → 呼叫 watchlist/add
 *   4. 成功 / 失敗顯示對應 Toast
 *
 * 規範：
 *   - 禁止 hard-code 文字（用 i18n）
 *   - 搜尋框最大輸入 50 字元（schema-lock §3.1）
 */

import { type FC, useState, useRef, useCallback } from 'react';
import { Modal, Input, List, Tag, Spin, Empty, Typography, notification } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { stockSearchService, watchlistService } from '@/services/watchlistService';
import { WATCHLIST_QUERY_KEY } from '@/hooks/useWatchlist';
import type { StockSearchItem, AddWatchlistRequest } from '@/types/watchlist';
import { BusinessError } from '@/types/api';

const { Text } = Typography;

const DEBOUNCE_MS = 250;
const MAX_KEYWORD_LEN = 50;

interface AddStockModalProps {
  open: boolean;
  onClose: () => void;
}

export const AddStockModal: FC<AddStockModalProps> = ({ open, onClose }) => {
  const { t } = useTranslation();
  const [keyword, setKeyword] = useState('');
  const [debouncedKeyword, setDebouncedKeyword] = useState('');
  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const queryClient = useQueryClient();

  // 搜尋 Query（只有 debouncedKeyword 非空才啟用）
  const { data: searchResult, isFetching: isSearching } = useQuery({
    queryKey: ['stock', 'search', debouncedKeyword],
    queryFn: () => stockSearchService.search(debouncedKeyword),
    enabled: debouncedKeyword.trim().length > 0,
    staleTime: 30_000,
  });

  // 加入 Mutation
  const addMutation = useMutation({
    mutationFn: (req: AddWatchlistRequest) => watchlistService.add(req),
    onSuccess: (data) => {
      notification.success({
        message: t('watchlist.addSuccess'),
        description: `${data.stockName}（${data.stockId}）`,
      });
      void queryClient.invalidateQueries({ queryKey: WATCHLIST_QUERY_KEY });
      handleClose();
    },
    onError: (error) => {
      const code = error instanceof BusinessError ? error.code : 9999;
      notification.error({
        message: t(`errors.${String(code)}`, { defaultValue: t('errors.9999') }),
      });
    },
  });

  const handleKeywordChange = useCallback((value: string) => {
    const trimmed = value.slice(0, MAX_KEYWORD_LEN);
    setKeyword(trimmed);

    if (debounceTimerRef.current !== null) {
      clearTimeout(debounceTimerRef.current);
    }
    debounceTimerRef.current = setTimeout(() => {
      setDebouncedKeyword(trimmed);
    }, DEBOUNCE_MS);
  }, []);

  const handleSelectStock = useCallback(
    (item: StockSearchItem) => {
      if (addMutation.isPending) return;
      addMutation.mutate({ stockId: item.stockId });
    },
    [addMutation],
  );

  const handleClose = useCallback(() => {
    setKeyword('');
    setDebouncedKeyword('');
    if (debounceTimerRef.current !== null) {
      clearTimeout(debounceTimerRef.current);
    }
    onClose();
  }, [onClose]);

  const items = searchResult?.items ?? [];
  const showNoResult =
    debouncedKeyword.trim().length > 0 && !isSearching && items.length === 0;

  return (
    <Modal
      title={t('watchlist.search.modalTitle')}
      open={open}
      onCancel={handleClose}
      footer={null}
      destroyOnClose
      aria-label={t('watchlist.search.modalTitle')}
    >
      <Input
        prefix={<SearchOutlined aria-hidden="true" />}
        placeholder={t('watchlist.search.placeholder')}
        value={keyword}
        onChange={(e) => handleKeywordChange(e.target.value)}
        maxLength={MAX_KEYWORD_LEN}
        allowClear
        autoFocus
        aria-label={t('watchlist.search.placeholder')}
      />

      <div style={{ marginTop: 12, minHeight: 80 }}>
        {isSearching && (
          <div style={{ textAlign: 'center', padding: 24 }}>
            <Spin aria-label={t('watchlist.search.searching')} />
          </div>
        )}

        {showNoResult && (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={t('watchlist.search.noResult')}
          />
        )}

        {!isSearching && items.length > 0 && (
          <List
            dataSource={items}
            renderItem={(item) => (
              <List.Item
                key={item.stockId}
                onClick={() => handleSelectStock(item)}
                style={{ cursor: 'pointer', padding: '8px 0' }}
                role="option"
                aria-selected={false}
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    handleSelectStock(item);
                  }
                }}
              >
                <List.Item.Meta
                  title={
                    <span>
                      <Text strong>{item.stockId}</Text>
                      {'  '}
                      <Text>{item.stockName}</Text>
                    </span>
                  }
                  description={
                    item.stockNameEn !== null ? (
                      <Text type="secondary">{item.stockNameEn}</Text>
                    ) : null
                  }
                />
                <Tag color={item.market === 'TWSE' ? 'blue' : 'geekblue'}>
                  {item.market === 'TWSE' ? '上市' : '上櫃'}
                </Tag>
              </List.Item>
            )}
          />
        )}
      </div>
    </Modal>
  );
};
