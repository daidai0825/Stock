/**
 * Playwright E2E 測試配置
 *
 * 涵蓋 Wave 2（M-QUOTE / M-FUND / M-CHIP + StockDetail 前端頁面）
 *
 * 跨瀏覽器：Chromium（桌面）、Firefox（桌面）、WebKit（桌面 + 行動模擬）
 * 測試環境：前端 dev server（Vite，port 5173）搭配 MSW mock
 *
 * 啟動方式：
 *   VITE_MSW_ENABLED=true npx playwright test
 *
 * 注意：
 * - 前端需啟動 MSW（VITE_MSW_ENABLED=true）才能跑 E2E（後端 API 尚在開發）
 * - Wave 3 後端穩定後可改 baseURL 指向真實環境
 */

import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: true,
  forbidOnly: process.env['CI'] !== undefined,
  retries: process.env['CI'] !== undefined ? 2 : 0,
  workers: process.env['CI'] !== undefined ? 1 : undefined,
  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['list'],
  ],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 10_000,
    navigationTimeout: 30_000,
  },

  projects: [
    // ─── Chromium（桌面）───────────────────────────────────────────────────
    {
      name: 'chromium-desktop',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 1440, height: 900 },
        channel: 'chromium',
      },
    },

    // ─── Firefox（桌面）─────────────────────────────────────────────────────
    {
      name: 'Firefox',
      use: { ...devices['Desktop Firefox'] },
    },

    // ─── WebKit（桌面）──────────────────────────────────────────────────────
    {
      name: 'webkit-desktop',
      use: {
        ...devices['Desktop Safari'],
        viewport: { width: 1440, height: 900 },
      },
    },

    // ─── WebKit（iPhone 14 模擬，mobile viewport）──────────────────────────
    {
      name: 'webkit-mobile-iphone14',
      use: {
        ...devices['iPhone 14'],
        // devices['iPhone 14'] 已包含 viewport: { width: 390, height: 844 }
      },
    },
  ],

  // 本地跑測試時自動啟動 Vite dev server
  webServer: {
    command: 'npm run dev',
    cwd: './frontend',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env['CI'],
    env: {
      VITE_MSW_ENABLED: 'true',
    },
    timeout: 60_000,
  },
});
