import { type FC } from 'react';
import { Card, Skeleton, Table, Tooltip, Typography } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useTranslation } from 'react-i18next';
import type { InstitutionItem, StockChip } from '@/types/stock';

const { Text } = Typography;

interface ChipCardProps {
  chip: StockChip | undefined;
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

/**
 * 三大法人籌碼卡片（Wave B Round 2 對齊 Schema Lock）
 *
 * 固定 3 筆，順序由後端保證：外資、投信、自營商（schema-lock §5.3）。
 * 前端依此順序渲染，不依名稱動態排序。
 * totalNetBuySell 由後端計算（schema-lock §5.3），前端直接顯示。
 */
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

  const columns: ColumnsType<InstitutionItem> = [
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
        <>
          <Table<InstitutionItem>
            dataSource={chip.institutions}
            columns={columns}
            rowKey="name"
            pagination={false}
            size="small"
            aria-label={t('stock.chip.title')}
          />
          {/* 三大法人合計（totalNetBuySell 由後端計算，schema-lock §5.3） */}
          <Text
            type="secondary"
            style={{ fontSize: 12, display: 'block', marginTop: 8, textAlign: 'right' }}
            data-testid="chip-total-net"
          >
            {t('stock.chip.totalNetBuySell')}：
            <span style={{ color: netColor(chip.totalNetBuySell) }}>
              {chip.totalNetBuySell >= 0 ? '+' : ''}{fmtNumber(chip.totalNetBuySell)}
            </span>
          </Text>
        </>
      )}
    </Card>
  );
};
