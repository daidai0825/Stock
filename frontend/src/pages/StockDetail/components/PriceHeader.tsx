import { type FC } from 'react';
import { Space, Skeleton, Tag, Typography } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined, MinusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { StockQuote } from '@/types/stock';
import { formatDateTime } from '@/utils/datetime';

const { Title, Text } = Typography;

interface PriceHeaderProps {
  quote: StockQuote | undefined;
  isLoading: boolean;
}

/**
 * 判斷漲跌方向。
 * m-FE-WaveB-06：改用純字串比較，避免 parseFloat 對 BigDecimal 字串的精度問題。
 * 此函式只決定顯示方向，不參與數值計算，字串比較已足夠。
 */
const getPriceDirection = (change: string): 'up' | 'down' | 'flat' => {
  const trimmed = change.trim();
  if (trimmed.startsWith('+') && trimmed !== '+0' && trimmed !== '+0.00') return 'up';
  if (trimmed.startsWith('-')) return 'down';
  return 'flat';
};

export const PriceHeader: FC<PriceHeaderProps> = ({ quote, isLoading }) => {
  const { t } = useTranslation();

  if (isLoading) {
    return <Skeleton active paragraph={{ rows: 2 }} />;
  }

  if (quote === undefined) {
    return (
      <Text type="secondary" data-testid="price-header-empty">
        {t('stock.loadingQuote')}
      </Text>
    );
  }

  const direction = getPriceDirection(quote.change);

  const changeColor =
    direction === 'up' ? '#cf1322' : direction === 'down' ? '#3f8600' : undefined;

  const changeIcon =
    direction === 'up' ? (
      <ArrowUpOutlined />
    ) : direction === 'down' ? (
      <ArrowDownOutlined />
    ) : (
      <MinusOutlined />
    );

  return (
    <Space direction="vertical" size={4} data-testid="price-header">
      <Space align="baseline" size={8}>
        <Title
          level={4}
          style={{ margin: 0 }}
          aria-label={`${quote.stockId} ${quote.stockName}`}
        >
          {quote.stockId}・{quote.stockName}
        </Title>
      </Space>

      <Space size={16} align="baseline" wrap>
        <Title
          level={2}
          style={{ margin: 0, color: changeColor }}
          aria-label={`${t('stock.price')} ${quote.price}`}
          data-testid="price-value"
        >
          {quote.price}
        </Title>

        <Tag
          color={direction === 'up' ? 'red' : direction === 'down' ? 'green' : 'default'}
          icon={changeIcon}
          style={{ fontSize: 14 }}
          aria-label={`${t('stock.change')} ${quote.change}`}
          data-testid="price-change"
        >
          {quote.change} ({quote.changePercent}%)
        </Tag>
      </Space>

      <Text type="secondary" style={{ fontSize: 12 }}>
        {t('stock.volume')}：{quote.volume.toLocaleString()}・
        {t('stock.updatedAt')}：{formatDateTime(quote.updatedAt)}
      </Text>
    </Space>
  );
};
