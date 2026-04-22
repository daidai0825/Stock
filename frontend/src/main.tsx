import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import './styles/global.css';

/**
 * 條件啟動 MSW Service Worker（M-FE-WaveB-02）。
 *
 * 啟用條件：
 *   - import.meta.env.DEV === true（Vite dev server）
 *   - VITE_MSW_ENABLED === 'true'（需在 .env.local 手動 opt-in）
 *
 * 設計要點：
 * - 使用 dynamic import，確保 msw/browser 程式碼不進入 prod bundle
 *   （Vite tree-shaking 會依 import.meta.env.DEV 分析靜態分支）
 * - onUnhandledRequest: 'bypass' → 未攔截的請求直接放行（不 warn）
 * - 非同步啟動完成後才 mount React tree，避免首次 API 請求漏 mock
 */
async function enableMocks(): Promise<void> {
  if (import.meta.env.DEV && import.meta.env.VITE_MSW_ENABLED === 'true') {
    const { worker } = await import('./mocks/browser');
    await worker.start({ onUnhandledRequest: 'bypass' });
  }
}

const rootElement = document.getElementById('root');
if (rootElement === null) {
  throw new Error('找不到 #root 容器');
}

void enableMocks().then(() => {
  createRoot(rootElement).render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
});
