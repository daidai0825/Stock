import { type FC, lazy, Suspense } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { Spin } from 'antd';
import { AppLayout } from '@/components/common/Layout/AppLayout';
import { PublicLayout } from '@/components/common/Layout/PublicLayout';
import { ProtectedRoute } from '@/components/common/Layout/ProtectedRoute';
import { NavigatorBridge } from '@/components/common/Layout/NavigatorBridge';
import LoginPage from '@/pages/Login';
import RegisterPage from '@/pages/Register';
import HomePage from '@/pages/Home';
import WatchlistPage from '@/pages/Watchlist';
import ProfilePage from '@/pages/Profile';
import { RoutePath } from '@/constants/routes';

/** Lazy-loaded：個股詳情頁（含 lightweight-charts，拆分 chunk 避免影響首頁載入） */
const StockDetailPage = lazy(() => import('@/pages/StockDetail'));

const PageFallback: FC = () => (
  <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
    <Spin size="large" />
  </div>
);

export const AppRouter: FC = () => {
  return (
    <BrowserRouter>
      {/* 掛載 NavigatorBridge：將 useNavigate 注入給非 React 樹的 axios 攔截器使用 */}
      <NavigatorBridge />
      <Routes>
        {/* 公開路由 */}
        <Route element={<PublicLayout />}>
          <Route path={RoutePath.LOGIN} element={<LoginPage />} />
          <Route path={RoutePath.REGISTER} element={<RegisterPage />} />
        </Route>

        {/* 受保護路由 */}
        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route path={RoutePath.HOME} element={<HomePage />} />
          <Route path={RoutePath.WATCHLIST} element={<WatchlistPage />} />
          <Route path={RoutePath.PROFILE} element={<ProfilePage />} />
          <Route
            path={RoutePath.STOCK_DETAIL}
            element={
              <Suspense fallback={<PageFallback />}>
                <StockDetailPage />
              </Suspense>
            }
          />
        </Route>

        {/* 預設導向 */}
        <Route path={RoutePath.ROOT} element={<Navigate to={RoutePath.HOME} replace />} />
        <Route path="*" element={<Navigate to={RoutePath.HOME} replace />} />
      </Routes>
    </BrowserRouter>
  );
};
