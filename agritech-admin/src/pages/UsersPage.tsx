import { useEffect, useState } from "react";
import {
  Alert,
  Button,
  Descriptions,
  Drawer,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from "antd";
import type { TableColumnsType } from "antd";
import {
  EyeOutlined,
  LockOutlined,
  ReloadOutlined,
  UnlockOutlined,
} from "@ant-design/icons";
import axios from "axios";
import { api, getErrorMessage } from "../api";

type UserRole = "ADMIN" | "USER";

type AdminUser = {
  id: string;
  fullName: string;
  phoneNumber: string;
  email: string;
  role: UserRole;
  createdAt: string | null;
  locked: boolean;
};

type UsersResponse = {
  items: AdminUser[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
};

type UsersQuery = {
  keyword: string;
  role?: UserRole;
  page: number;
  size: number;
};

const roleLabels: Record<UserRole, string> = {
  ADMIN: "Admin",
  USER: "Người dùng",
};

const roleColors: Record<UserRole, string> = {
  ADMIN: "purple",
  USER: "green",
};

function formatDate(value: string | null) {
  if (!value) return "Chưa có thông tin";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) return "Chưa có thông tin";

  return date.toLocaleString("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
  });
}

function StatusTag({ locked }: { locked: boolean }) {
  return (
    <Tag color={locked ? "red" : "green"}>
      {locked ? "Đã khóa" : "Hoạt động"}
    </Tag>
  );
}

