import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import zhTW from './zh-TW.json';
import en from './en.json';

// 預設繁體中文（台灣用語），符合 conversation.md
void i18n.use(initReactI18next).init({
  resources: {
    'zh-TW': { translation: zhTW },
    en: { translation: en },
  },
  lng: 'zh-TW',
  fallbackLng: 'zh-TW',
  interpolation: { escapeValue: false },
  returnNull: false,
});

export default i18n;
