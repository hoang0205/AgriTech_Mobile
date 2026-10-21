import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  Space,
  Spin,
  Typography,
} from "antd";
import { LockOutlined, PhoneOutlined } from "@ant-design/icons";
import axios from "axios";
import {
  clearSession,
  getErrorMessage,
  loginAdmin,
  logoutAdmin,
  readSession,
  SESSION_EXPIRED_EVENT,
  verifyAdmin,
} from "./api";
import type { AdminSession } from "./api";
import FarmLogo from "./components/FarmLogo";

type LoginValues = {
  phoneNumber: string;
  password: string;
};

type AuthGateProps = {
  children: (auth: {
    session: AdminSession;
    onLogout: () => Promise<void>;
    loggingOut: boolean;
  }) => ReactNode;
};

export default function AuthGate({ children }: AuthGateProps) {
  const [session, setSession] = useState<AdminSession | null>(null);
  const [checking, setChecking] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  const [restoreFailed, setRestoreFailed] = useState(false);

  useEffect(() => {
    function handleExpired() {
      setSession(null);
      setError("Phiên đã hết hạn. Vui lòng đăng nhập lại.");
    }

    window.addEventListener(SESSION_EXPIRED_EVENT, handleExpired);

    return () => {
      window.removeEventListener(SESSION_EXPIRED_EVENT, handleExpired);
    };
  }, []);

  useEffect(() => {
    let active = true;

    async function restore() {
      setChecking(true);
      setError("");
      setRestoreFailed(false);

      try {
        const saved = readSession();

        if (!saved) return;

        await verifyAdmin(saved.accessToken);

        if (active) setSession(saved);
      } catch (cause) {
        if (!active) return;

        const status = axios.isAxiosError(cause)
          ? cause.response?.status
          : undefined;

        if (
          status === 401 ||
          status === 403 ||
          !axios.isAxiosError(cause)
        ) {
          clearSession();
          setSession(null);
        } else {
          // Giữ phiên khi backend tạm thời mất kết nối.
          setRestoreFailed(true);
        }

        setError(getErrorMessage(cause));
      } finally {
        if (active) setChecking(false);
      }
    }

    void restore();

    return () => {
      active = false;
    };
  }, [retry]);

  async function handleLogin(values: LoginValues) {
    setSubmitting(true);
    setError("");

    try {
      const authenticated = await loginAdmin(
        values.phoneNumber,
        values.password,
      );

      setSession(authenticated);
    } catch (cause) {
      setError(getErrorMessage(cause));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleLogout() {
    setLoggingOut(true);
    setError("");

    try {
      await logoutAdmin();
      setSession(null);
    } catch (cause) {
      setError(getErrorMessage(cause));
    } finally {
      setLoggingOut(false);
    }
  }

  if (checking) {
    return (
      <div
        style={{
          minHeight: "100dvh",
          display: "grid",
          placeItems: "center",
          background: "#F6F8F6",
        }}
      >
        <Space direction="vertical" align="center" size="large">
          <Spin size="large" />
          <Typography.Text type="secondary">
            Đang kiểm tra phiên đăng nhập…
          </Typography.Text>
        </Space>
      </div>
    );
  }

  if (session) {
    return (
      <>
        {error && (
          <Alert
            type="error"
            showIcon
            title={error}
            closable
            onClose={() => setError("")}
            style={{
              position: "fixed",
              top: 12,
              left: "50%",
              transform: "translateX(-50%)",
              zIndex: 1100,
              width: "min(520px, 90vw)",
            }}
          />
        )}

        {children({
          session,
          onLogout: handleLogout,
          loggingOut,
        })}
      </>
    );
  }

  return (
    <main
      style={{
        minHeight: "100dvh",
        display: "grid",
        placeItems: "center",
        padding: "32px 20px",
        background: "#F6F8F6",
      }}
    >
      <div style={{ width: "100%", maxWidth: 430 }}>
        <div style={{ textAlign: "center", marginBottom: 28 }}>
          <div
            style={{
              display: "grid",
              placeItems: "center",
              width: 84,
              height: 84,
              margin: "0 auto 16px",
              color: "#1B6E2A",
              background: "#FFFFFF",
              border: "1px solid #E5EBE4",
              borderRadius: 24,
              boxShadow: "0 6px 24px rgba(27, 110, 42, 0.06)",
            }}
          >
            <FarmLogo size={52} />
          </div>
          <Typography.Title
            level={1}
            style={{
              margin: "0 0 8px",
              color: "#1B6E2A",
              fontFamily: '"Trebuchet MS", "Segoe UI", sans-serif',
              fontSize: 44,
              fontWeight: 700,
              lineHeight: 1.15,
              letterSpacing: -2,
            }}
          >
            AgriTech<span style={{ color: "#82B873" }}>.</span>
          </Typography.Title>
          <Typography.Text type="secondary">
            Không gian quản trị nền tảng nông sản
          </Typography.Text>
        </div>

        <Card
          styles={{ body: { padding: 30 } }}
          style={{ borderRadius: 20, borderColor: "#E5EBE4" }}
        >
          <div style={{ textAlign: "center", marginBottom: 24 }}>
            <Typography.Title
              level={3}
              style={{ margin: "0 0 10px", fontWeight: 600 }}
            >
              Chào mừng trở lại
            </Typography.Title>
            <Typography.Paragraph
              type="secondary"
              style={{ margin: 0, lineHeight: 1.7, textWrap: "balance" }}
            >
              Đăng nhập bằng tài khoản quản trị viên để tiếp tục.
            </Typography.Paragraph>
          </div>

          {error && (
            <Alert
              type="error"
              showIcon
              title={error}
              style={{ marginBottom: 20 }}
            />
          )}

          {restoreFailed ? (
            <Space direction="vertical" style={{ width: "100%" }}>
              <Button
                type="primary"
                block
                onClick={() => setRetry((value) => value + 1)}
              >
                Thử kết nối lại
              </Button>

              <Button
                block
                onClick={() => {
                  clearSession();
                  setRestoreFailed(false);
                  setError("");
                }}
              >
                Dùng tài khoản khác
              </Button>
            </Space>
          ) : (
            <Form<LoginValues>
              layout="vertical"
              onFinish={handleLogin}
              requiredMark={false}
              disabled={submitting}
            >
              <Form.Item
                label="Số điện thoại"
                name="phoneNumber"
                rules={[
                  {
                    required: true,
                    whitespace: true,
                    message: "Nhập số điện thoại.",
                  },
                ]}
              >
                <Input
                  size="large"
                  prefix={<PhoneOutlined />}
                  placeholder="Số điện thoại đăng ký"
                  autoComplete="username"
                  inputMode="tel"
                />
              </Form.Item>

              <Form.Item
                label="Mật khẩu"
                name="password"
                rules={[
                  {
                    required: true,
                    message: "Nhập mật khẩu.",
                  },
                ]}
              >
                <Input.Password
                  size="large"
                  prefix={<LockOutlined />}
                  placeholder="Nhập mật khẩu"
                  autoComplete="current-password"
                />
              </Form.Item>

              <Button
                type="primary"
                htmlType="submit"
                size="large"
                block
                loading={submitting}
              >
                Đăng nhập
              </Button>
            </Form>
          )}
        </Card>

        <p
          style={{
            marginTop: 24,
            textAlign: "center",
            color: "#788477",
            fontSize: 12,
          }}
        >
          AgriTech · Kết nối những giá trị xanh
        </p>
      </div>
    </main>
  );
}