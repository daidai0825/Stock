import { type FC, useEffect } from 'react';
import { Button, Card, Form, Input, Space, Typography, Alert, Switch, Divider, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { useMutation } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';
import { memberService } from '@/services/memberService';
import { BusinessError } from '@/types/api';
import type { UpdateProfileRequest } from '@/types/member';

const { Title } = Typography;

export const ProfilePage: FC = () => {
  const { t } = useTranslation();
  const user = useAuthStore((state) => state.user);
  const setUser = useAuthStore((state) => state.setUser);
  const [form] = Form.useForm<UpdateProfileRequest>();
  const [messageApi, contextHolder] = message.useMessage();

  const updateMutation = useMutation({
    mutationFn: (req: UpdateProfileRequest) => memberService.updateProfile(req),
    onSuccess: (data) => {
      setUser(data);
      void messageApi.success(t('profile.updateSuccess'));
    },
  });

  useEffect(() => {
    if (user !== null) {
      form.setFieldsValue({ displayName: user.displayName });
    }
  }, [user, form]);

  if (user === null) {
    return <Alert type="warning" message={t('errors.3001')} showIcon />;
  }

  const errorMessage =
    updateMutation.error instanceof BusinessError
      ? `[${updateMutation.error.code}] ${updateMutation.error.message}`
      : updateMutation.isError
      ? t('common.errorRetry')
      : null;

  return (
    <>
      {contextHolder}
      <Space direction="vertical" size="large" style={{ width: '100%', maxWidth: 640 }}>
        <Title level={3} style={{ margin: 0 }}>
          {t('profile.title')}
        </Title>

        {errorMessage !== null && <Alert type="error" message={errorMessage} showIcon />}

        <Card>
          <Form<UpdateProfileRequest>
            form={form}
            layout="vertical"
            onFinish={(values) => updateMutation.mutate(values)}
            initialValues={{ displayName: user.displayName }}
          >
            <Form.Item label={t('auth.email')}>
              <Input value={user.email} disabled />
            </Form.Item>

            <Form.Item
              label={t('auth.displayName')}
              name="displayName"
              rules={[
                { required: true, message: t('auth.validation.displayNameRequired') },
                { max: 50, message: t('auth.validation.displayNameMaxLength') },
              ]}
            >
              <Input maxLength={50} />
            </Form.Item>

            <Form.Item>
              <Button type="primary" htmlType="submit" loading={updateMutation.isPending}>
                {t('common.save')}
              </Button>
            </Form.Item>
          </Form>

          <Divider />

          <Title level={5}>{t('profile.notifySection')}</Title>
          <Alert type="info" showIcon message={t('profile.notifyComingSoon')} style={{ marginBottom: 16 }} />
          <Space direction="vertical">
            <div>
              <Switch checked={user.notifyWebEnabled} disabled /> &nbsp;{t('profile.notifyWeb')}
            </div>
            <div>
              <Switch checked={false} disabled /> &nbsp;{t('profile.notifyTelegram')}
            </div>
          </Space>
        </Card>
      </Space>
    </>
  );
};

export default ProfilePage;
