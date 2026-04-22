/**
 * MSW Node Server 設定（Vitest 用）
 * 測試環境使用 Node 版本的 MSW（不需要 Service Worker）。
 */

import { setupServer } from 'msw/node';
import { handlers } from './handlers';

export const server = setupServer(...handlers);
