import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import zhTW from './zh-TW.json';
import en from './en.json';

const SUPPORTED_LANGS = ['zh-TW', 'en'] as const;
type SupportedLang = (typeof SUPPORTED_LANGS)[number];

const isSupportedLang = (val: unknown): val is SupportedLang =>
  SUPPORTED_LANGS.includes(val as SupportedLang);

/**
 * 安全讀取初始語言，優先序：
 * 1. URL query param `?lang=` — QA / Playwright 確定性覆蓋，不依賴 localStorage 時序
 * 2. localStorage `i18nextLng` — 使用者語言偏好持久化
 * 3. 預設 `zh-TW` — 符合台灣股市為主要市場
 *
 * 只接受 SUPPORTED_LANGS 白名單，非法值直接降級到下一個來源。
 */
export const resolveInitialLang = (): SupportedLang => {
  // 1. query param（?lang=en）
  if (typeof window !== 'undefined') {
    const params = new URLSearchParams(window.location.search);
    const queryLang = params.get('lang');
    if (isSupportedLang(queryLang)) return queryLang;
  }

  // 2. localStorage
  if (typeof window !== 'undefined') {
    const stored = localStorage.getItem('i18nextLng');
    if (isSupportedLang(stored)) return stored;
  }

  // 3. 預設 zh-TW
  return 'zh-TW';
};

void i18n.use(initReactI18next).init({
  resources: {
    'zh-TW': { translation: zhTW },
    en: { translation: en },
  },
  lng: resolveInitialLang(),
  fallbackLng: 'zh-TW',
  interpolation: { escapeValue: false },
  returnNull: false,
});

export default i18n;
