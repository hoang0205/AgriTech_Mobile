import { useEffect, useState } from "react";
import {
  Alert,
  Button,
  Descriptions,
  Drawer,
  Empty,
  Input,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
} from "antd";
import type { TableColumnsType } from "antd";
import {
  ArrowRightOutlined,
  EyeOutlined,
  HistoryOutlined,
  ReloadOutlined,
} from "@ant-design/icons";
import axios from "axios";
import { api, getErrorMessage } from "../api";

type AuditAction =
  | "USER_LOCK"
  | "USER_UNLOCK"
  | "PRODUCT_HIDE"
  | "PRODUCT_SHOW"
  | "REPORT_UPDATE";

type AuditLog = {
  id: string;
  actorId: string;
  actorName: string;
  action: AuditAction;
  targetType: string;
  targetId: string;
  targetName: string;
  beforeState: string;
  afterState: string;
  relatedReportId: string | null;
  createdAt: string;
};

type AuditResponse = {
  items: AuditLog[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
};

type AuditQuery = {
  keyword: string;
  action?: AuditAction;
  page: number;
  size: number;
};

type AuditLogsProps = {
  compact?: boolean;
  onViewAll?: () => void;
};

const actionLabels: Record<AuditAction, string> = {
  USER_LOCK: "Khóa tài khoản",
  USER_UNLOCK: "Mở khóa tài khoản",
  PRODUCT_HIDE: "Ẩn sản phẩm",
  PRODUCT_SHOW: "Hiển thị sản phẩm",
  REPORT_UPDATE: "Xử lý báo cáo",
};

const actionColors: Record<AuditAction, string> = {
  USER_LOCK: "red",
  USER_UNLOCK: "green",
  PRODUCT_HIDE: "orange",
  PRODUCT_SHOW: "green",
  REPORT_UPDATE: "blue",
};

const targetLabels: Record<string, string> = {
  USER: "Người dùng",
  PRODUCT: "Sản phẩm",
  REPORT: "Báo cáo",
};

function formatDate(value: string) {
  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? "Chưa có thông tin"
    : date.toLocaleString("vi-VN", {
        timeZone: "Asia/Ho_Chi_Minh",
      });
}

function StateText({ value }: { value: string }) {
  return (
    <div
      style={{
        whiteSpace: "pre-wrap",
        overflowWrap: "anywhere",
        lineHeight: 1.7,
      }}
    >
      {value || "Không có thông tin"}
    </div>
  );
}

export default function AuditLogs({
  compact = false,
  onViewAll,
}: AuditLogsProps) {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [searchText, setSearchText] = useState("");
  const [selected, setSelected] = useState<AuditLog | null>(null);

  const [query, setQuery] = useState<AuditQuery>({
    keyword: "",
    page: 1,
    size: 10,
  });

  useEffect(() => {
    const controller = new AbortController();

    async function fetchLogs() {
      setLoading(true);
      setError("");

      try {
        const { data } = await api.get<AuditResponse>(
          "/admin/audit-logs",
          {
            params: compact
              ? {
                  page: 0,
                  size: 5,
                }
              : {
                  keyword: query.keyword,
                  action: query.action,
                  page: query.page - 1,
                  size: query.size,
                },
            signal: controller.signal,
          },
        );

        if (controller.signal.aborted) return;

        setLogs(data.items);
        setTotal(data.totalElements);
      } catch (cause) {
        if (axios.isCancel(cause) || controller.signal.aborted) {
          return;
        }

        setLogs([]);
        setTotal(0);
        setError(getErrorMessage(cause));
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void fetchLogs();

    return () => controller.abort();
  }, [compact, query, reload]);

  const columns: TableColumnsType<AuditLog> = [
    {
      title: "Thời gian",
      dataIndex: "createdAt",
      width: 190,
      render: (value: string) => formatDate(value),
    },
    {
      title: "Admin thực hiện",
      dataIndex: "actorName",
      width: 180,
    },
    {
      title: "Thao tác",
      dataIndex: "action",
      width: 180,
      render: (action: AuditAction) => (
        <Tag color={actionColors[action]}>
          {actionLabels[action] ?? action}
        </Tag>
      ),
    },
    {
      title: "Đối tượng",
      dataIndex: "targetName",
      width: 260,
      render: (name: string, log) => (
        <div>
          <Typography.Text strong>{name}</Typography.Text>

          <div>
            <Typography.Text
              type="secondary"
              style={{ fontSize: 12 }}
            >
              {targetLabels[log.targetType] ?? log.targetType}
            </Typography.Text>
          </div>
        </div>
      ),
    },
    {
      title: "Chi tiết",
      key: "detail",
      width: 100,
      render: (_, log) => (
        <Button
          type="text"
          icon={<EyeOutlined />}
          onClick={() => setSelected(log)}
          aria-label={`Xem nhật ký ${log.id}`}
        >
          Xem
        </Button>
      ),
    },
  ];

  return (
    <section className="panel">
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          flexWrap: "wrap",
          gap: 12,
          marginBottom: 20,
        }}
      >
        <div>
          <Typography.Title
            level={compact ? 5 : 4}
            style={{ margin: "0 0 6px" }}
          >
            {compact ? "Hoạt động gần đây" : "Nhật ký quản trị"}
          </Typography.Title>

          <Typography.Text
            type="secondary"
            style={{ fontSize: 12 }}
          >
            {compact
              ? "5 thao tác quản trị mới nhất"
              : "Tra cứu người thực hiện và lịch sử thay đổi dữ liệu"}
          </Typography.Text>
        </div>

        <Button
          type={compact ? "text" : "default"}
          icon={<ReloadOutlined />}
          loading={loading}
          onClick={() => setReload((value) => value + 1)}
          aria-label="Tải lại nhật ký"
        >
          {!compact && "Tải lại"}
        </Button>
      </div>

      {!compact && (
        <Space wrap size={12} style={{ marginBottom: 20 }}>
          <Input.Search
            placeholder="Tìm tên Admin, đối tượng hoặc mã"
            aria-label="Tìm nhật ký"
            maxLength={200}
            allowClear
            value={searchText}
            onChange={(event) =>
              setSearchText(event.target.value)
            }
            onSearch={(value) => {
              setQuery((current) => ({
                ...current,
                keyword: value.trim(),
                page: 1,
              }));
            }}
            style={{ width: "min(330px, 100%)" }}
          />

          <Select<AuditAction>
            placeholder="Tất cả thao tác"
            aria-label="Lọc loại thao tác"
            allowClear
            value={query.action}
            style={{ width: 210 }}
            options={(
              Object.keys(actionLabels) as AuditAction[]
            ).map((action) => ({
              value: action,
              label: actionLabels[action],
            }))}
            onChange={(action) => {
              setQuery((current) => ({
                ...current,
                action,
                page: 1,
              }));
            }}
          />
        </Space>
      )}

      {error && (
        <Alert
          type="error"
          showIcon
          title="Không tải được nhật ký"
          description={error}
          style={{ marginBottom: 16 }}
        />
      )}

      {compact ? (
        <>
          {loading ? (
            <div style={{ padding: 50, textAlign: "center" }}>
              <Spin />
            </div>
          ) : error ? null : logs.length === 0 ? (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="Chưa có thao tác quản trị được ghi nhận"
            />
          ) : (
            <div>
              {logs.map((log) => (
                <button
                  key={log.id}
                  type="button"
                  onClick={() => setSelected(log)}
                  style={{
                    display: "flex",
                    alignItems: "flex-start",
                    gap: 12,
                    width: "100%",
                    padding: "15px 0",
                    textAlign: "left",
                    background: "transparent",
                    border: "none",
                    borderBottom: "1px solid #F0F3EE",
                    cursor: "pointer",
                    color: "inherit",
                  }}
                >
                  <span
                    style={{
                      display: "grid",
                      placeItems: "center",
                      width: 36,
                      height: 36,
                      flexShrink: 0,
                      borderRadius: 11,
                      background: "#EFF6ED",
                      color: "#1B6E2A",
                      fontSize: 17,
                    }}
                  >
                    <HistoryOutlined />
                  </span>

                  <span style={{ minWidth: 0, flex: 1 }}>
                    <span
                      style={{
                        display: "block",
                        fontSize: 13,
                        lineHeight: 1.6,
                      }}
                    >
                      <strong>{log.actorName}</strong>
                      {" · "}
                      {actionLabels[log.action] ?? log.action}
                    </span>

                    <span
                      style={{
                        display: "block",
                        marginTop: 3,
                        fontSize: 12,
                        color: "#788477",
                        overflowWrap: "anywhere",
                      }}
                    >
                      {log.targetName}
                    </span>

                    <span
                      style={{
                        display: "block",
                        marginTop: 5,
                        color: "#929E8E",
                        fontSize: 11,
                      }}
                    >
                      {formatDate(log.createdAt)}
                    </span>
                  </span>
                </button>
              ))}
            </div>
          )}

          {onViewAll && (
            <Button
              type="link"
              icon={<ArrowRightOutlined />}
              onClick={onViewAll}
              style={{ paddingInline: 0, marginTop: 16 }}
            >
              Xem toàn bộ nhật ký
            </Button>
          )}
        </>
      ) : (
        <Table<AuditLog>
          rowKey="id"
          columns={columns}
          dataSource={logs}
          loading={loading}
          scroll={{ x: 910 }}
          locale={{
            emptyText: error
              ? "Không tải được nhật ký"
              : "Không tìm thấy nhật ký phù hợp",
          }}
          pagination={{
            current: query.page,
            pageSize: query.size,
            total,
            showSizeChanger: true,
            pageSizeOptions: [10, 20, 50],
            showTotal: (count) => `${count} thao tác`,
            onChange: (page, size) => {
              setQuery((current) => ({
                ...current,
                page: size !== current.size ? 1 : page,
                size,
              }));
            },
          }}
        />
      )}

      <Drawer
        title="Chi tiết thao tác quản trị"
        open={selected !== null}
        onClose={() => setSelected(null)}
        styles={{
          wrapper: { width: "min(640px, 100vw)" },
        }}
      >
        {selected && (
          <Descriptions column={1} layout="vertical" bordered>
            <Descriptions.Item label="Mã nhật ký">
              {selected.id}
            </Descriptions.Item>

            <Descriptions.Item label="Admin thực hiện">
              {selected.actorName}
            </Descriptions.Item>

            <Descriptions.Item label="Mã Admin">
              {selected.actorId}
            </Descriptions.Item>

            <Descriptions.Item label="Thời gian">
              {formatDate(selected.createdAt)}
            </Descriptions.Item>

            <Descriptions.Item label="Thao tác">
              <Tag color={actionColors[selected.action]}>
                {actionLabels[selected.action] ?? selected.action}
              </Tag>
            </Descriptions.Item>

            <Descriptions.Item label="Loại đối tượng">
              {targetLabels[selected.targetType] ??
                selected.targetType}
            </Descriptions.Item>

            <Descriptions.Item label="Đối tượng">
              <StateText value={selected.targetName} />
            </Descriptions.Item>

            <Descriptions.Item label="Mã đối tượng">
              <StateText value={selected.targetId} />
            </Descriptions.Item>

            {selected.relatedReportId && (
              <Descriptions.Item label="Thực hiện từ báo cáo">
                #{selected.relatedReportId}
              </Descriptions.Item>
            )}

            <Descriptions.Item label="Trước thay đổi">
              <StateText value={selected.beforeState} />
            </Descriptions.Item>

            <Descriptions.Item label="Sau thay đổi">
              <StateText value={selected.afterState} />
            </Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </section>
  );
}