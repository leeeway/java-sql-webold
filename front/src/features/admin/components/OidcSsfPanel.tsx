import React, { useEffect, useState, useCallback } from 'react';
import {
  Alert,
  Badge,
  Button,
  Card,
  Checkbox,
  Col,
  Descriptions,
  Divider,
  Drawer,
  Empty,
  Form,
  Input,
  Modal,
  Popconfirm,
  Row,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import {
  ApiOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  CloudSyncOutlined,
  DeleteOutlined,
  DisconnectOutlined,
  LinkOutlined,
  PlusOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  SendOutlined,
  SyncOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { createClient } from '@/shared/api/apiClient';

const { Text, Title } = Typography;

const COMMON_EVENT_TYPES = [
  'https://schemas.openid.net/secevent/caep/event-type/session-revoked',
  'https://schemas.openid.net/secevent/caep/event-type/token-claims-change',
  'https://schemas.openid.net/secevent/caep/event-type/credential-change',
  'https://schemas.openid.net/secevent/caep/event-type/assurance-level-change',
  'https://schemas.openid.net/secevent/caep/event-type/device-compliance-change',
  'https://schemas.openid.net/secevent/risc/event-type/account-credential-change-required',
  'https://schemas.openid.net/secevent/risc/event-type/account-purged',
  'https://schemas.openid.net/secevent/risc/event-type/account-disabled',
  'https://schemas.openid.net/secevent/risc/event-type/account-enabled',
  'https://schemas.openid.net/secevent/risc/event-type/identifier-changed',
  'https://schemas.openid.net/secevent/risc/event-type/identifier-recycled',
  'https://schemas.openid.net/secevent/ssf/event-type/verification',
];

function shortEventType(fullType) {
  if (!fullType) return '-';
  const parts = fullType.split('/');
  return parts[parts.length - 1] || fullType;
}

function formatTime(instant) {
  if (!instant) return '-';
  try {
    return new Date(instant).toLocaleString('zh-CN', { hour12: false });
  } catch {
    return String(instant);
  }
}

interface OidcSsfPanelProps {
  token: string;
}

function OidcSsfPanel({ token }: OidcSsfPanelProps) {
  const [loading, setLoading] = useState(false);
  const [oidcStatus, setOidcStatus] = useState<any>(null);
  const [userInfo, setUserInfo] = useState<any>(null);
  const [ssfStream, setSsfStream] = useState<any>(null);
  const [ssfStreamLoading, setSsfStreamLoading] = useState(false);
  const [eventLog, setEventLog] = useState<any[]>([]);
  const [eventLogLoading, setEventLogLoading] = useState(false);
  const [eventDetailVisible, setEventDetailVisible] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState<any>(null);
  const [createStreamVisible, setCreateStreamVisible] = useState(false);
  const [createStreamEndpoint, setCreateStreamEndpoint] = useState('');
  const [createStreamEvents, setCreateStreamEvents] = useState<string[]>([]);
  const [creating, setCreating] = useState(false);

  const headers = {
    headers: {
      'Content-Type': 'application/json',
      'User-Token': token,
    },
  };

  const loadOidcStatus = useCallback(async () => {
    setLoading(true);
    try {
      const client = createClient();
      const [statusRes, userinfoRes] = await Promise.all([
        client.get('/api/oidc/status', headers),
        client.get('/api/oidc/userinfo', headers),
      ]);
      if (statusRes.jsonData.status) {
        setOidcStatus(statusRes.jsonData.data);
      }
      if (userinfoRes.jsonData.status) {
        setUserInfo(userinfoRes.jsonData.data);
      }
    } catch (err) {
      console.error('Failed to load OIDC status', err);
    } finally {
      setLoading(false);
    }
  }, [token]);

  const loadSsfStream = useCallback(async () => {
    setSsfStreamLoading(true);
    try {
      const client = createClient();
      const res = await client.get('/api/ssf/stream', headers);
      if (res.jsonData.status) {
        setSsfStream(res.jsonData.data);
      } else {
        setSsfStream(null);
      }
    } catch (err) {
      console.error('Failed to load SSF stream', err);
      setSsfStream(null);
    } finally {
      setSsfStreamLoading(false);
    }
  }, [token]);

  const loadEventLog = useCallback(async () => {
    setEventLogLoading(true);
    try {
      const client = createClient();
      const res = await client.get('/api/ssf/events/log', headers);
      if (res.jsonData.status) {
        setEventLog(res.jsonData.data || []);
      }
    } catch (err) {
      console.error('Failed to load event log', err);
    } finally {
      setEventLogLoading(false);
    }
  }, [token]);

  useEffect(() => {
    void loadOidcStatus();
    void loadSsfStream();
    void loadEventLog();
  }, []);

  const handleConnect = async () => {
    try {
      const client = createClient();
      const res = await client.get('/api/oidc/auth-url', headers);
      if (res.jsonData.status && res.jsonData.data?.authUrl) {
        window.location.href = res.jsonData.data.authUrl;
      } else {
        message.error(res.jsonData.message || '无法获取授权 URL');
      }
    } catch (err) {
      message.error('请求失败');
    }
  };

  const handleDisconnect = async () => {
    try {
      const client = createClient();
      const res = await client.post('/api/oidc/disconnect', headers);
      if (res.jsonData.status) {
        message.success('已断开 OIDC 连接');
        setOidcStatus(null);
        setUserInfo(null);
        setSsfStream(null);
      } else {
        message.error(res.jsonData.message);
      }
    } catch (err) {
      message.error('断开失败');
    }
    await loadOidcStatus();
  };

  const handleRefresh = async () => {
    try {
      const client = createClient();
      const res = await client.post('/api/oidc/refresh', headers);
      if (res.jsonData.status) {
        message.success('令牌已刷新');
        setOidcStatus(res.jsonData.data);
      } else {
        message.error(res.jsonData.message);
      }
    } catch (err) {
      message.error('刷新失败');
    }
  };

  const handleCreateStream = async () => {
    if (!createStreamEndpoint) {
      message.warning('请输入推送端点 URL');
      return;
    }
    setCreating(true);
    try {
      const client = createClient();
      const res = await client.post('/api/ssf/stream', {
        ...headers,
        body: JSON.stringify({
          endpointUrl: createStreamEndpoint,
          eventsRequested: createStreamEvents,
        }),
      });
      if (res.jsonData.status) {
        message.success('Stream 创建成功');
        setSsfStream(res.jsonData.data);
        setCreateStreamVisible(false);
      } else {
        message.error(res.jsonData.message);
      }
    } catch (err) {
      message.error('创建失败');
    } finally {
      setCreating(false);
    }
  };

  const handleDeleteStream = async () => {
    try {
      const client = createClient();
      const res = await client.delete('/api/ssf/stream', headers);
      if (res.jsonData.status) {
        message.success('Stream 已删除');
        setSsfStream(null);
      } else {
        message.error(res.jsonData.message);
      }
    } catch (err) {
      message.error('删除失败');
    }
  };

  const handleVerify = async () => {
    try {
      const client = createClient();
      const res = await client.post('/api/ssf/verify', headers);
      if (res.jsonData.status) {
        message.success('验证事件已发送');
        setTimeout(() => void loadEventLog(), 2000);
      } else {
        message.error(res.jsonData.message);
      }
    } catch (err) {
      message.error('验证请求失败');
    }
  };

  const isConnected = oidcStatus?.connected === true;

  // ── Tab 1: OIDC Connection ────────────────────────────

  const renderOidcTab = () => (
    <div>
      <Card
        className="oidc-status-card"
        style={{
          background: isConnected
            ? 'linear-gradient(135deg, #e6fffb 0%, #f0f5ff 100%)'
            : 'linear-gradient(135deg, #fff2e8 0%, #fff7e6 100%)',
          borderColor: isConnected ? '#87e8de' : '#ffd591',
          marginBottom: 24,
        }}
      >
        <Row align="middle" gutter={16}>
          <Col>
            {isConnected ? (
              <CheckCircleOutlined style={{ fontSize: 48, color: '#52c41a' }} />
            ) : (
              <CloseCircleOutlined style={{ fontSize: 48, color: '#fa8c16' }} />
            )}
          </Col>
          <Col flex="auto">
            <Title level={4} style={{ margin: 0 }}>
              {isConnected ? 'OIDC 已连接' : 'OIDC 未连接'}
            </Title>
            <Text type="secondary">
              {isConnected
                ? '与身份提供方的连接正常'
                : '点击连接按钮开始 OIDC 授权'}
            </Text>
          </Col>
          <Col>
            {isConnected ? (
              <Space>
                <Button
                  icon={<ReloadOutlined />}
                  onClick={handleRefresh}
                >
                  刷新令牌
                </Button>
                <Popconfirm
                  title="确认断开 OIDC 连接？"
                  onConfirm={handleDisconnect}
                  okText="确认"
                  cancelText="取消"
                >
                  <Button danger icon={<DisconnectOutlined />}>
                    断开连接
                  </Button>
                </Popconfirm>
              </Space>
            ) : (
              <Button
                type="primary"
                size="large"
                icon={<LinkOutlined />}
                onClick={handleConnect}
                style={{
                  background: 'linear-gradient(135deg, #1890ff, #722ed1)',
                  border: 'none',
                  borderRadius: 8,
                  height: 44,
                  paddingLeft: 24,
                  paddingRight: 24,
                  fontWeight: 600,
                }}
              >
                连接 OIDC
              </Button>
            )}
          </Col>
        </Row>
      </Card>

      {isConnected && (
        <>
          {userInfo && (
            <Card title="用户信息" size="small" style={{ marginBottom: 16 }}>
              <Descriptions column={2} size="small">
                <Descriptions.Item label="Subject">{userInfo.sub || '-'}</Descriptions.Item>
                <Descriptions.Item label="Name">{userInfo.name || '-'}</Descriptions.Item>
                <Descriptions.Item label="Email">{userInfo.email || '-'}</Descriptions.Item>
                <Descriptions.Item label="Email Verified">
                  {userInfo.emailVerified === true ? (
                    <Tag color="success">已验证</Tag>
                  ) : (
                    <Tag color="warning">未验证</Tag>
                  )}
                </Descriptions.Item>
                <Descriptions.Item label="Preferred Username">
                  {userInfo.preferredUsername || '-'}
                </Descriptions.Item>
              </Descriptions>
            </Card>
          )}

          <Card title="令牌信息" size="small">
            <Descriptions column={2} size="small">
              <Descriptions.Item label="Access Token">
                <Text code>{oidcStatus.accessToken || '-'}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="ID Token">
                <Text code>{oidcStatus.idToken || '-'}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Token Type">
                {oidcStatus.tokenType || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Expires At">
                {formatTime(oidcStatus.expiresAt)}
              </Descriptions.Item>
              <Descriptions.Item label="Scopes" span={2}>
                {(oidcStatus.scopes || []).map((s) => (
                  <Tag key={s} color="blue" style={{ marginBottom: 4 }}>{s}</Tag>
                ))}
              </Descriptions.Item>
            </Descriptions>
          </Card>
        </>
      )}
    </div>
  );

  // ── Tab 2: SSF Stream ─────────────────────────────────

  const renderSsfTab = () => (
    <div>
      {!isConnected && (
        <Alert
          message="请先连接 OIDC"
          description="SSF Stream 管理需要先完成 OIDC 授权连接。"
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      {isConnected && !ssfStream && (
        <Card
          style={{
            textAlign: 'center',
            padding: '40px 0',
            background: 'linear-gradient(135deg, #f0f5ff 0%, #e6fffb 100%)',
            borderStyle: 'dashed',
            marginBottom: 16,
          }}
        >
          <Empty
            description="当前没有活跃的 SSF Stream"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          >
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setCreateStreamVisible(true)}
              style={{
                background: 'linear-gradient(135deg, #13c2c2, #1890ff)',
                border: 'none',
                borderRadius: 8,
                fontWeight: 600,
              }}
            >
              创建 Stream
            </Button>
          </Empty>
        </Card>
      )}

      {isConnected && ssfStream && (
        <Card
          title={
            <Space>
              <CloudSyncOutlined />
              <span>SSF Stream</span>
              <Badge
                status={ssfStream.status === 'enabled' ? 'success' : 'warning'}
                text={ssfStream.status || 'unknown'}
              />
            </Space>
          }
          size="small"
          extra={
            <Space>
              <Button
                icon={<ReloadOutlined />}
                size="small"
                onClick={loadSsfStream}
              >
                刷新
              </Button>
              <Button
                icon={<SendOutlined />}
                size="small"
                onClick={handleVerify}
              >
                发送验证
              </Button>
              <Popconfirm
                title="确认删除 SSF Stream？"
                onConfirm={handleDeleteStream}
                okText="确认"
                cancelText="取消"
              >
                <Button danger icon={<DeleteOutlined />} size="small">
                  删除
                </Button>
              </Popconfirm>
            </Space>
          }
          style={{ marginBottom: 16 }}
        >
          <Descriptions column={2} size="small">
            <Descriptions.Item label="Stream ID">{ssfStream.streamId || '-'}</Descriptions.Item>
            <Descriptions.Item label="Issuer">{ssfStream.issuer || '-'}</Descriptions.Item>
            <Descriptions.Item label="Delivery Method">
              <Tag color="cyan">{shortEventType(ssfStream.deliveryMethod)}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Endpoint URL">
              <Text copyable style={{ fontSize: 12 }}>{ssfStream.endpointUrl || '-'}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Audience" span={2}>
              {(ssfStream.audience || []).map((a) => (
                <Tag key={a}>{a}</Tag>
              ))}
            </Descriptions.Item>
            <Descriptions.Item label="Events Requested" span={2}>
              {(ssfStream.eventsRequested || []).map((e) => (
                <Tag key={e} color="geekblue" style={{ marginBottom: 4 }}>
                  {shortEventType(e)}
                </Tag>
              ))}
            </Descriptions.Item>
            <Descriptions.Item label="Events Delivered" span={2}>
              {(ssfStream.eventsDelivered || []).map((e) => (
                <Tag key={e} color="green" style={{ marginBottom: 4 }}>
                  {shortEventType(e)}
                </Tag>
              ))}
            </Descriptions.Item>
          </Descriptions>
        </Card>
      )}

      <Modal
        title="创建 SSF Stream"
        open={createStreamVisible}
        onOk={handleCreateStream}
        onCancel={() => setCreateStreamVisible(false)}
        confirmLoading={creating}
        okText="创建"
        cancelText="取消"
        width={640}
      >
        <Form layout="vertical">
          <Form.Item label="推送端点 URL" required>
            <Input
              placeholder="https://your-server.com/api/ssf/events"
              value={createStreamEndpoint}
              onChange={(e) => setCreateStreamEndpoint(e.target.value)}
            />
            <Text type="secondary" style={{ fontSize: 12, marginTop: 4, display: 'block' }}>
              SSF Transmitter 将向此 URL 推送安全事件 (SET)
            </Text>
          </Form.Item>
          <Form.Item label="请求的事件类型">
            <Checkbox.Group
              value={createStreamEvents}
              onChange={(values) => setCreateStreamEvents(values)}
              style={{ display: 'flex', flexDirection: 'column', gap: 6 }}
            >
              {COMMON_EVENT_TYPES.map((eventType) => (
                <Checkbox key={eventType} value={eventType}>
                  <Text code style={{ fontSize: 12 }}>{shortEventType(eventType)}</Text>
                  <Text type="secondary" style={{ fontSize: 11, marginLeft: 8 }}>
                    {eventType}
                  </Text>
                </Checkbox>
              ))}
            </Checkbox.Group>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );

  // ── Tab 3: Event Log ──────────────────────────────────

  const eventColumns = [
    {
      title: '时间',
      dataIndex: 'receivedAt',
      width: 180,
      render: formatTime,
    },
    {
      title: '事件类型',
      dataIndex: 'eventType',
      width: 200,
      render: (value) => (
        <Tag color="purple">{shortEventType(value)}</Tag>
      ),
    },
    {
      title: 'Issuer',
      dataIndex: 'iss',
      width: 200,
      ellipsis: true,
    },
    {
      title: 'JTI',
      dataIndex: 'jti',
      width: 160,
      ellipsis: true,
      render: (value) => (
        <Text code style={{ fontSize: 11 }}>{value || '-'}</Text>
      ),
    },
    {
      title: 'Subject',
      dataIndex: 'subject',
      ellipsis: true,
      render: (value) => value || '-',
    },
    {
      title: '操作',
      width: 80,
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          onClick={() => {
            setSelectedEvent(record);
            setEventDetailVisible(true);
          }}
        >
          详情
        </Button>
      ),
    },
  ];

  const renderEventTab = () => (
    <div>
      <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'flex-end' }}>
        <Button
          icon={<ReloadOutlined />}
          onClick={loadEventLog}
          loading={eventLogLoading}
        >
          刷新
        </Button>
      </div>
      <Table
        columns={eventColumns}
        dataSource={eventLog}
        loading={eventLogLoading}
        rowKey={(record) => record.jti || `${record.receivedAt}-${Math.random()}`}
        size="small"
        pagination={{ pageSize: 15 }}
        locale={{ emptyText: '暂无事件记录' }}
      />
      <Drawer
        title="事件详情"
        open={eventDetailVisible}
        onClose={() => {
          setEventDetailVisible(false);
          setSelectedEvent(null);
        }}
        width={600}
      >
        {selectedEvent && (
          <>
            <Descriptions column={1} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="JTI">{selectedEvent.jti || '-'}</Descriptions.Item>
              <Descriptions.Item label="Event Type">{selectedEvent.eventType || '-'}</Descriptions.Item>
              <Descriptions.Item label="Issuer">{selectedEvent.iss || '-'}</Descriptions.Item>
              <Descriptions.Item label="Audience">{selectedEvent.aud || '-'}</Descriptions.Item>
              <Descriptions.Item label="Issued At">{formatTime(selectedEvent.iat)}</Descriptions.Item>
              <Descriptions.Item label="Received At">{formatTime(selectedEvent.receivedAt)}</Descriptions.Item>
              <Descriptions.Item label="Subject">{selectedEvent.subject || '-'}</Descriptions.Item>
            </Descriptions>
            <Divider>Raw Payload</Divider>
            <pre
              style={{
                background: '#1e1e2e',
                color: '#cdd6f4',
                padding: 16,
                borderRadius: 8,
                fontSize: 12,
                overflow: 'auto',
                maxHeight: 400,
              }}
            >
              {JSON.stringify(selectedEvent.rawPayload, null, 2)}
            </pre>
          </>
        )}
      </Drawer>
    </div>
  );

  // ── Main ──────────────────────────────────────────────

  const tabItems = [
    {
      key: 'oidc',
      label: (
        <span>
          <SafetyCertificateOutlined /> OIDC 连接
        </span>
      ),
      children: renderOidcTab(),
    },
    {
      key: 'ssf',
      label: (
        <span>
          <CloudSyncOutlined /> SSF Stream
        </span>
      ),
      children: renderSsfTab(),
    },
    {
      key: 'events',
      label: (
        <span>
          <ThunderboltOutlined /> 事件日志
          {eventLog.length > 0 && (
            <Badge count={eventLog.length} style={{ marginLeft: 8 }} />
          )}
        </span>
      ),
      children: renderEventTab(),
    },
  ];

  return (
    <Spin spinning={loading}>
      <Tabs
        items={tabItems}
        type="card"
        size="large"
        style={{ marginTop: 8 }}
        tabBarStyle={{
          marginBottom: 16,
        }}
      />
    </Spin>
  );
}

export default OidcSsfPanel;
