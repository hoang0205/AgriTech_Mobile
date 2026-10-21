import { useEffect, useState } from "react";
import {
  Alert,
  Button,
  Input,
  Space,
  Spin,
  Table,
  Typography,
} from "antd";
import { DownloadOutlined, SearchOutlined } from "@ant-design/icons";
import axios from "axios";
import { api, getErrorMessage } from "../api";

type DailyStatistics = {
  date: string;
  orders: number;
  revenue: number;
};

type StatusStatistics = {
  status: string;
  count: number;
};

type TopProduct = {
  productId: string;
  productName: string;
  unit: string;
  quantity: number;
  revenue: number;
};

type Statistics = {
  from: string;
  to: string;
  totalOrders: number;
  completedOrders: number;
  completedRevenue: number;
  daily: DailyStatistics[];
  statuses: StatusStatistics[];
  topProducts: TopProduct[];
};

type DateRange = {
  from: string;
  to: string;
};

const currency = new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
});

const numberFormatter = new Intl.NumberFormat("vi-VN", {
  maximumFractionDigits: 3,
});

const compactMoney = new Intl.NumberFormat("vi-VN", {
  notation: "compact",
  maximumFractionDigits: 1,
});

const statusLabels: Record<string, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  SHIPPING: "Đang giao",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy",
};

function dateInputValue(date: Date) {
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, "0"),
    String(date.getDate()).padStart(2, "0"),
  ].join("-");
}

function defaultRange(): DateRange {
  const to = new Date();
  const from = new Date(to);
  from.setDate(from.getDate() - 29);

  return {
    from: dateInputValue(from),
    to: dateInputValue(to),
  };
}

function displayDate(value: string) {
  return value.split("-").reverse().join("/");
}

function RevenueChart({ daily }: { daily: DailyStatistics[] }) {
  const maximum = Math.max(1, ...daily.map((item) => item.revenue));
  const left = 82;
  const top = 20;
  const width = 650;
  const height = 150;
  const baseline = top + height;
  const step = width / Math.max(daily.length, 1);

  const labelIndexes = [
    ...new Set([
      0,
      Math.floor((daily.length - 1) / 2),
      daily.length - 1,
    ]),
  ].filter((index) => index >= 0);

  return (
    <svg
      viewBox="0 0 760 225"
      role="img"
      aria-label="Biểu đồ doanh thu đơn hoàn thành theo ngày đặt đơn"
      style={{ display: "block", width: "100%", height: "auto" }}
    >
      <title>
        Doanh thu theo ngày đặt đơn. Số liệu chi tiết có trong bảng
        thống kê theo ngày bên dưới.
      </title>

      {[0, 0.5, 1].map((ratio) => {
        const y = baseline - ratio * height;

        return (
          <g key={ratio}>
            <line
              x1={left}
              x2={left + width}
              y1={y}
              y2={y}
              stroke="#E5EBE4"
              strokeDasharray={ratio === 0 ? undefined : "4 4"}
            />
            <text
              x={left - 10}
              y={y + 4}
              textAnchor="end"
              fill="#788477"
              fontSize="11"
            >
              {compactMoney.format(maximum * ratio)}
            </text>
          </g>
        );
      })}

      {daily.map((item, index) => {
        const barHeight = (item.revenue / maximum) * height;

        return (
          <rect
            key={item.date}
            x={left + index * step + step * 0.15}
            y={baseline - barHeight}
            width={step * 0.7}
            height={barHeight}
            rx={Math.min(3, step * 0.15)}
            fill="#1B6E2A"
          >
            <title>
              {displayDate(item.date)}: {currency.format(item.revenue)}
            </title>
          </rect>
        );
      })}

      {labelIndexes.map((index) => (
        <text
          key={index}
          x={
            daily.length === 1
              ? left + width / 2
              : index === 0
                ? left
                : index === daily.length - 1
                  ? left + width
                  : left + index * step + step / 2
          }
          y={baseline + 27}
          textAnchor={
            daily.length === 1
              ? "middle"
              : index === 0
                ? "start"
                : index === daily.length - 1
                  ? "end"
                  : "middle"
          }
          fill="#788477"
          fontSize="11"
        >
          {displayDate(daily[index].date)}
        </text>
      ))}

      <text
        x={left}
        y={12}
        fill="#788477"
        fontSize="10"
      >
        VND
      </text>
    </svg>
  );
}

