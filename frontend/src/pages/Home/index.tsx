import { type FC } from 'react';
import { Card, Col, Row, Typography, Space } from 'antd';
import { StarOutlined, BarChartOutlined, FundOutlined, BellOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';

const { Title, Paragraph } = Typography;

export const HomePage: FC = () => {
  const { t } = useTranslation();
  const user = useAuthStore((state) => state.user);

  const tiles = [
    { icon: <StarOutlined />, label: t('home.moduleTitles.watchlist') },
    { icon: <BarChartOutlined />, label: t('home.moduleTitles.techObservation') },
    { icon: <FundOutlined />, label: t('home.moduleTitles.chipObservation') },
    { icon: <BellOutlined />, label: t('home.moduleTitles.alert') },
  ];

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Title level={3} style={{ marginBottom: 0 }}>
        {t('home.welcome', { name: user?.displayName ?? user?.email ?? '' })}
      </Title>
      <Paragraph type="secondary">{t('home.summary')}</Paragraph>

      <Row gutter={[16, 16]}>
        {tiles.map((tile) => (
          <Col xs={24} sm={12} md={6} key={tile.label}>
            <Card hoverable style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 32, color: '#1677ff' }}>{tile.icon}</div>
              <Title level={5} style={{ marginTop: 12, marginBottom: 0 }}>
                {tile.label}
              </Title>
            </Card>
          </Col>
        ))}
      </Row>
    </Space>
  );
};

export default HomePage;
