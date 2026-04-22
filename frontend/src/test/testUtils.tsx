import { type ReactElement, type ReactNode } from 'react';
import { render, type RenderOptions, type RenderResult } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhTW from 'antd/locale/zh_TW';
import '@/i18n';

interface ProvidersProps {
  children: ReactNode;
  initialEntries?: string[] | undefined;
}

const createTestQueryClient = (): QueryClient =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });

// eslint-disable-next-line react-refresh/only-export-components -- 測試輔助檔案不需 fast refresh
const AllProviders = ({ children, initialEntries }: ProvidersProps): ReactElement => {
  const client = createTestQueryClient();
  return (
    <QueryClientProvider client={client}>
      <ConfigProvider locale={zhTW}>
        <MemoryRouter initialEntries={initialEntries ?? ['/']}>{children}</MemoryRouter>
      </ConfigProvider>
    </QueryClientProvider>
  );
};

export const renderWithProviders = (
  ui: ReactElement,
  options?: Omit<RenderOptions, 'wrapper'> & { initialEntries?: string[] },
): RenderResult => {
  const { initialEntries, ...rest } = options ?? {};
  return render(ui, {
    wrapper: ({ children }) => <AllProviders initialEntries={initialEntries}>{children}</AllProviders>,
    ...rest,
  });
};
