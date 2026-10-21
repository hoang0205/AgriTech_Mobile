import { useEffect, useState } from "react";
import { Alert, Button, Skeleton, Typography } from "antd";
import {
  BarChartOutlined,
  FileSearchOutlined,
  ReloadOutlined,
  ShoppingCartOutlined,
  ShopOutlined,
  TeamOutlined,
} from "@ant-design/icons";
import axios from "axios";
import { api, getErrorMessage } from "../api";

type Overview = {
  users: number;
  visibleProducts: number;
  orders: number;
  completedOrders: number;
  completedRevenue: number;
  pendingReports: number;
  reviewingReports: number;
};

const numberFormatter = new Intl.NumberFormat("vi-VN");

const currencyFormatter = new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
  maximumFractionDigits: 0,
});

export default function DashboardMetrics() {
  const [data, setData] = useState<Overview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    async function fetchOverview() {
      setLoading(true);
      setError("");

      try {
        const { data: response } = await api.get<Overview>(
          "/admin/overview",
          { signal: controller.signal },
        );

        if (controller.signal.aborted) return;

        setData(response);
        setUpdatedAt(new Date());
      } catch (cause) {
        if (axios.isCancel(cause) || controller.signal.aborted) {
          return;
        }

        // Không biến lỗi kết nối thành các chỉ số bằng 0.
        setData(null);
        setUpdatedAt(null);
        setError(getErrorMessage(cause));
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void fetchOverview();

    return () => controller.abort();
  }, [reload]);

  const cards = [
    {
      label: "Người dùng",
      value: data ? numberFormatter.format(data.users) : "—",
      note: "Không bao gồm tài khoản Admin",
      icon: <TeamOutlined />,
    },
    {
      label: "Sản phẩm hiển thị",
      value: data
        ? numberFormatter.format(data.visibleProducts)
        : "—",
      note: "Không bao gồm sản phẩm đang ẩn",
      icon: <ShopOutlined />,
    },
    {
      label: "Đơn hàng",
      value: data ? numberFormatter.format(data.orders) : "—",
      note: data
        ? `${numberFormatter.format(data.completedOrders)} đơn hoàn thành`
        : "Tất cả trạng thái đơn hàng",
      icon: <ShoppingCartOutlined />,
    },
    {
      label: "Doanh thu đơn hoàn thành",
      value: data
        ? currencyFormatter.format(data.completedRevenue)
        : "—",
      note: "Tổng giá trị các đơn COMPLETED",
      icon: <BarChartOutlined />,
    },
    {
      label: "Báo cáo chưa đóng",
      value: data
        ? numberFormatter.format(
            data.pendingReports + data.reviewingReports,
          )
        : "—",
      note: data
        ? `${numberFormatter.format(data.pendingReports)} chờ xử lý · ${numberFormatter.format(data.reviewingReports)} đang xem xét`
        : "Chờ xử lý và đang xem xét",
      icon: <FileSearchOutlined />,
    },
  ];

  return (
    <section style={{ marginTop: 24 }}>
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          flexWrap: "wrap",
          gap: 12,
          marginBottom: 16,
        }}
      >
        <div>
          <Typography.Title
            level={5}
            style={{ margin: "0 0 5px" }}
          >
            Tổng quan hệ thống
          </Typography.Title>

          <Typography.Text
            type="secondary"
            style={{ fontSize: 12 }}
          >
            Toàn bộ thời gian
            {updatedAt &&
              ` · Cập nhật lúc ${updatedAt.toLocaleTimeString("vi-VN")}`}
          </Typography.Text>
        </div>

        <Button
          icon={<ReloadOutlined />}
          loading={loading}
          onClick={() => setReload((value) => value + 1)}
        >
          Cập nhật
        </Button>
      </div>

      {error && (
        <Alert
          type="error"
          showIcon
          title="Không tải được số liệu tổng quan"
          description={error}
          style={{ marginBottom: 16 }}
        />
      )}

      <div
        className="metrics-grid"
        style={{
          marginTop: 0,
          gridTemplateColumns:
            "repeat(auto-fit, minmax(min(100%, 190px), 1fr))",
        }}
      >
        {cards.map((card) => (
          <section className="metric-card" key={card.label}>
            <div className="metric-top">
              <span>{card.label}</span>
              <span className="metric-icon">{card.icon}</span>
            </div>

            <div
              className="metric-value"
              style={{
                fontSize: 25,
                overflowWrap: "anywhere",
              }}
            >
              {loading ? (
                <Skeleton.Input
                  active
                  size="small"
                  style={{ width: 100, minWidth: 0 }}
                />
              ) : (
                card.value
              )}
            </div>

            <p>{card.note}</p>
          </section>
        ))}
      </div>

      <Typography.Paragraph
        type="secondary"
        style={{ fontSize: 12, marginBottom: 24 }}
      >
        Doanh thu tính theo tổng tiền đơn hoàn thành, chưa phải số
        tiền COD được đối soát thực thu.
      </Typography.Paragraph>
    </section>
  );
}