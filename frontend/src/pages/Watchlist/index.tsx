import { type FC } from 'react';
import { Link } from 'react-router-dom';
import { Button, Card, Empty, Space, Spin, Table, Tag, Typography, Alert } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useWatchlist } from '@/hooks/useWatchlist';
import type { WatchlistItem } from '@/types/watchlist';
import { buildStockDetailPath } from '@/constants/routes';

const { Title, Text } = Typography;

export const WatchlistPage: FC = () => {
  const { t } = useTranslation();
  const { data, isLoading, isError } = useWatchlist();

  const handleAdd = (): void => {
    // Wave A：暫不開放新增介面，等 Wave B 完整實作
    // 依規範禁止 console.log，改用 console.info（permitted）
    console.info('[Watchlist] add button clicked — Wave B 啟用');
  };

  const columns: ColumnsType<WatchlistItem> = [
    {
      title: t('watchlist.columns.stockCode'),
      dataIndex: 'stockCode',
      key: 'stockCode',
      width: 100,
      // M-FE-WaveB-05：stock code 加超連結，點擊跳至個股詳情頁
      render: (stockCode: string) => (
        <Link to={buildStockDetailPath(stockCode)}>{stockCode}</Link>
      ),
    },
    {
      title: t('watchlist.columns.stockName'),
      dataIndex: 'stockName',
      key: 'stockName',
    },
    {
      title: t('watchlist.columns.market'),
      dataIndex: 'market',
      key: 'market',
      width: 80,
      render: (market: WatchlistItem['market']) => <Tag color={market === 'TSE' ? 'blue' : 'geekblue'}>{market}</Tag>,
    },
    {
      title: t('watchlist.columns.lastPrice'),
      dataIndex: 'lastPrice',
      key: 'lastPrice',
      align: 'right',
      render: (price: number | null) => (price === null ? '-' : price.toFixed(2)),
    },
    {
      title: t('watchlist.columns.changePercent'),
      dataIndex: 'changePercent',
      key: 'changePercent',
      align: 'right',
      render: (pct: number | null) => {
        if (pct === null) return '-';
        const color = pct > 0 ? '#cf1322' : pct < 0 ? '#3f8600' : 'inherit';
        const sign = pct > 0 ? '+' : '';
        return <span style={{ color }}>{`${sign}${pct.toFixed(2)}%`}</span>;
      },
    },
    {
      title: t('watchlist.columns.healthScore'),
      dataIndex: 'healthScore',
      key: 'healthScore',
      align: 'right',
      render: (score: number | null) => (score === null ? <Tag>N/A</Tag> : <Text strong>{score}</Text>),
    },
  ];

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap' }}>
        <Title level={3} style={{ margin: 0 }}>
          {t('watchlist.title')}
        </Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          {t('watchlist.addStock')}
        </Button>
      </div>

      <Alert type="info" showIcon message={t('watchlist.comingSoon')} />

      <Card>
        {isLoading && (
          <div style={{ textAlign: 'center', padding: 24 }}>
            <Spin tip={t('common.loading')} />
          </div>
        )}

        {isError && <Alert type="error" message={t('common.errorRetry')} showIcon />}

        {!isLoading && !isError && (data === undefined || data.length === 0) && (
          <Empty description={t('common.empty')} />
        )}

        {!isLoading && !isError && data !== undefined && data.length > 0 && (
          <Table<WatchlistItem>
            rowKey="watchId"
            dataSource={data}
            columns={columns}
            pagination={false}
            scroll={{ x: 720 }}
            size="middle"
          />
        )}
      </Card>
    </Space>
  );
};

export default WatchlistPage;