export default function StatisticsPage() {
  const [query, setQuery] = useState<DateRange>(defaultRange);
  const [draft, setDraft] = useState<DateRange>(query);
  const [data, setData] = useState<Statistics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [inputError, setInputError] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    async function fetchStatistics() {
      setLoading(true);
      setError("");

      try {
        const { data: response } = await api.get<Statistics>(
          "/admin/statistics",
          {
            params: query,
            signal: controller.signal,
          },
        );

        if (!controller.signal.aborted) setData(response);
      } catch (cause) {
        if (axios.isCancel(cause) || controller.signal.aborted) {
          return;
        }

        setData(null);
        setError(getErrorMessage(cause));
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void fetchStatistics();

    return () => controller.abort();
  }, [query]);

  function applyRange() {
    const fromTime = Date.parse(draft.from);
    const toTime = Date.parse(draft.to);
    const dayDifference = (toTime - fromTime) / 86400000;

    if (
      !draft.from ||
      !draft.to ||
      !Number.isFinite(dayDifference) ||
      dayDifference < 0 ||
      dayDifference > 365
    ) {
      setInputError(
        "Chọn ngày bắt đầu không sau ngày kết thúc, tối đa 366 ngày.",
      );
      return;
    }

    setInputError("");
    setQuery({ ...draft });
  }

  function exportCsv() {
    if (!data) return;

    const rows = [
      ["Ngày đặt đơn", "Số đơn", "Doanh thu đơn hoàn thành (VND)"],
      ...data.daily.map((item) => [
        item.date,
        item.orders,
        item.revenue,
      ]),
    ];

    const csv = rows
      .map((row) =>
        row
          .map((value) => `"${String(value).replaceAll('"', '""')}"`)
          .join(","),
      )
      .join("\r\n");

    const blob = new Blob(["\uFEFF", csv], {
      type: "text/csv;charset=utf-8;",
    });

    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `agritech-thong-ke-${data.from}-${data.to}.csv`;

    document.body.appendChild(link);
    link.click();
    link.remove();

    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  return (
    <Space
      direction="vertical"
      size="large"
      style={{ width: "100%" }}
    >
      <section className="panel">
        <Typography.Title level={4} style={{ marginTop: 0 }}>
          Thống kê kinh doanh
        </Typography.Title>

        <Space wrap size={12} align="end">
          <div>
            <label
              htmlFor="statistics-from"
              style={{ display: "block", marginBottom: 6 }}
            >
              Từ ngày
            </label>
            <Input
              id="statistics-from"
              type="date"
              min="2000-01-01"
              max="2100-12-31"
              value={draft.from}
              onChange={(event) =>
                setDraft((current) => ({
                  ...current,
                  from: event.target.value,
                }))
              }
            />
          </div>

          <div>
            <label
              htmlFor="statistics-to"
              style={{ display: "block", marginBottom: 6 }}
            >
              Đến ngày
            </label>
            <Input
              id="statistics-to"
              type="date"
              min="2000-01-01"
              max="2100-12-31"
              value={draft.to}
              onChange={(event) =>
                setDraft((current) => ({
                  ...current,
                  to: event.target.value,
                }))
              }
            />
          </div>

          <Button
            type="primary"
            icon={<SearchOutlined />}
            loading={loading}
            onClick={applyRange}
          >
            Xem thống kê
          </Button>

          <Button
            icon={<DownloadOutlined />}
            disabled={!data || loading}
            onClick={exportCsv}
          >
            Xuất CSV theo ngày
          </Button>
        </Space>

        {(inputError || error) && (
          <Alert
            type="error"
            showIcon
            title={inputError || error}
            style={{ marginTop: 16 }}
          />
        )}

        <Typography.Paragraph
          type="secondary"
          style={{ margin: "18px 0 0", fontSize: 12 }}
        >
          Lọc theo ngày đặt đơn. Doanh thu chỉ tính đơn hiện đã hoàn
          thành, chưa phải tiền COD đối soát thực thu.
        </Typography.Paragraph>
      </section>

      {loading ? (
        <div style={{ padding: 60, textAlign: "center" }}>
          <Spin size="large" />
        </div>
      ) : data ? (
        <>
          <Typography.Text type="secondary">
            Kết quả từ {displayDate(data.from)} đến{" "}
            {displayDate(data.to)}, bao gồm cả hai ngày.
          </Typography.Text>

          <div
            className="metrics-grid"
            style={{
              margin: 0,
              gridTemplateColumns:
                "repeat(auto-fit, minmax(min(100%, 200px), 1fr))",
            }}
          >
            {[
              {
                label: "Tổng đơn hàng",
                value: numberFormatter.format(data.totalOrders),
              },
              {
                label: "Đơn hoàn thành",
                value: numberFormatter.format(data.completedOrders),
              },
              {
                label: "Doanh thu đơn hoàn thành",
                value: currency.format(data.completedRevenue),
              },
            ].map((item) => (
              <section className="metric-card" key={item.label}>
                <Typography.Text type="secondary">
                  {item.label}
                </Typography.Text>
                <div
                  className="metric-value"
                  style={{
                    fontSize: 25,
                    overflowWrap: "anywhere",
                  }}
                >
                  {item.value}
                </div>
              </section>
            ))}
          </div>

          <section className="panel">
            <Typography.Title level={5} style={{ marginTop: 0 }}>
              Doanh thu theo ngày đặt đơn
            </Typography.Title>

            {data.totalOrders === 0 && (
              <Alert
                type="info"
                showIcon
                title="Không có đơn hàng trong khoảng ngày đã chọn."
                style={{ marginBottom: 16 }}
              />
            )}

            <RevenueChart daily={data.daily} />
          </section>

          <section className="panel">
            <Typography.Title level={5} style={{ marginTop: 0 }}>
              Đơn hàng theo trạng thái
            </Typography.Title>

            <Table<StatusStatistics>
              rowKey="status"
              dataSource={data.statuses}
              pagination={false}
              size="small"
              locale={{ emptyText: "Không có đơn hàng" }}
              columns={[
                {
                  title: "Trạng thái",
                  dataIndex: "status",
                  render: (status: string) =>
                    statusLabels[status] ?? status,
                },
                {
                  title: "Số đơn",
                  dataIndex: "count",
                  align: "right",
                  render: (value: number) =>
                    numberFormatter.format(value),
                },
              ]}
            />
          </section>

          <section className="panel">
            <Typography.Title level={5} style={{ marginTop: 0 }}>
              Top 10 sản phẩm theo doanh thu
            </Typography.Title>

            <Typography.Paragraph type="secondary">
              Chỉ tính sản phẩm trong đơn hoàn thành.
            </Typography.Paragraph>

            <Table<TopProduct>
              rowKey="productId"
              dataSource={data.topProducts}
              pagination={false}
              scroll={{ x: 560 }}
              locale={{ emptyText: "Chưa có sản phẩm bán thành công" }}
              columns={[
                {
                  title: "Sản phẩm",
                  dataIndex: "productName",
                },
                {
                  title: "Số lượng bán",
                  dataIndex: "quantity",
                  render: (value: number, item) =>
                    `${numberFormatter.format(value)} ${item.unit}`,
                },
                {
                  title: "Doanh thu",
                  dataIndex: "revenue",
                  align: "right",
                  render: (value: number) => currency.format(value),
                },
              ]}
            />
          </section>

          <section className="panel">
            <Typography.Title level={5} style={{ marginTop: 0 }}>
              Số liệu theo ngày
            </Typography.Title>

            <Table<DailyStatistics>
              rowKey="date"
              dataSource={data.daily}
              scroll={{ x: 520 }}
              pagination={{
                pageSize: 10,
                showSizeChanger: false,
                hideOnSinglePage: true,
              }}
              columns={[
                {
                  title: "Ngày đặt đơn",
                  dataIndex: "date",
                  render: (value: string) => displayDate(value),
                },
                {
                  title: "Số đơn",
                  dataIndex: "orders",
                  render: (value: number) =>
                    numberFormatter.format(value),
                },
                {
                  title: "Doanh thu đơn hoàn thành",
                  dataIndex: "revenue",
                  align: "right",
                  render: (value: number) => currency.format(value),
                },
              ]}
            />
          </section>
        </>
      ) : null}
    </Space>
  );
}