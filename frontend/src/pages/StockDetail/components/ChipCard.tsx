import { type FC } from 'react';
import { Card, Skeleton, Table, Tooltip, Typography } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useTranslation } from 'react-i18next';
import type { Chip, ChipInstitutionEntry } from '@/types/stock';

const { Text } = Typography;

interface ChipCardProps {
  chip: Chip | undefined;
  isLoading: boolean;
}

/** 以千分位格式化整數 */
const fmtNumber = (n: number): string => n.toLocaleString();

/** 買賣超顏色：正紅負綠（台灣股市慣例） */
const netColor = (val: number): string => {
  if (val > 0) return '#cf1322';
  if (val < 0) return '#3f8600';
  return 'inherit';
};

export const ChipCard: FC<ChipCardProps> = ({ chip, isLoading }) => {
  const { t } = useTranslation();

  const titleWithTooltip = (
    <span>
      {t('stock.chip.title')}
      <Tooltip title={t('stock.chip.sourceTooltip')}>
        <InfoCircleOutlined
          style={{ marginLeft: 6, color: '#8c8c8c', fontSize: 13 }}
          aria-label={t('stock.chip.sourceTooltip')}
        />
      </Tooltip>
    </span>
  );

  const columns: ColumnsType<ChipInstitutionEntry> = [
    {
      title: t('stock.chip.institution'),
      dataIndex: 'name',
      key: 'name',
      width: 80,
    },
    {
      title: t('stock.chip.netBuySell'),
      dataIndex: 'netBuySell',
      key: 'netBuySell',
      align: 'right',
      render: (val: number) => (
        <span style={{ color: netColor(val) }}>
          {val >= 0 ? '+' : ''}{fmtNumber(val)}
        </span>
      ),
    },
    {
      title: t('stock.chip.buy'),
      dataIndex: 'buy',
      key: 'buy',
      align: 'right',
      render: (val: number) => fmtNumber(val),
    },
    {
      title: t('stock.chip.sell'),
      dataIndex: 'sell',
      key: 'sell',
      align: 'right',
      render: (val: number) => fmtNumber(val),
    },
  ];

  const extra = chip !== undefined ? (
    <Text type="secondary" style={{ fontSize: 12 }}>
      {t('stock.chip.date')}：{chip.date}
    </Text>
  ) : null;

  return (
    <Card
      title={titleWithTooltip}
      extra={extra}
      size="small"
      data-testid="chip-card"
    >
      {isLoading ? (
        <Skeleton active paragraph={{ rows: 3 }} />
      ) : chip === undefined ? (
        <Text type="secondary">{t('common.empty')}</Text>
      ) : (
        <Table<ChipInstitutionEntry>
          dataSource={chip.institutions}
          columns={columns}
          rowKey="name"
          pagination={false}
          size="small"
          aria-label={t('stock.chip.title')}
        />
      )}
    </Card>
  );
};
