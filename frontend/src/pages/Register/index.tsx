import { type FC } from 'react';
import { Card, Form, Input, Button, Typography, Alert, Space, message } from 'antd';
import { LockOutlined, MailOutlined, UserOutlined } from '@ant-design/icons';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useRegister } from '@/hooks/useAuth';
import { RoutePath } from '@/constants/routes';
import { BusinessError } from '@/types/api';

interface RegisterFormValues {
  email: string;
  password: string;
  confirmPassword: string;
  displayName: string;
}

export const RegisterPage: FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [form] = Form.useForm<RegisterFormValues>();
  const registerMutation = useRegister();
  const [messageApi, contextHolder] = message.useMessage();

  const onFinish = (values: RegisterFormValues): void => {
    registerMutation.mutate(
      {
        email: values.email,
        password: values.password,
        displayName: values.displayName,
      },
      {
        onSuccess: () => {
          void messageApi.success(t('auth.registerSuccess'));
          navigate(RoutePath.LOGIN, { replace: true });
        },
      },
    );
  };

  const errorMessage =
    registerMutation.error instanceof BusinessError
      ? `[${registerMutation.error.code}] ${registerMutation.error.message}`
      : registerMutation.isError
      ? t('common.errorRetry')
      : null;

  return (
    <>
      {contextHolder}
      <Card style={{ width: 420, maxWidth: '100%' }}>
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <Typography.Title level={3} style={{ textAlign: 'center', margin: 0 }}>
            {t('auth.register')}
          </Typography.Title>

          {errorMessage !== null && <Alert type="error" message={errorMessage} showIcon />}

          <Form<RegisterFormValues>
            form={form}
            layout="vertical"
            onFinish={onFinish}
            autoComplete="off"
            aria-label={t('auth.register')}
          >
            <Form.Item
              label={t('auth.email')}
              name="email"
              rules={[
                { required: true, message: t('auth.validation.emailRequired') },
                { type: 'email', message: t('auth.validation.emailFormat') },
              ]}
            >
              <Input prefix={<MailOutlined />} autoComplete="email" />
            </Form.Item>

            <Form.Item
              label={t('auth.displayName')}
              name="displayName"
              rules={[
                { required: true, message: t('auth.validation.displayNameRequired') },
                { max: 50, message: t('auth.validation.displayNameMaxLength') },
              ]}
            >
              <Input prefix={<UserOutlined />} maxLength={50} />
            </Form.Item>

            <Form.Item
              label={t('auth.password')}
              name="password"
              rules={[
                { required: true, message: t('auth.validation.passwordRequired') },
                { min: 8, message: t('auth.validation.passwordMinLength') },
              ]}
              hasFeedback
            >
              <Input.Password prefix={<LockOutlined />} autoComplete="new-password" />
            </Form.Item>

            <Form.Item
              label={t('auth.confirmPassword')}
              name="confirmPassword"
              dependencies={['password']}
              hasFeedback
              rules={[
                { required: true, message: t('auth.validation.passwordRequired') },
                ({ getFieldValue }) => ({
                  validator(_rule, value: string) {
                    if (value === undefined || value === '' || getFieldValue('password') === value) {
                      return Promise.resolve();
                    }
                    return Promise.reject(new Error(t('auth.validation.passwordMismatch')));
                  },
                }),
              ]}
            >
              <Input.Password prefix={<LockOutlined />} autoComplete="new-password" />
            </Form.Item>

            <Form.Item>
              <Button type="primary" htmlType="submit" block loading={registerMutation.isPending}>
                {t('auth.register')}
              </Button>
            </Form.Item>

            <div style={{ textAlign: 'center' }}>
              <Link to={RoutePath.LOGIN}>{t('auth.haveAccount')}</Link>
            </div>
          </Form>
        </Space>
      </Card>
    </>
  );
};

export default RegisterPage;
