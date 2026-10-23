import { useEffect, useState } from "react";
import {
  Alert,
  Button,
  Descriptions,
  Drawer,
  Image,
  Input,
  Select,
  Modal,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from "antd";
import type { TableColumnsType } from "antd";
import {
  EyeOutlined,
  PictureOutlined,
  ReloadOutlined,
  EyeInvisibleOutlined,
} from "@ant-design/icons";
import axios from "axios";
import { api, getErrorMessage } from "../api";

type AdminProduct = {
  id: string;
  name: string;
  category: string;
  price: number;
  quantity: number;
  unit: string;
  description: string;
  imageUrls: string[];
  farmerId: string;
  farmerName: string;
  hidden: boolean;
};

type ProductsResponse = {
  items: AdminProduct[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
};

type Visibility = "visible" | "hidden";

type ProductsQuery = {
  keyword: string;
  category?: string;
  visibility?: Visibility;
  page: number;
  size: number;
};

const categories = [
  "Rau củ",
  "Trái cây",
  "Thịt",
  "Thủy hải sản",
  "Sản phẩm khác",
];

const currencyFormatter = new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
});

const quantityFormatter = new Intl.NumberFormat("vi-VN", {
  maximumFractionDigits: 3,
});

function VisibilityTag({ hidden }: { hidden: boolean }) {
  return (
    <Tag color={hidden ? "default" : "green"}>
      {hidden ? "Đang ẩn" : "Đang hiển thị"}
    </Tag>
  );
}

function ProductThumbnail({ url }: { url?: string }) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);

  if (!url || failedUrl === url) {
    return (
      <div
        style={{
          width: 48,
          height: 48,
          flexShrink: 0,
          display: "grid",
          placeItems: "center",
          borderRadius: 10,
          background: "#F1F3F1",
          color: "#9E9E9E",
          fontSize: 21,
        }}
      >
        <PictureOutlined />
      </div>
    );
  }

  return (
    <img
      src={url}
      alt=""
      loading="lazy"
      onError={() => setFailedUrl(url)}
      style={{
        width: 48,
        height: 48,
        flexShrink: 0,
        objectFit: "cover",
        borderRadius: 10,
        background: "#F1F3F1",
      }}
    />
  );
}

