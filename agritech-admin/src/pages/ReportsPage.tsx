import { useEffect, useState } from "react";
import {
  Alert,
  Button,
  Descriptions,
  Drawer,
  Image,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from "antd";
import type { TableColumnsType } from "antd";
import {
  EyeInvisibleOutlined,
  EyeOutlined,
  ReloadOutlined,
} from "@ant-design/icons";
import axios from "axios";
import { api, getErrorMessage } from "../api";

type ReportStatus =
  | "PENDING"
  | "REVIEWING"
  | "RESOLVED"
  | "REJECTED";

type ReportReason =
  | "SCAM"
  | "MISLEADING_INFORMATION"
  | "INAPPROPRIATE_CONTENT"
  | "OTHER";

type ProductReport = {
  id: number;
  productId: string;
  productName: string;
  reason: ReportReason;
  description: string;
  imageUrls: string[];
  status: ReportStatus;
  adminReply: string | null;
  createdAt: string;
  productHidden: boolean | null;
  reporterId: string;
  reporterName: string;
};

type ReportsResponse = {
  content: ProductReport[];
  totalElements: number;
};

type ReportsQuery = {
  status?: ReportStatus;
  page: number;
  size: number;
};

type ConfirmAction = "status" | "visibility";

const statusOptions: {
  value: ReportStatus;
  label: string;
}[] = [
  { value: "PENDING", label: "Chờ xử lý" },
  { value: "REVIEWING", label: "Đang xem xét" },
  { value: "RESOLVED", label: "Đã giải quyết" },
  { value: "REJECTED", label: "Từ chối" },
];

const statusColors: Record<ReportStatus, string> = {
  PENDING: "gold",
  REVIEWING: "blue",
  RESOLVED: "green",
  REJECTED: "red",
};

const reasonLabels: Record<ReportReason, string> = {
  SCAM: "Lừa đảo",
  MISLEADING_INFORMATION: "Thông tin sai lệch",
  INAPPROPRIATE_CONTENT: "Nội dung không phù hợp",
  OTHER: "Lý do khác",
};

function isClosed(status: ReportStatus) {
  return status === "RESOLVED" || status === "REJECTED";
}

function StatusTag({ status }: { status: ReportStatus }) {
  return (
    <Tag color={statusColors[status]}>
      {statusOptions.find((item) => item.value === status)?.label}
    </Tag>
  );
}

function formatDate(value: string) {
  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? "Chưa có thông tin"
    : date.toLocaleString("vi-VN");
}

export default function ReportsPage() {
  const [messageApi, messageContext] = message.useMessage();

  const [reports, setReports] = useState<ProductReport[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);

  const [query, setQuery] = useState<ReportsQuery>({
    page: 1,
    size: 10,
  });

  const [selected, setSelected] = useState<ProductReport | null>(
    null,
  );
  const [nextStatus, setNextStatus] =
    useState<ReportStatus>("REVIEWING");
  const [reply, setReply] = useState("");
  const [actionError, setActionError] = useState("");
  const [saving, setSaving] = useState(false);
  const [confirmAction, setConfirmAction] =
    useState<ConfirmAction | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    async function fetchReports() {
      setLoading(true);
      setError("");

      try {
        const { data } = await api.get<ReportsResponse>(
          "/admin/reports",
          {
            params: {
              status: query.status,
              page: query.page - 1,
              size: query.size,
            },
            signal: controller.signal,
          },
        );

        if (controller.signal.aborted) return;

        // API hiện tại trả Spring Page, dữ liệu nằm trong content.
        setReports(data.content);
        setTotal(data.totalElements);
      } catch (cause) {
        if (axios.isCancel(cause) || controller.signal.aborted) {
          return;
        }

        setReports([]);
        setTotal(0);
        setError(getErrorMessage(cause));
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void fetchReports();

    return () => controller.abort();
  }, [query, reload]);

  function openReport(report: ProductReport) {
    setSelected(report);
    setReply(report.adminReply ?? "");
    setNextStatus(
      report.status === "PENDING" ? "REVIEWING" : report.status,
    );
    setActionError("");
  }

  function requestConfirmation(action: ConfirmAction) {
    if (!selected || saving) return;

    setActionError("");

    if (action === "status") {
      if (isClosed(selected.status)) return;

      if (isClosed(nextStatus) && !reply.trim()) {
        setActionError(
          "Cần nhập phản hồi khi giải quyết hoặc từ chối báo cáo.",
        );
        return;
      }

      if (reply.trim().length > 2000) {
        setActionError("Phản hồi không được quá 2000 ký tự.");
        return;
      }
    }

    if (
      action === "visibility" &&
      selected.productHidden === null
    ) {
      return;
    }

    setConfirmAction(action);
  }

  async function submitAction() {
    if (!selected || !confirmAction || saving) return;

    const action = confirmAction;

    setSaving(true);
    setActionError("");

    try {
      const url =
        action === "status"
          ? `/admin/reports/${selected.id}`
          : `/admin/reports/${selected.id}/product-visibility`;

      const payload =
        action === "status"
          ? {
              status: nextStatus,
              adminReply: reply.trim() || null,
            }
          : {
              hidden: !selected.productHidden,
            };

      const { data } = await api.patch<ProductReport>(
        url,
        payload,
      );

      setSelected(data);

      if (action === "status") {
        setReply(data.adminReply ?? "");
        setNextStatus(data.status);
      }

      setConfirmAction(null);

      void messageApi.success(
        action === "status"
          ? "Đã cập nhật báo cáo."
          : data.productHidden
            ? "Đã ẩn sản phẩm."
            : "Đã hiển thị lại sản phẩm.",
      );

      // Tải lại để cập nhật cả bộ lọc và các báo cáo cùng sản phẩm.
      setQuery((current) => ({
        ...current,
        page: 1,
      }));
    } catch (cause) {
      setActionError(getErrorMessage(cause));
    } finally {
      setSaving(false);
    }
  }

  const columns: TableColumnsType<ProductReport> = [
    {
      title: "Mã",
      dataIndex: "id",
      width: 90,
      render: (id: number) => (
        <Typography.Text strong>#{id}</Typography.Text>
      ),
    },
    {
      title: "Sản phẩm bị báo cáo",
      dataIndex: "productName",
      width: 230,
    },
    {
      title: "Người báo cáo",
      dataIndex: "reporterName",
      width: 190,
    },
    {
      title: "Lý do",
      dataIndex: "reason",
      width: 200,
      render: (reason: ReportReason) => reasonLabels[reason],
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      width: 150,
      render: (status: ReportStatus) => (
        <StatusTag status={status} />
      ),
    },
    {
      title: "Ngày gửi",
      dataIndex: "createdAt",
      width: 190,
      render: (value: string) => formatDate(value),
    },
    {
      title: "Thao tác",
      key: "actions",
      width: 110,
      render: (_, report) => (
        <Button
          type="text"
          icon={<EyeOutlined />}
          onClick={() => openReport(report)}
        >
          Chi tiết
        </Button>
      ),
    },
  ];

  return (
    <section className="panel">
      {messageContext}

      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
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
            Báo cáo vi phạm
          </Typography.Title>

          <Typography.Text type="secondary">
            Tiếp nhận phản ánh và xử lý nội dung trên nền tảng.
          </Typography.Text>
        </div>

        <Button
          icon={<ReloadOutlined />}
          loading={loading}
          disabled={saving}
          onClick={() => setReload((value) => value + 1)}
        >
          Tải lại
        </Button>
      </div>

      <Select<ReportStatus>
        placeholder="Tất cả trạng thái"
        aria-label="Lọc trạng thái báo cáo"
        allowClear
        disabled={saving}
        value={query.status}
        options={statusOptions}
        style={{ width: 210, marginBottom: 20 }}
        onChange={(status) => {
          setQuery((current) => ({
            ...current,
            status,
            page: 1,
          }));
        }}
      />

      {error && (
        <Alert
          type="error"
          showIcon
          title={error}
          style={{ marginBottom: 20 }}
        />
      )}

      <Table<ProductReport>
        rowKey="id"
        columns={columns}
        dataSource={reports}
        loading={loading}
        scroll={{ x: 1160 }}
        locale={{
          emptyText: error
            ? "Không tải được danh sách báo cáo"
            : "Không có báo cáo phù hợp",
        }}
        pagination={{
          current: query.page,
          pageSize: query.size,
          total,
          disabled: saving,
          showSizeChanger: true,
          pageSizeOptions: [10, 20, 50],
          showTotal: (count) => `${count} báo cáo`,
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
        title={`Báo cáo #${selected?.id ?? ""}`}
        open={selected !== null}
        onClose={() => {
          if (!saving && !confirmAction) setSelected(null);
        }}
        closable={!saving}
        maskClosable={!saving}
        keyboard={!saving}
        styles={{
          wrapper: { width: "min(680px, 100vw)" },
        }}
      >
        {selected && (
          <Space
            direction="vertical"
            size="large"
            style={{ width: "100%" }}
          >
            <Descriptions column={1} layout="vertical" bordered>
              <Descriptions.Item label="Sản phẩm">
                {selected.productName}
              </Descriptions.Item>

              <Descriptions.Item label="Mã sản phẩm">
                {selected.productId}
              </Descriptions.Item>

              <Descriptions.Item label="Người báo cáo">
                {selected.reporterName}
              </Descriptions.Item>

              <Descriptions.Item label="Mã người báo cáo">
                {selected.reporterId}
              </Descriptions.Item>

              <Descriptions.Item label="Lý do">
                {reasonLabels[selected.reason]}
              </Descriptions.Item>

              <Descriptions.Item label="Nội dung phản ánh">
                <div
                  style={{
                    whiteSpace: "pre-wrap",
                    overflowWrap: "anywhere",
                  }}
                >
                  {selected.description}
                </div>
              </Descriptions.Item>

              <Descriptions.Item label="Ngày gửi">
                {formatDate(selected.createdAt)}
              </Descriptions.Item>

              <Descriptions.Item label="Trạng thái xử lý">
                <StatusTag status={selected.status} />
              </Descriptions.Item>
            </Descriptions>

            <div>
              <Typography.Title level={5}>
                Ảnh bằng chứng
              </Typography.Title>

              {selected.imageUrls.length > 0 ? (
                <Image.PreviewGroup>
                  <Space wrap>
                    {selected.imageUrls.map((url, index) => (
                      <Image
                        key={`${url}-${index}`}
                        src={url}
                        alt={`Bằng chứng ${index + 1}`}
                        width={110}
                        height={110}
                        style={{
                          objectFit: "cover",
                          borderRadius: 12,
                        }}
                      />
                    ))}
                  </Space>
                </Image.PreviewGroup>
              ) : (
                <Typography.Text type="secondary">
                  Không có ảnh đính kèm.
                </Typography.Text>
              )}
            </div>

            <div
              style={{
                padding: 18,
                background: "#F6F8F6",
                borderRadius: 12,
              }}
            >
              <Typography.Title
                level={5}
                style={{ marginTop: 0 }}
              >
                Hiển thị sản phẩm
              </Typography.Title>

              <Space wrap>
                <Tag
                  color={
                    selected.productHidden === null
                      ? "default"
                      : selected.productHidden
                        ? "default"
                        : "green"
                  }
                >
                  {selected.productHidden === null
                    ? "Sản phẩm không còn tồn tại"
                    : selected.productHidden
                      ? "Đang ẩn"
                      : "Đang hiển thị"}
                </Tag>

                <Button
                  danger={selected.productHidden === false}
                  disabled={
                    saving || selected.productHidden === null
                  }
                  icon={
                    selected.productHidden
                      ? <EyeOutlined />
                      : <EyeInvisibleOutlined />
                  }
                  onClick={() =>
                    requestConfirmation("visibility")
                  }
                >
                  {selected.productHidden
                    ? "Hiển thị lại"
                    : "Ẩn sản phẩm"}
                </Button>
              </Space>

              <p style={{ color: "#788477", fontSize: 12 }}>
                Thay đổi hiển thị không tự đóng báo cáo.
              </p>
            </div>

            <div>
              <Typography.Title level={5}>
                Kết quả xử lý
              </Typography.Title>

              {isClosed(selected.status) ? (
                <>
                  <Alert
                    type="info"
                    showIcon
                    title="Báo cáo đã đóng"
                    description="Kết quả và phản hồi không thể chỉnh sửa."
                    style={{ marginBottom: 16 }}
                  />

                  <Typography.Paragraph
                    style={{
                      whiteSpace: "pre-wrap",
                      overflowWrap: "anywhere",
                    }}
                  >
                    {selected.adminReply || "Chưa có phản hồi"}
                  </Typography.Paragraph>
                </>
              ) : (
                <Space
                  direction="vertical"
                  size="middle"
                  style={{ width: "100%" }}
                >
                  <Select<ReportStatus>
                    aria-label="Kết quả xử lý báo cáo"
                    value={nextStatus}
                    disabled={saving}
                    options={statusOptions.filter(
                      (item) => item.value !== "PENDING",
                    )}
                    onChange={setNextStatus}
                    style={{ width: "100%" }}
                  />

                  <Input.TextArea
                    aria-label="Phản hồi cho người báo cáo"
                    placeholder="Nhập phản hồi cho người báo cáo..."
                    value={reply}
                    disabled={saving}
                    onChange={(event) =>
                      setReply(event.target.value)
                    }
                    maxLength={2000}
                    showCount
                    autoSize={{ minRows: 4, maxRows: 8 }}
                  />

                  <Button
                    type="primary"
                    disabled={saving}
                    onClick={() => requestConfirmation("status")}
                  >
                    Lưu kết quả
                  </Button>
                </Space>
              )}
            </div>

            {actionError && (
              <Alert
                type="error"
                showIcon
                title={actionError}
              />
            )}
          </Space>
        )}
      </Drawer>

      <Modal
        title={
          confirmAction === "status"
            ? "Xác nhận kết quả xử lý"
            : selected?.productHidden
              ? "Hiển thị lại sản phẩm"
              : "Ẩn sản phẩm"
        }
        open={confirmAction !== null}
        onCancel={() => {
          if (!saving) setConfirmAction(null);
        }}
        onOk={() => void submitAction()}
        confirmLoading={saving}
        okText="Xác nhận"
        cancelText="Hủy"
        cancelButtonProps={{ disabled: saving }}
        okButtonProps={{
          danger:
            confirmAction === "visibility" &&
            selected?.productHidden === false,
        }}
        closable={!saving}
        maskClosable={!saving}
        keyboard={!saving}
      >
        {confirmAction === "status" ? (
          <>
            <Typography.Paragraph>
              Chuyển báo cáo sang trạng thái{" "}
              <strong>
                {
                  statusOptions.find(
                    (item) => item.value === nextStatus,
                  )?.label
                }
              </strong>
              ?
            </Typography.Paragraph>

            {isClosed(nextStatus) && (
              <Alert
                type="warning"
                showIcon
                title="Báo cáo sẽ được đóng"
                description="Sau khi lưu, không thể sửa lại kết quả và phản hồi."
              />
            )}
          </>
        ) : (
          <Typography.Paragraph>
            Bạn muốn{" "}
            {selected?.productHidden ? "hiển thị lại" : "ẩn"} sản
            phẩm <strong>{selected?.productName}</strong>?
          </Typography.Paragraph>
        )}

        {actionError && (
          <Alert
            type="error"
            showIcon
            title={actionError}
            style={{ marginTop: 16 }}
          />
        )}
      </Modal>
    </section>
  );
}