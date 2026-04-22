/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_LOG_LEVEL: 'debug' | 'info' | 'warn' | 'error';
  readonly VITE_APP_ENV: 'local' | 'dev' | 'uat' | 'stg' | 'preProd' | 'prod';
  /** MSW Service Worker dev fallback；需手動設為 'true' 才啟用 */
  readonly VITE_MSW_ENABLED: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
