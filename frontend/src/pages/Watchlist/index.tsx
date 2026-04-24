/**
 * WatchlistPage（Wave 3 W1）
 *
 * 功能：
 *   - 顯示自選股清單（含即時報價快照）
 *   - 移除自選股（Popconfirm + 樂觀更新）
 *   - 新增自選股（AddStockModal）
 *   - 空狀態（EmptyState）
 *
 * 規範：
 *   - 正漲顯示紅色（#cf1322），下跌顯示綠色（#3f8600）——台股慣例
 *   - 所有文字使用 i18n，禁止 hard-code
 *   - 移除前端必須先登入（ProtectedRoute 已確保）
 */

import { type FC, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Alert,
  Button,
  Card,
  Popconfirm,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useWatchlistQuery, useRemoveStockMutation } from '@/hooks/useWatchlist';
import type { WatchlistListItem } from '@/types/watchlist';
import { buildStockDetailPath } from '@/constants/routes';
import { BusinessError } from '@/types/api';
import { AddStockModal } from './components/AddStockModal';
import { EmptyState } from './components/EmptyState';

const { Title } = Typography;

/** 台股漲跌顏色慣例：正值紅色、負值綠色 */
const getChangeColor = (raw: string): string => {
  if (raw.startsWith('+')) return '#cf1322';
  if (raw.startsWith('-')) return '#3f8600';
  return 'inherit';
};

/** 安全取得行情欄位；null quote → '-' */
const renderQuoteField = (
  item: WatchlistListItem,
  field: keyof NonNullable<WatchlistListItem['quote']>,
): string => {
  if (item.quote === null) return '-';
  const value = item.quote[field];
  return String(value);
};

export const WatchlistPage: FC = () => {
  const { t } = useTranslation();
  const { data: items, isLoading, isError, error } = useWatchlistQuery();
  const removeMutation = useRemoveStockMutation();
  const [modalOpen, setModalOpen] = useState(false);

  const errorMessage: string = (() => {
    if (!isError || error === null) return t('common.errorRetry');
    if (error instanceof BusinessError) {
      return t(`errors.${String(error.code)}`, { defaultValue: error.message });
    }
    return t('errors.network');
  })();

  const columns: ColumnsType<WatchlistListItem> = [
    {
      title: t('watchlist.columns.stockId'),
      dataIndex: 'stockId',
      key: 'stockId',
      width: 90,
      render: (stockId: string) => (
        <Link to={buildStockDetailPath(stockId)}>{stockId}</Link>
      ),
    },
    {
      title: t('watchlist.columns.stockName'),
      dataIndex: 'stockName',
      key: 'stockName',
      render: (name: string, record: WatchlistListItem) => (
        <Link to={buildStockDetailPath(record.stockId)}>{name}</Link>
      ),
    },
    {
      title: t('watchlist.columns.market'),
      dataIndex: 'market',
      key: 'market',
      width: 80,
      render: (market: WatchlistListItem['market']) => (
        <Tag color={market === 'TWSE' ? 'blue' : 'geekblue'}>
          {market === 'TWSE' ? '上市' : '上櫃'}
        </Tag>
      ),
    },
    {
      title: t('watchlist.columns.price'),
      key: 'price',
      align: 'right',
      render: (_: unknown, record: WatchlistListItem) => renderQuoteField(record, 'price'),
    },
    {
      title: t('watchlist.columns.change'),
      key: 'change',
      align: 'right',
      render: (_: unknown, record: WatchlistListItem) => {
        if (record.quote === null) return '-';
        const raw = record.quote.change;
        return <span style={{ color: getChangeColor(raw) }}>{raw}</span>;
      },
    },
    {
      title: t('watchlist.columns.changePercent'),
      key: 'changePercent',
      align: 'right',
      render: (_: unknown, record: WatchlistListItem) => {
        if (record.quote === null) return '-';
        const raw = record.quote.changePercent;
        const display = raw.startsWith('+') || raw.startsWith('-') ? `${raw}%` : `${raw}%`;
        return <span style={{ color: getChangeColor(raw) }}>{display}</span>;
      },
    },
    {
      title: t('watchlist.columns.volume'),
      key: 'volume',
      align: 'right',
      render: (_: unknown, record: WatchlistListItem) => {
        if (record.quote === null) return '-';
        return record.quote.volume.toLocaleString();
      },
    },
    {
      title: t('watchlist.columns.actions'),
      key: 'actions',
      width: 80,
      align: 'center',
      render: (_: unknown, record: WatchlistListItem) => (
        <Popconfirm
          title={t('watchlist.removeConfirm', {
            stockName: record.stockName,
            stockId: record.stockId,
          })}
          onConfirm={() => {
            removeMutation.mutate({ stockId: record.stockId });
          }}
          okText={t('common.confirm')}
          cancelText={t('common.cancel')}
        >
          <Button
            type="text"
            danger
            icon={<DeleteOutlined />}
            loading={removeMutation.isPending && removeMutation.variables?.stockId === record.stockId}
            aria-label={t('watchlist.removeStock', { stockName: record.stockName })}
          >
            {t('watchlist.removeStock', { stockName: record.stockName })}
          </Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 8,
        }}
      >
        <Title level={3} style={{ margin: 0 }}>
          {t('watchlist.title')}
        </Title>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => setModalOpen(true)}
          aria-label={t('watchlist.addStock')}
        >
          {t('watchlist.addStock')}
        </Button>
      </div>

      <Card>
        {isLoading && (
          <div
            role="status"
            aria-label={t('common.loading')}
            style={{ textAlign: 'center', padding: 24 }}
          >
            <Spin />
            <div>{t('common.loading')}</div>
          </div>
        )}

        {isError && (
          <Alert type="error" message={errorMessage} showIcon />
        )}

        {!isLoading && !isError && (items === undefined || items.length === 0) && (
          <EmptyState onAddClick={() => setModalOpen(true)} />
        )}

        {!isLoading && !isError && items !== undefined && items.length > 0 && (
          <Table<WatchlistListItem>
            rowKey="itemId"
            dataSource={items}
            columns={columns}
            pagination={false}
            scroll={{ x: 800 }}
            size="middle"
          />
        )}
      </Card>

      <AddStockModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
      />
    </Space>
  );
};

export default WatchlistPage;
