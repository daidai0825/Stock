import { type FC, useMemo } from 'react';
import { Layout, Menu, Button, Space, Typography } from 'antd';
import { HomeOutlined, StarOutlined, UserOutlined, LogoutOutlined } from '@ant-design/icons';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { useLogout } from '@/hooks/useAuth';
import { RoutePath } from '@/constants/routes';
import { currentYear } from '@/utils/datetime';

const { Header, Content, Footer, Sider } = Layout;
const { Title, Text } = Typography;

export const AppLayout: FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((state) => state.user);
  const logoutMutation = useLogout();

  const menuItems = useMemo(
    () => [
      { key: RoutePath.HOME, icon: <HomeOutlined />, label: t('nav.home') },
      { key: RoutePath.WATCHLIST, icon: <StarOutlined />, label: t('nav.watchlist') },
      { key: RoutePath.PROFILE, icon: <UserOutlined />, label: t('nav.profile') },
    ],
    [t],
  );

  const handleLogout = (): void => {
    logoutMutation.mutate(undefined, {
      onSettled: () => {
        navigate(RoutePath.LOGIN, { replace: true });
      },
    });
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: '#001529',
          padding: '0 24px',
        }}
      >
        <Title level={4} style={{ color: '#fff', margin: 0 }}>
          {t('app.title')}
        </Title>
        <Space>
          {user !== null && <Text style={{ color: '#fff' }}>{user.displayName || user.email}</Text>}
          <Button
            type="text"
            icon={<LogoutOutlined />}
            onClick={handleLogout}
            loading={logoutMutation.isPending}
            style={{ color: '#fff' }}
            aria-label={t('common.logout')}
          >
            {t('common.logout')}
          </Button>
        </Space>
      </Header>

      <Layout>
        <Sider
          breakpoint="md"
          collapsedWidth={0}
          width={200}
          style={{ background: '#fff' }}
          aria-label="primary navigation"
        >
          <Menu
            mode="inline"
            selectedKeys={[location.pathname]}
            items={menuItems}
            onClick={({ key }) => navigate(key)}
            style={{ height: '100%', borderRight: 0 }}
          />
        </Sider>

        <Layout style={{ padding: '24px' }} className="responsive-container">
          <Content
            style={{
              padding: 24,
              margin: 0,
              minHeight: 280,
              background: '#fff',
              borderRadius: 8,
            }}
          >
            <Outlet />
          </Content>

          <Footer style={{ textAlign: 'center', padding: '12px 24px', background: 'transparent' }}>
            <div className="disclaimer-text">{t('disclaimer')}</div>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {t('footer.copyright', { year: currentYear() })} ・ {t('footer.version', { version: '0.1.0' })}
            </Text>
          </Footer>
        </Layout>
      </Layout>
    </Layout>
  );
};
