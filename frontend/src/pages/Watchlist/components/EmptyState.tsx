/**
 * Watchlist 空狀態元件
 *
 * 顯示條件：自選股清單為空時
 * CTA：引導使用者新增第一檔自選股
 */

import { type FC } from 'react';
import { Button, Empty } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

interface EmptyStateProps {
  onAddClick: () => void;
}

export const EmptyState: FC<EmptyStateProps> = ({ onAddClick }) => {
  const { t } = useTranslation();

  return (
    <Empty
      description={
        <span>
          <strong>{t('watchlist.emptyTitle')}</strong>
          <br />
          {t('watchlist.emptyDesc')}
        </span>
      }
    >
      <Button
        type="primary"
        icon={<PlusOutlined />}
        onClick={onAddClick}
        aria-label={t('watchlist.emptyAction')}
      >
        {t('watchlist.emptyAction')}
      </Button>
    </Empty>
  );
};
