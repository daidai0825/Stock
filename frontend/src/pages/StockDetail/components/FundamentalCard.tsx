import { type FC } from 'react';
import { Card, Descriptions, Skeleton, Tooltip, Typography } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { StockFundamental } from '@/types/stock';

const { Text } = Typography;

interface FundamentalCardProps {
  fundamental: StockFundamental | undefined;
  isLoading: boolean;
}

export const FundamentalCard: FC<FundamentalCardProps> = ({ fundamental, isLoading }) => {
  const { t } = useTranslation();

  const titleWithTooltip = (
    <span>
      {t('stock.fundamental.title')}
      <Tooltip title={t('stock.fundamental.sourceTooltip')}>
        <InfoCircleOutlined
          style={{ marginLeft: 6, color: '#8c8c8c', fontSize: 13 }}
          aria-label={t('stock.fundamental.sourceTooltip')}
        />
      </Tooltip>
    </span>
  );

  return (
    <Card
      title={titleWithTooltip}
      size="small"
      data-testid="fundamental-card"
    >
      {isLoading ? (
        <Skeleton active paragraph={{ rows: 4 }} />
      ) : fundamental === undefined ? (
        <Text type="secondary">{t('common.empty')}</Text>
      ) : (
        <Descriptions column={2} size="small" bordered>
          <Descriptions.Item label={t('stock.fundamental.eps')} data-testid="fundamental-eps">
            {fundamental.eps}
          </Descriptions.Item>
          {/* 欄位名稱固定為 per（schema-lock §4.3：禁止 perRatio） */}
          <Descriptions.Item label={t('stock.fundamental.per')} data-testid="fundamental-per">
            {fundamental.per}
          </Descriptions.Item>
          {/* 欄位名稱固定為 pbr（schema-lock §4.3：禁止 pbrRatio） */}
          <Descriptions.Item label={t('stock.fundamental.pbr')} data-testid="fundamental-pbr">
            {fundamental.pbr}
          </Descriptions.Item>
          <Descriptions.Item label={t('stock.fundamental.roe')} data-testid="fundamental-roe">
            {fundamental.roe}
          </Descriptions.Item>
          <Descriptions.Item
            label={t('stock.fundamental.reportPeriod')}
            data-testid="fundamental-report-period"
          >
            {fundamental.reportYear} Q{fundamental.reportQuarter}
          </Descriptions.Item>
          <Descriptions.Item
            label={t('stock.fundamental.updatedAt')}
            data-testid="fundamental-updated-at"
          >
            {fundamental.updatedAt}
          </Descriptions.Item>
        </Descriptions>
      )}
    </Card>
  );
};
