import { type FC } from 'react';
import { Card, Form, Input, Button, Typography, Alert, Space } from 'antd';
import { LockOutlined, MailOutlined } from '@ant-design/icons';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useLogin } from '@/hooks/useAuth';
import { RoutePath } from '@/constants/routes';
import { BusinessError } from '@/types/api';
import type { LoginRequest } from '@/types/member';

interface LocationState {
  from?: string;
}

export const LoginPage: FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const [form] = Form.useForm<LoginRequest>();
  const loginMutation = useLogin();

  const fromPath = (location.state as LocationState | null)?.from ?? RoutePath.HOME;

  const onFinish = (values: LoginRequest): void => {
    loginMutation.mutate(values, {
      onSuccess: () => {
        navigate(fromPath, { replace: true });
      },
    });
  };

  const errorMessage =
    loginMutation.error instanceof BusinessError
      ? `[${loginMutation.error.code}] ${loginMutation.error.message}`
      : loginMutation.isError
      ? t('common.errorRetry')
      : null;

  return (
    <Card style={{ width: 400, maxWidth: '100%' }}>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Typography.Title level={3} style={{ textAlign: 'center', margin: 0 }}>
          {t('auth.login')}
        </Typography.Title>

        {errorMessage !== null && <Alert type="error" message={errorMessage} showIcon />}

        <Form<LoginRequest>
          form={form}
          layout="vertical"
          onFinish={onFinish}
          autoComplete="off"
          aria-label={t('auth.login')}
        >
          <Form.Item
            label={t('auth.email')}
            name="email"
            rules={[
              { required: true, message: t('auth.validation.emailRequired') },
              { type: 'email', message: t('auth.validation.emailFormat') },
            ]}
          >
            <Input prefix={<MailOutlined />} placeholder="user@example.com" autoComplete="email" />
          </Form.Item>

          <Form.Item
            label={t('auth.password')}
            name="password"
            rules={[
              { required: true, message: t('auth.validation.passwordRequired') },
              { min: 8, message: t('auth.validation.passwordMinLength') },
            ]}
          >
            <Input.Password prefix={<LockOutlined />} autoComplete="current-password" />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={loginMutation.isPending}>
              {t('auth.login')}
            </Button>
          </Form.Item>

          <div style={{ textAlign: 'center' }}>
            <Link to={RoutePath.REGISTER}>{t('auth.noAccount')}</Link>
          </div>
        </Form>
      </Space>
    </Card>
  );
};

export default LoginPage;
