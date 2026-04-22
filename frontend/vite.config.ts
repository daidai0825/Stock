/// <reference types="vitest" />
import { defineConfig, type UserConfig } from 'vite';
import react from '@vitejs/plugin-react-swc';
import path from 'node:path';

// 台股分析平台 — Wave A：純 Web Responsive
// 依拍板 D：不含 React Native，純 Web + RWD
// 注意：vitest 設定透過 triple-slash reference 增強 UserConfig 型別，避免引入 vitest/config 造成
//       vitest 內建 vite 與 root vite 版本型別衝突（exactOptionalPropertyTypes 模式）
const config: UserConfig = {
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
  },
  build: {
    target: 'es2022',
    sourcemap: true,
    rollupOptions: {
      output: {
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
          antd: ['antd', '@ant-design/icons'],
          chart: ['lightweight-charts'],
        },
      },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
} as UserConfig;

export default defineConfig(config);
