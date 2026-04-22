import dayjs from 'dayjs';
import utc from 'dayjs/plugin/utc';
import timezone from 'dayjs/plugin/timezone';
import 'dayjs/locale/zh-tw';

dayjs.extend(utc);
dayjs.extend(timezone);
dayjs.locale('zh-tw');

/**
 * 統一時區：GMT+8（Asia/Taipei）
 * 對應 system-design.md：Fixed Timezone Architecture
 */
const TZ = 'Asia/Taipei';

export const formatDateTime = (iso: string | null | undefined): string => {
  if (iso === null || iso === undefined || iso === '') {
    return '-';
  }
  return dayjs(iso).tz(TZ).format('YYYY-MM-DD HH:mm:ss');
};

export const formatDate = (iso: string | null | undefined): string => {
  if (iso === null || iso === undefined || iso === '') {
    return '-';
  }
  return dayjs(iso).tz(TZ).format('YYYY-MM-DD');
};

export const currentYear = (): number => dayjs().tz(TZ).year();
