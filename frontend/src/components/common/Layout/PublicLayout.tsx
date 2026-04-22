import { type FC } from 'react';
import { Layout, Typography } from 'antd';
import { Outlet } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { currentYear } from '@/utils/datetime';

const { Header, Content, Footer } = Layout;
const { Title } = Typography;

export const PublicLayout: FC = () => {
  const { t } = useTranslation();

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header
        style={{
          background: '#001529',
          display: 'flex',
          alignItems: 'center',
          padding: '0 24px',
        }}
      >
        <Title level={4} style={{ color: '#fff', margin: 0 }}>
          {t('app.title')}
        </Title>
      </Header>

      <Content
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: 24,
        }}
      >
        <Outlet />
      </Content>

      <Footer style={{ textAlign: 'center', background: 'transparent' }}>
        <div className="disclaimer-text">{t('disclaimer')}</div>
        <div style={{ fontSize: 12, color: 'rgba(0,0,0,0.45)' }}>
          {t('footer.copyright', { year: currentYear() })}
        </div>
      </Footer>
    </Layout>
  );
};
