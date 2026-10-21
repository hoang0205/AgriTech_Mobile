import { useEffect, useState } from "react";
import {
  Alert,
  Avatar,
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
  EyeOutlined,
  PictureOutlined,
  ReloadOutlined,
} from "@ant-design/icons";
import axios from "axios";
import { api, getErrorMessage } from "../api";

type OrderProductPreview = {
  id: string;
  productName: string;
  quantity: number;
  unit: string;
  thumbnail: string | null;
};

type AdminOrder = {
  id: string;
  buyerName: string;
  phoneNumber: string;
  shippingAddress: string;
  totalAmount: number;
  status: string;
  orderDate: string;
  products: OrderProductPreview[];
};

type OrderItem = {
  id: string;
  productId: string;
  productName: string;
  farmerName: string;
  quantity: number;
  unit: string;
  price: number;
  subtotal: number;
};

type OrdersResponse = {
  items: AdminOrder[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
};

type OrderDetail = {
  order: AdminOrder;
  items: OrderItem[];
};

type OrdersQuery = {
  keyword: string;
  status?: string;
  page: number;
  size: number;
};

const statuses = [
  { value: "PENDING", label: "Chờ xác nhận" },
  { value: "CONFIRMED", label: "Đã xác nhận" },
  { value: "SHIPPING", label: "Đang giao" },
  { value: "COMPLETED", label: "Hoàn thành" },
  { value: "CANCELLED", label: "Đã hủy" },
];

const statusColors: Record<string, string> = {
  PENDING: "gold",
  CONFIRMED: "blue",
  SHIPPING: "cyan",
  COMPLETED: "green",
  CANCELLED: "red",
};

const currency = new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
});

const quantityFormatter = new Intl.NumberFormat("vi-VN", {
  maximumFractionDigits: 3,
});

function formatDate(value: string) {
  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? "Chưa có thông tin"
    : date.toLocaleString("vi-VN");
}

function OrderStatusTag({ status }: { status: string }) {
  const label = statuses.find((item) => item.value === status)?.label;

  return (
    <Tag color={statusColors[status] ?? "default"}>
      {label ?? status}
    </Tag>
  );
}

function OrderProductsCell({
  products,
}: {
  products: OrderProductPreview[];
}) {
  const [expanded, setExpanded] = useState(false);

  if (products.length === 0) {
    return (
      <Typography.Text type="secondary">
        Chưa có sản phẩm
      </Typography.Text>
    );
  }

  const visibleProducts = expanded ? products : products.slice(0, 2);
  const remaining = products.length - 2;

  return (
    <div style={{ paddingBlock: 4 }}>
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          gap: 12,
        }}
      >
        {visibleProducts.map((product) => (
          <div
            key={product.id}
            style={{
              display: "flex",
              alignItems: "center",
              gap: 10,
            }}
          >
            <Avatar
              shape="square"
              size={42}
              src={product.thumbnail || undefined}
              icon={<PictureOutlined />}
              style={{
                flexShrink: 0,
                borderRadius: 10,
                background: "#F1F3F1",
                color: "#788477",
              }}
            />

            <div style={{ minWidth: 0 }}>
              <Typography.Text
                style={{
                  display: "block",
                  fontWeight: 500,
                  fontSize: 13,
                  overflowWrap: "anywhere",
                }}
              >
                {product.productName}
              </Typography.Text>

              <Typography.Text
                type="secondary"
                style={{ fontSize: 12 }}
              >
                Số lượng:{" "}
                {quantityFormatter.format(product.quantity)}{" "}
                {product.unit}
              </Typography.Text>
            </div>
          </div>
        ))}
      </div>

      {products.length > 2 && (
        <Button
          type="link"
          size="small"
          aria-expanded={expanded}
          onClick={() => setExpanded((current) => !current)}
          style={{
            paddingInline: 0,
            marginTop: 8,
            height: "auto",
            fontSize: 12,
          }}
        >
          {expanded
            ? "Thu gọn"
            : `Xem thêm ${remaining} sản phẩm`}
        </Button>
      )}
    </div>
  );
}

