import { type FC } from 'react';
import { ConfigProvider } from 'antd';
import zhTW from 'antd/locale/zh_TW';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AppRouter } from '@/routes/AppRouter';
import '@/i18n';

// TanStack Query：禁用降級處理（system-design.md），故 retry: false
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
      refetchOnWindowFocus: false,
      staleTime: 30_000,
    },
    mutations: {
      retry: false,
    },
  },
});

const App: FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={zhTW} theme={{ token: { colorPrimary: '#1677ff', borderRadius: 6 } }}>
        <AppRouter />
      </ConfigProvider>
    </QueryClientProvider>
  );
};

export default App;