export default function ProductsPage() {
  const [products, setProducts] = useState<AdminProduct[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchText, setSearchText] = useState("");
  const [reload, setReload] = useState(0);
  const [messageApi, messageContext] = message.useMessage();

const [visibilityTarget, setVisibilityTarget] =
  useState<AdminProduct | null>(null);

const [savingVisibility, setSavingVisibility] = useState(false);
const [visibilityError, setVisibilityError] = useState("");

  const [selectedProduct, setSelectedProduct] =
    useState<AdminProduct | null>(null);

  const [query, setQuery] = useState<ProductsQuery>({
    keyword: "",
    page: 1,
    size: 10,
  });

  useEffect(() => {
    const controller = new AbortController();

    async function fetchProducts() {
      setLoading(true);
      setError("");

      try {
        const { data } = await api.get<ProductsResponse>(
          "/admin/products",
          {
            params: {
              keyword: query.keyword,
              category: query.category,
              hidden:
                query.visibility === undefined
                  ? undefined
                  : query.visibility === "hidden",
              page: query.page - 1,
              size: query.size,
            },
            signal: controller.signal,
          },
        );

        if (controller.signal.aborted) return;

        setProducts(data.items);
        setTotal(data.totalElements);
      } catch (cause) {
        if (axios.isCancel(cause) || controller.signal.aborted) {
          return;
        }

        setProducts([]);
        setTotal(0);
        setError(getErrorMessage(cause));
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    void fetchProducts();

    return () => controller.abort();
  }, [query, reload]);

  function openVisibilityConfirm(product: AdminProduct) {
  setVisibilityError("");
  setVisibilityTarget(product);
}

function closeVisibilityConfirm() {
  if (savingVisibility) return;

  setVisibilityTarget(null);
  setVisibilityError("");
}

async function updateVisibility() {
  if (!visibilityTarget || savingVisibility) return;

  const target = visibilityTarget;

  setSavingVisibility(true);
  setVisibilityError("");

  try {
    const { data } = await api.patch<{
      id: string;
      hidden: boolean;
    }>(
      `/admin/products/${encodeURIComponent(target.id)}/visibility`,
      {
        hidden: !target.hidden,
      },
    );

    setSelectedProduct((current) =>
      current?.id === data.id
        ? { ...current, hidden: data.hidden }
        : current,
    );

    setVisibilityTarget(null);

    void messageApi.success(
      data.hidden
        ? "Đã ẩn sản phẩm."
        : "Đã hiển thị lại sản phẩm.",
    );

    setLoading(true);
    setQuery((current) => ({
      ...current,
      page: 1,
    }));
  } catch (cause) {
    setVisibilityError(getErrorMessage(cause));
  } finally {
    setSavingVisibility(false);
  }
}

  const columns: TableColumnsType<AdminProduct> = [
    {
      title: "Sản phẩm",
      key: "product",
      width: 270,
      render: (_, product) => (
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 12,
          }}
        >
          <ProductThumbnail url={product.imageUrls[0]} />

          <div style={{ minWidth: 0 }}>
            <Typography.Text strong>
              {product.name}
            </Typography.Text>

            <div>
              <Typography.Text
                type="secondary"
                style={{ fontSize: 12 }}
              >
                {product.category}
              </Typography.Text>
            </div>
          </div>
        </div>
      ),
    },
    {
      title: "Người bán",
      dataIndex: "farmerName",
      width: 180,
      ellipsis: true,
    },
    {
      title: "Giá bán",
      dataIndex: "price",
      width: 160,
      render: (price: number, product) => (
        <span>
          {currencyFormatter.format(price)}
          <Typography.Text type="secondary">
            /{product.unit}
          </Typography.Text>
        </span>
      ),
    },
    {
      title: "Tồn kho",
      dataIndex: "quantity",
      width: 130,
      render: (quantity: number, product) => (
        <Typography.Text type={quantity <= 0 ? "danger" : undefined}>
          {quantityFormatter.format(quantity)} {product.unit}
        </Typography.Text>
      ),
    },
    {
      title: "Trạng thái",
      dataIndex: "hidden",
      width: 150,
      render: (hidden: boolean) => (
        <VisibilityTag hidden={hidden} />
      ),
    },
    {
  title: "Thao tác",
  key: "actions",
  fixed: "right",
  width: 230,
  render: (_, product) => (
    <Space size={4}>
      <Button
        type="text"
        icon={<EyeOutlined />}
        onClick={() => setSelectedProduct(product)}
        aria-label={`Xem sản phẩm ${product.name}`}
      >
        Chi tiết
      </Button>

      <Button
        type="text"
        danger={!product.hidden}
        disabled={loading || savingVisibility}
        icon={
          product.hidden
            ? <EyeOutlined />
            : <EyeInvisibleOutlined />
        }
        onClick={() => openVisibilityConfirm(product)}
        aria-label={`${
          product.hidden ? "Hiện" : "Ẩn"
        } sản phẩm ${product.name}`}
      >
        {product.hidden ? "Hiện lại" : "Ẩn"}
      </Button>
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
            Danh sách sản phẩm
          </Typography.Title>

          <Typography.Text type="secondary">
            Tra cứu sản phẩm và theo dõi trạng thái hiển thị.
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

      <Space
        wrap
        size={12}
        style={{ width: "100%", marginBottom: 20 }}
      >
        <Input.Search
          placeholder="Tìm sản phẩm hoặc người bán"
          aria-label="Tìm sản phẩm"
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
          style={{ width: "min(320px, 100%)" }}
        />

        <Select<string>
          placeholder="Tất cả danh mục"
          aria-label="Lọc danh mục"
          allowClear
          value={query.category}
          style={{ width: 180 }}
          options={categories.map((category) => ({
            value: category,
            label: category,
          }))}
          onChange={(category) => {
            setQuery((current) => ({
              ...current,
              category,
              page: 1,
            }));
          }}
        />

        <Select<Visibility>
          placeholder="Tất cả trạng thái"
          aria-label="Lọc trạng thái hiển thị"
          allowClear
          value={query.visibility}
          style={{ width: 180 }}
          options={[
            { value: "visible", label: "Đang hiển thị" },
            { value: "hidden", label: "Đang ẩn" },
          ]}
          onChange={(visibility) => {
            setQuery((current) => ({
              ...current,
              visibility,
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

      <Table<AdminProduct>
        rowKey="id"
        columns={columns}
        dataSource={products}
        loading={loading}
        scroll={{ x: 1120 }}
        locale={{
          emptyText: error
            ? "Không tải được danh sách sản phẩm"
            : "Không tìm thấy sản phẩm",
        }}
        pagination={{
          current: query.page,
          pageSize: query.size,
          disabled: savingVisibility,
          total,
          showSizeChanger: true,
          pageSizeOptions: [10, 20, 50],
          showTotal: (count) => `${count} sản phẩm`,
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
        title="Thông tin sản phẩm"
        open={selectedProduct !== null}
        onClose={() => setSelectedProduct(null)}
      >
        {selectedProduct && (
          <>
            {selectedProduct.imageUrls.length > 0 && (
              <div style={{ marginBottom: 24 }}>
                <Image.PreviewGroup>
                  <Space wrap size={10}>
                    {selectedProduct.imageUrls.map((url, index) => (
                      <Image
                        key={`${url}-${index}`}
                        src={url}
                        alt={`${selectedProduct.name} - ảnh ${index + 1}`}
                        width={92}
                        height={92}
                        style={{
                          objectFit: "cover",
                          borderRadius: 10,
                        }}
                      />
                    ))}
                  </Space>
                </Image.PreviewGroup>
              </div>
            )}

            <Descriptions column={1} layout="vertical" bordered>
              <Descriptions.Item label="Mã sản phẩm">
                {selectedProduct.id}
              </Descriptions.Item>

              <Descriptions.Item label="Tên sản phẩm">
                {selectedProduct.name}
              </Descriptions.Item>

              <Descriptions.Item label="Danh mục">
                {selectedProduct.category}
              </Descriptions.Item>

              <Descriptions.Item label="Người bán">
                {selectedProduct.farmerName}
              </Descriptions.Item>

              <Descriptions.Item label="Giá bán">
                {currencyFormatter.format(selectedProduct.price)}
                /{selectedProduct.unit}
              </Descriptions.Item>

              <Descriptions.Item label="Tồn kho">
                {quantityFormatter.format(selectedProduct.quantity)}{" "}
                {selectedProduct.unit}
              </Descriptions.Item>

              <Descriptions.Item label="Trạng thái">
                <VisibilityTag hidden={selectedProduct.hidden} />
              </Descriptions.Item>

              <Descriptions.Item label="Mô tả">
                <div
                  style={{
                    whiteSpace: "pre-wrap",
                    overflowWrap: "anywhere",
                  }}
                >
                  {selectedProduct.description || "Chưa có mô tả"}
                </div>
              </Descriptions.Item>
            </Descriptions>
          </>
        )}
      </Drawer>
      <Modal
  title={
    visibilityTarget?.hidden
      ? "Hiển thị lại sản phẩm"
      : "Ẩn sản phẩm"
  }
  open={visibilityTarget !== null}
  onCancel={closeVisibilityConfirm}
  onOk={() => void updateVisibility()}
  confirmLoading={savingVisibility}
  okText={
    visibilityTarget?.hidden
      ? "Xác nhận hiển thị"
      : "Xác nhận ẩn"
  }
  cancelText="Hủy"
  okButtonProps={{
    danger:
      visibilityTarget !== null && !visibilityTarget.hidden,
  }}
  cancelButtonProps={{ disabled: savingVisibility }}
  closable={!savingVisibility}
  maskClosable={!savingVisibility}
  keyboard={!savingVisibility}
>
  {visibilityTarget && (
    <>
      <Typography.Paragraph>
        Bạn muốn{" "}
        {visibilityTarget.hidden ? "hiển thị lại" : "ẩn"} sản phẩm{" "}
        <strong>{visibilityTarget.name}</strong> của{" "}
        <strong>{visibilityTarget.farmerName}</strong>?
      </Typography.Paragraph>

      <Alert
        showIcon
        type={visibilityTarget.hidden ? "info" : "warning"}
        title={
          visibilityTarget.hidden
            ? "Sản phẩm sẽ xuất hiện lại trong danh sách trên mobile."
            : "Sản phẩm sẽ bị ẩn khỏi danh sách trên mobile."
        }
        description="Thông tin sản phẩm và lịch sử đơn hàng được giữ lại."
      />

      {visibilityError && (
        <Alert
          type="error"
          showIcon
          title={visibilityError}
          style={{ marginTop: 16 }}
        />
      )}
    </>
  )}
</Modal>
    </section>
  );
}
