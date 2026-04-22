/**
 * MSW Browser Worker 設定（Wave B）
 * 在 dev 環境啟動 Service Worker 攔截 API 請求。
 *
 * 使用方式：
 *   main.tsx 在 VITE_MSW_ENABLED=true 時動態 import 此檔並呼叫 worker.start()。
 *
 * Service Worker 檔案（mockServiceWorker.js）需由 npx msw init 產生：
 *   npx msw init public/ --save
 */

import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';

export const worker = setupWorker(...handlers);
