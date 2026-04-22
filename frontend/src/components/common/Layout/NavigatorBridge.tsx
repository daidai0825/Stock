import { useEffect, type FC } from 'react';
import { useNavigate } from 'react-router-dom';
import { setAppNavigate } from '@/services/navigator';

/**
 * NavigatorBridge
 *
 * 將 React Router 的 useNavigate() 結果注入到 services/navigator.ts，
 * 讓 axios 攔截器（位於非 React 樹）可呼叫 SPA navigate 而非 hard reload。
 *
 * 必須掛在 <BrowserRouter> 之內、Routes 之前（兄弟層級即可）。
 */
export const NavigatorBridge: FC = () => {
  const navigate = useNavigate();

  useEffect(() => {
    setAppNavigate(navigate);
    return () => {
      setAppNavigate(null);
    };
  }, [navigate]);

  return null;
};