export default function UsersPage() {
  const [messageApi, messageContext] = message.useMessage();

  const [users, setUsers] = useState<AdminUser[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchText, setSearchText] = useState("");
  const [reload, setReload] = useState(0);

  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(
    null,
  );

  const [statusTarget, setStatusTarget] = useState<AdminUser | null>(
    null,
  );
  const [savingStatus, setSavingStatus] = useState(false);
  const [statusError, setStatusError] = useState("");

  const [query, setQuery] = useState<UsersQuery>({
    keyword: "",
    page: 1,
    size: 10,
  });

  useEffect(() => {
    const controller = new AbortController();

    async function fetchUsers() {
      setLoading(true);
      setError("");

      try {
        const { data } = await api.get<UsersResponse>(
          "/admin/users",
          {
            params: {
              keyword: query.keyword,
              role: query.role,
              page: query.page - 1,
              size: query.size,
            },
            signal: controller.signal,
          },
        );

        if (controller.signal.aborted) return;

        setUsers(data.items);
        setTotal(data.totalElements);
      } catch (cause) {
        if (axios.isCancel(cause) || controller.signal.aborted) {
          return;
        }

        setError(getErrorMessage(cause));
        setUsers([]);
        setTotal(0);
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    void fetchUsers();

    return () => controller.abort();
  }, [query, reload]);

  function openStatusConfirm(user: AdminUser) {
    setStatusError("");
    setStatusTarget(user);
  }

  function closeStatusConfirm() {
    if (savingStatus) return;

    setStatusTarget(null);
    setStatusError("");
  }

  async function updateUserStatus() {
    if (!statusTarget || savingStatus) return;

    const target = statusTarget;
    const nextLocked = !target.locked;

    setSavingStatus(true);
    setStatusError("");

    try {
      const { data } = await api.patch<AdminUser>(
        `/admin/users/${encodeURIComponent(target.id)}/status`,
        {
          locked: nextLocked,
        },
      );

      // Chỉ cập nhật giao diện sau khi backend xác nhận thành công.
      setUsers((current) =>
        current.map((user) => (user.id === data.id ? data : user)),
      );

      setSelectedUser((current) =>
        current?.id === data.id ? data : current,
      );

      setStatusTarget(null);

      void messageApi.success(
        data.locked
          ? "Đã khóa tài khoản thành công."
          : "Đã mở khóa tài khoản thành công.",
      );
    } catch (cause) {
      // Giữ hộp xác nhận mở để người dùng biết lỗi và thử lại.
      setStatusError(getErrorMessage(cause));
    } finally {
      setSavingStatus(false);
    }
  }

  const columns: TableColumnsType<AdminUser> = [
    {
      title: "Người dùng",
      dataIndex: "fullName",
      width: 210,
      render: (name: string, user) => (
        <div>
          <Typography.Text strong>{name}</Typography.Text>
          <div>
            <Typography.Text
              type="secondary"
              style={{ fontSize: 12 }}
            >
              {user.phoneNumber}
            </Typography.Text>
          </div>
        </div>
      ),
    },
    {
      title: "Email",
      dataIndex: "email",
      width: 230,
      ellipsis: true,
    },
    {
      title: "Vai trò",
      dataIndex: "role",
      width: 130,
      render: (role: UserRole) => (
        <Tag color={roleColors[role]}>{roleLabels[role]}</Tag>
      ),
    },
    {
      title: "Trạng thái",
      dataIndex: "locked",
      width: 130,
      render: (locked: boolean) => <StatusTag locked={locked} />,
    },
    {
      title: "Ngày tham gia",
      dataIndex: "createdAt",
      width: 190,
      render: (value: string | null) => formatDate(value),
    },
    {
      title: "Thao tác",
      key: "actions",
      fixed: "right",
      width: 220,
      render: (_, user) => (
        <Space size={4}>
          <Button
            type="text"
            icon={<EyeOutlined />}
            onClick={() => setSelectedUser(user)}
            aria-label={`Xem thông tin ${user.fullName}`}
          >
            Chi tiết
          </Button>

          {user.role === "ADMIN" ? (
            <Tooltip title="Không được khóa tài khoản Admin">
              <span>
                <Button
                  type="text"
                  icon={<LockOutlined />}
                  disabled
                >
                  Khóa
                </Button>
              </span>
            </Tooltip>
          ) : (
            <Button
              type="text"
              danger={!user.locked}
              icon={
                user.locked ? <UnlockOutlined /> : <LockOutlined />
              }
              disabled={loading || savingStatus}
              onClick={() => openStatusConfirm(user)}
              aria-label={`${
                user.locked ? "Mở khóa" : "Khóa"
              } tài khoản ${user.fullName}`}
            >
              {user.locked ? "Mở khóa" : "Khóa"}
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <section className="panel">
      {messageContext}

      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          flexWrap: "wrap",
          gap: 12,
          marginBottom: 24,
        }}
      >
        <div>
          <Typography.Title
            level={4}
            style={{ margin: "0 0 6px" }}
          >
            Danh sách người dùng
          </Typography.Title>

          <Typography.Text type="secondary">
            Tra cứu và quản lý tài khoản trên nền tảng AgriTech.
          </Typography.Text>
        </div>

        <Button
          icon={<ReloadOutlined />}
          loading={loading}
          disabled={savingStatus}
          onClick={() => setReload((value) => value + 1)}
        >
          Tải lại
        </Button>
      </div>

      <Space
        wrap
        size={12}
        style={{ marginBottom: 20, width: "100%" }}
      >
        <Input.Search
          placeholder="Tìm tên, email, số điện thoại"
          aria-label="Tìm người dùng"
          allowClear
          disabled={savingStatus}
          value={searchText}
          onChange={(event) => setSearchText(event.target.value)}
          onSearch={(value) => {
            setQuery((current) => ({
              ...current,
              keyword: value.trim(),
              page: 1,
            }));
          }}
          style={{ width: "min(320px, 100%)" }}
        />

        <Select<UserRole>
          placeholder="Tất cả vai trò"
          aria-label="Lọc vai trò"
          allowClear
          disabled={savingStatus}
          value={query.role}
          style={{ width: 180 }}
          options={[
            { value: "ADMIN", label: "Admin" },
            { value: "USER", label: "Người dùng" },
          ]}
          onChange={(role) => {
            setQuery((current) => ({
              ...current,
              role,
              page: 1,
            }));
          }}
        />
      </Space>

      {error && (
        <Alert
          type="error"
          showIcon
          title={error}
          style={{ marginBottom: 20 }}
        />
      )}

      <Table<AdminUser>
        rowKey="id"
        columns={columns}
        dataSource={users}
        loading={loading}
        scroll={{ x: 1110 }}
        locale={{
          emptyText: error
            ? "Không tải được danh sách người dùng"
            : "Không tìm thấy người dùng",
        }}
        pagination={{
          current: query.page,
          pageSize: query.size,
          total,
          disabled: savingStatus,
          showSizeChanger: true,
          pageSizeOptions: [10, 20, 50],
          showTotal: (count) => `${count} người dùng`,
          onChange: (page, size) => {
            setQuery((current) => ({
              ...current,
              page: size !== current.size ? 1 : page,
              size,
            }));
          },
        }}
      />

      <Drawer
        title="Thông tin người dùng"
        open={selectedUser !== null}
        onClose={() => setSelectedUser(null)}
      >
        {selectedUser && (
          <Descriptions column={1} layout="vertical" bordered>
            <Descriptions.Item label="Mã tài khoản">
              {selectedUser.id}
            </Descriptions.Item>

            <Descriptions.Item label="Họ và tên">
              {selectedUser.fullName}
            </Descriptions.Item>

            <Descriptions.Item label="Số điện thoại">
              {selectedUser.phoneNumber}
            </Descriptions.Item>

            <Descriptions.Item label="Email">
              {selectedUser.email}
            </Descriptions.Item>

            <Descriptions.Item label="Vai trò">
              <Tag color={roleColors[selectedUser.role]}>
                {roleLabels[selectedUser.role]}
              </Tag>
            </Descriptions.Item>

            <Descriptions.Item label="Trạng thái">
              <StatusTag locked={selectedUser.locked} />
            </Descriptions.Item>

            <Descriptions.Item label="Ngày tham gia">
              {formatDate(selectedUser.createdAt)}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>

      <Modal
        title={
          statusTarget?.locked
            ? "Mở khóa tài khoản"
            : "Khóa tài khoản"
        }
        open={statusTarget !== null}
        onCancel={closeStatusConfirm}
        onOk={() => void updateUserStatus()}
        confirmLoading={savingStatus}
        okText={
          statusTarget?.locked
            ? "Xác nhận mở khóa"
            : "Xác nhận khóa"
        }
        cancelText="Hủy"
        okButtonProps={{
          danger: statusTarget !== null && !statusTarget.locked,
        }}
        cancelButtonProps={{ disabled: savingStatus }}
        closable={!savingStatus}
        maskClosable={!savingStatus}
        keyboard={!savingStatus}
      >
        {statusTarget && (
          <>
            <Typography.Paragraph>
              Bạn muốn {statusTarget.locked ? "mở khóa" : "khóa"} tài
              khoản <strong>{statusTarget.fullName}</strong> (
              {statusTarget.phoneNumber})?
            </Typography.Paragraph>

            <Alert
              type={statusTarget.locked ? "info" : "warning"}
              showIcon
              title={
                statusTarget.locked
                  ? "Người dùng có thể đăng nhập lại sau khi được mở khóa."
                  : "Người dùng sẽ không thể đăng nhập hoặc tiếp tục sử dụng phiên hiện tại."
              }
              description={
                statusTarget.locked
                  ? "Các phiên đã bị thu hồi vẫn không sử dụng lại được."
                  : "Thao tác này giữ lại tài khoản và lịch sử giao dịch."
              }
            />

            {statusError && (
              <Alert
                type="error"
                showIcon
                title={statusError}
                style={{ marginTop: 16 }}
              />
            )}
          </>
        )}
      </Modal>
    </section>
  );
}
