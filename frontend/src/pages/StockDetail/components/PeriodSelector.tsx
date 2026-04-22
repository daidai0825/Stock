import { type FC } from 'react';
import { Radio } from 'antd';
import { useTranslation } from 'react-i18next';
import type { KLinePeriod } from '@/types/stock';

interface PeriodSelectorProps {
  value: KLinePeriod;
  onChange: (period: KLinePeriod) => void;
}

const PERIODS: KLinePeriod[] = ['daily', 'weekly', 'monthly'];

export const PeriodSelector: FC<PeriodSelectorProps> = ({ value, onChange }) => {
  const { t } = useTranslation();

  return (
    <Radio.Group
      value={value}
      onChange={(e) => onChange(e.target.value as KLinePeriod)}
      optionType="button"
      buttonStyle="solid"
      size="small"
      aria-label="K 線週期切換"
      data-testid="period-selector"
    >
      {PERIODS.map((p) => (
        <Radio.Button key={p} value={p} data-testid={`period-${p}`}>
          {t(`stock.period.${p}`)}
        </Radio.Button>
      ))}
    </Radio.Group>
  );
};
