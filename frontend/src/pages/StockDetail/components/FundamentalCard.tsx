import { type FC } from 'react';
import { Card, Descriptions, Skeleton, Tooltip, Typography } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { Fundamental } from '@/types/stock';

const { Text } = Typography;

interface FundamentalCardProps {
  fundamental: Fundamental | undefined;
  isLoading: boolean;
}

/** 將 null 值轉換為顯示用字串 */
const display = (val: string | null): string => (val !== null ? val : '-');

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
            {display(fundamental.eps)}
          </Descriptions.Item>
          <Descriptions.Item label={t('stock.fundamental.per')} data-testid="fundamental-per">
            {display(fundamental.per)}
          </Descriptions.Item>
          <Descriptions.Item label={t('stock.fundamental.pbr')} data-testid="fundamental-pbr">
            {display(fundamental.pbr)}
          </Descriptions.Item>
          <Descriptions.Item label={t('stock.fundamental.roe')} data-testid="fundamental-roe">
            {display(fundamental.roe)}
          </Descriptions.Item>
          <Descriptions.Item
            label={t('stock.fundamental.updatedAt')}
            span={2}
            data-testid="fundamental-updated-at"
          >
            {fundamental.updatedAt}
          </Descriptions.Item>
        </Descriptions>
      )}
    </Card>
  );
};