export default function OrdersPage() {
  const [orders, setOrders] = useState<AdminOrder[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchText, setSearchText] = useState("");
  const [reload, setReload] = useState(0);

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [detail, setDetail] = useState<OrderDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState("");
  const [detailReload, setDetailReload] = useState(0);

  const [query, setQuery] = useState<OrdersQuery>({
    keyword: "",
    page: 1,
    size: 10,
  });

  useEffect(() => {
    const controller = new AbortController();

    async function fetchOrders() {
      setLoading(true);
      setError("");

      try {
        const { data } = await api.get<OrdersResponse>(
          "/admin/orders",
          {
            params: {
              keyword: query.keyword,
              status: query.status,
              page: query.page - 1,
              size: query.size,
            },
            signal: controller.signal,
          },
        );

        if (controller.signal.aborted) return;

        setOrders(data.items);
        setTotal(data.totalElements);
      } catch (cause) {
        if (axios.isCancel(cause) || controller.signal.aborted) {
          return;
        }

        setOrders([]);
        setTotal(0);
        setError(getErrorMessage(cause));
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void fetchOrders();

    return () => controller.abort();
  }, [query, reload]);

  useEffect(() => {
    if (selectedId === null) return;

    const controller = new AbortController();

    async function fetchDetail() {
      setDetailLoading(true);
      setDetailError("");
      setDetail(null);

      try {
        const { data } = await api.get<OrderDetail>(
          `/admin/orders/${encodeURIComponent(selectedId!)}`,
          { signal: controller.signal },
        );

        if (!controller.signal.aborted) setDetail(data);
      } catch (cause) {
        if (axios.isCancel(cause) || controller.signal.aborted) {
          return;
        }

        setDetailError(getErrorMessage(cause));
      } finally {
        if (!controller.signal.aborted) setDetailLoading(false);
      }
    }

    void fetchDetail();

    return () => controller.abort();
  }, [selectedId, detailReload]);

  function openDetail(id: string) {
    setDetail(null);
    setDetailError("");
    setDetailLoading(true);
    setSelectedId(id);
  }

  const columns: TableColumnsType<AdminOrder> = [
    {
      title: "Mã đơn",
      dataIndex: "id",
      width: 110,
      render: (id: string) => (
        <Typography.Text strong>#{id}</Typography.Text>
      ),
    },
    {
  title: "Sản phẩm",
  key: "products",
  width: 290,
  render: (_, order) => (
    <OrderProductsCell products={order.products ?? []} />
  ),
},
    {
      title: "Người mua",
      dataIndex: "buyerName",
      width: 210,
      render: (name: string, order) => (
        <div>
          <Typography.Text strong>{name}</Typography.Text>
          <div>
            <Typography.Text
              type="secondary"
              style={{ fontSize: 12 }}
            >
              {order.phoneNumber}
            </Typography.Text>
          </div>
        </div>
      ),
    },
    {
      title: "Ngày đặt",
      dataIndex: "orderDate",
      width: 190,
      render: (value: string) => formatDate(value),
    },
    {
      title: "Tổng tiền",
      dataIndex: "totalAmount",
      width: 160,
      render: (value: number) => currency.format(value),
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      width: 160,
      render: (status: string) => (
        <OrderStatusTag status={status} />
      ),
    },
    {
      title: "Thao tác",
      key: "actions",
      width: 110,
      render: (_, order) => (
        <Button
          type="text"
          icon={<EyeOutlined />}
          onClick={() => openDetail(order.id)}
        >
          Chi tiết
        </Button>
      ),
    },
  ];

  const itemColumns: TableColumnsType<OrderItem> = [
    {
      title: "Sản phẩm",
      dataIndex: "productName",
      width: 220,
      render: (name: string, item) => (
        <div>
          <Typography.Text strong>{name}</Typography.Text>
          <div>
            <Typography.Text type="secondary">
              {item.farmerName}
            </Typography.Text>
          </div>
        </div>
      ),
    },
    {
      title: "Số lượng",
      dataIndex: "quantity",
      width: 120,
      render: (quantity: number, item) =>
        `${quantityFormatter.format(quantity)} ${item.unit}`,
    },
    {
      title: "Đơn giá lúc đặt",
      dataIndex: "price",
      width: 150,
      render: (value: number) => currency.format(value),
    },
    {
      title: "Thành tiền",
      dataIndex: "subtotal",
      width: 150,
      render: (value: number) => currency.format(value),
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
          marginBottom: 24,
        }}
      >
        <div>
          <Typography.Title
            level={4}
            style={{ margin: "0 0 6px" }}
          >
            Danh sách đơn hàng
          </Typography.Title>
          <Typography.Text type="secondary">
            Tra cứu đơn hàng và theo dõi quá trình giao hàng.
          </Typography.Text>
        </div>

        <Button
          icon={<ReloadOutlined />}
          loading={loading}
          onClick={() => setReload((value) => value + 1)}
        >
          Tải lại
        </Button>
      </div>

      <Space wrap size={12} style={{ marginBottom: 20 }}>
        <Input.Search
          placeholder="Tìm mã đơn, người mua, số điện thoại"
          aria-label="Tìm đơn hàng"
          allowClear
          value={searchText}
          onChange={(event) => setSearchText(event.target.value)}
          onSearch={(value) => {
            setQuery((current) => ({
              ...current,
              keyword: value.trim(),
              page: 1,
            }));
          }}
          style={{ width: "min(340px, 100%)" }}
        />

        <Select<string>
          placeholder="Tất cả trạng thái"
          aria-label="Lọc trạng thái đơn hàng"
          allowClear
          value={query.status}
          options={statuses}
          style={{ width: 190 }}
          onChange={(status) => {
            setQuery((current) => ({
              ...current,
              status,
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

      <Table<AdminOrder>
        rowKey="id"
        columns={columns}
        dataSource={orders}
        loading={loading}
        scroll={{ x: 1230 }}
        onRow={() => ({
  style: { verticalAlign: "top" },
})}
        locale={{
          emptyText: error
            ? "Không tải được danh sách đơn hàng"
            : "Không tìm thấy đơn hàng",
        }}
        pagination={{
          current: query.page,
          pageSize: query.size,
          total,
          showSizeChanger: true,
          pageSizeOptions: [10, 20, 50],
          showTotal: (count) => `${count} đơn hàng`,
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
        title={`Chi tiết đơn hàng #${selectedId ?? ""}`}
        open={selectedId !== null}
        onClose={() => setSelectedId(null)}
        styles={{
          wrapper: { width: "min(780px, 100vw)" },
        }}
      >
        {detailLoading ? (
          <div style={{ padding: 60, textAlign: "center" }}>
            <Spin size="large" />
          </div>
        ) : detailError ? (
          <Space direction="vertical" style={{ width: "100%" }}>
            <Alert type="error" showIcon title={detailError} />
            <Button
              onClick={() => setDetailReload((value) => value + 1)}
            >
              Thử lại
            </Button>
          </Space>
        ) : detail ? (
          <>
            <Descriptions column={1} bordered>
              <Descriptions.Item label="Mã đơn">
                #{detail.order.id}
              </Descriptions.Item>
              <Descriptions.Item label="Người mua">
                {detail.order.buyerName}
              </Descriptions.Item>
              <Descriptions.Item label="Số điện thoại nhận hàng">
                {detail.order.phoneNumber}
              </Descriptions.Item>
              <Descriptions.Item label="Địa chỉ giao hàng">
                <span style={{ overflowWrap: "anywhere" }}>
                  {detail.order.shippingAddress}
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="Ngày đặt">
                {formatDate(detail.order.orderDate)}
              </Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <OrderStatusTag status={detail.order.status} />
              </Descriptions.Item>
              <Descriptions.Item label="Tổng tiền đơn hàng">
                <Typography.Text strong>
                  {currency.format(detail.order.totalAmount)}
                </Typography.Text>
              </Descriptions.Item>
            </Descriptions>

            <Typography.Title
              level={5}
              style={{ marginTop: 28 }}
            >
              Sản phẩm trong đơn
            </Typography.Title>

            <Table<OrderItem>
              rowKey="id"
              columns={itemColumns}
              dataSource={detail.items}
              pagination={false}
              scroll={{ x: 640 }}
              size="small"
              locale={{ emptyText: "Đơn hàng chưa có sản phẩm" }}
            />
          </>
        ) : (
          <Empty description="Chưa có thông tin đơn hàng" />
        )}
      </Drawer>
    </section>
  );
}