import { useEffect, useState } from "react";
import { Alert, Button, Drawer, Input, Select, Space, Table, Typography } from "antd";
import type { TableColumnsType } from "antd";
import axios from "axios";
import { api, getErrorMessage } from "../api";
import StatisticsPage, { FilterSearch } from "./StatisticsPage";

type UserRow = {
  userId: string; fullName: string; phoneNumber: string; email: string;
  soldOrders: number; revenue: number; profit: number;
  boughtOrders: number; spending: number; missingCostItems: number; missingShippingOrders: number;
};
type Query = { from: string; to: string; keyword: string; productId?: string; page: number; size: number; sort: string; direction: string };
type Response = { items: UserRow[]; totalElements: number; from: string; to: string };
const money = new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" });
function defaults(): Query {
  const to = new Date(new Date().toLocaleString("en-US", { timeZone: "Asia/Ho_Chi_Minh" }));
  const from = new Date(to); from.setDate(from.getDate() - 29);
  const date = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  return { from: date(from), to: date(to), keyword: "", page: 0, size: 10, sort: "revenue", direction: "desc" };
}
export default function UserStatisticsPage() {
  const [query, setQuery] = useState<Query>(defaults);
  const [draft, setDraft] = useState(query);
  const [data, setData] = useState<Response | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [inputError, setInputError] = useState("");
  const [selected, setSelected] = useState<UserRow | null>(null);
  const [side, setSide] = useState<"SELLER" | "BUYER">("SELLER");
  useEffect(() => {
    const controller = new AbortController();
    async function fetchRows() {
      setLoading(true); setError(""); setData(null);
      try {
        const result = await api.get<Response>("/admin/statistics/users", { params: query, signal: controller.signal });
        if (!controller.signal.aborted) setData(result.data);
      } catch (cause) {
        if (!controller.signal.aborted && !axios.isCancel(cause)) setError(getErrorMessage(cause));
      } finally { if (!controller.signal.aborted) setLoading(false); }
    }
    void fetchRows();
    return () => controller.abort();
  }, [query]);
  function apply() {
    const days = (Date.parse(draft.to) - Date.parse(draft.from)) / 86400000;
    if (!Number.isFinite(days) || days < 0 || days > 365 || draft.from < "2000-01-01" || draft.to > "2100-12-31") {
      setInputError("Chọn khoảng ngày hợp lệ trong năm 2000–2100, tối đa 366 ngày."); return;
    }
    setInputError(""); setQuery({ ...draft, keyword: draft.keyword.trim(), page: 0, sort: query.sort, direction: query.direction, size: query.size });
  }
  const columns: TableColumnsType<UserRow> = [
    { title: "Họ và tên", dataIndex: "fullName", width: 200, render: (name: string) => <Typography.Text strong>{name}</Typography.Text> },
    { title: "Email", dataIndex: "email", width: 250 },
    { title: "Số điện thoại", dataIndex: "phoneNumber", width: 150 },
    { title: "Đơn bán hoàn thành", dataIndex: "soldOrders", sorter: true, sortOrder: query.sort === "soldOrders" ? (query.direction === "asc" ? "ascend" : "descend") : null },
    { title: "Doanh thu bán", dataIndex: "revenue", sorter: true, sortOrder: query.sort === "revenue" ? (query.direction === "asc" ? "ascend" : "descend") : null, render: (v: number) => money.format(v) },
    { title: "Lợi nhuận ước tính", dataIndex: "profit", sorter: true, sortOrder: query.sort === "profit" ? (query.direction === "asc" ? "ascend" : "descend") : null,
      render: (v: number) => money.format(v) },
    { title: "Đơn mua hoàn thành", dataIndex: "boughtOrders", sorter: true, sortOrder: query.sort === "boughtOrders" ? (query.direction === "asc" ? "ascend" : "descend") : null },
    { title: "Chi tiêu mua hàng", dataIndex: "spending", sorter: true, sortOrder: query.sort === "spending" ? (query.direction === "asc" ? "ascend" : "descend") : null, render: (v: number) => money.format(v) },
    { title: "Thao tác", key: "actions", fixed: "right", width: 100, render: (_, row) => <Button onClick={() => { setSide("SELLER"); setSelected(row); }}>Chi tiết</Button> },
  ];
  return <section className="panel">
    <Typography.Title level={4} style={{ marginTop: 0 }}>Thống kê theo người dùng</Typography.Title>
    <Space wrap align="end" style={{ marginBottom: 20 }}>
      <div><label htmlFor="users-from">Từ ngày</label><Input id="users-from" type="date" min="2000-01-01" max="2100-12-31" value={draft.from} onChange={(e) => setDraft({ ...draft, from: e.target.value })} /></div>
      <div><label htmlFor="users-to">Đến ngày</label><Input id="users-to" type="date" min="2000-01-01" max="2100-12-31" value={draft.to} onChange={(e) => setDraft({ ...draft, to: e.target.value })} /></div>
      <Input style={{ width: 280 }} placeholder="Tìm tên, email, số điện thoại" aria-label="Tìm người dùng" allowClear value={draft.keyword} onChange={(e) => setDraft({ ...draft, keyword: e.target.value })} onPressEnter={apply} />
      <FilterSearch kind="products" value={draft.productId} onChange={(productId) => setDraft({ ...draft, productId })} />
      <Button type="primary" onClick={apply} loading={loading}>Lọc thống kê</Button>
      <Button onClick={() => { const value = defaults(); setDraft(value); setQuery(value); setInputError(""); }}>Đặt lại</Button>
    </Space>
    <Typography.Paragraph type="secondary">
      Chỉ tính đơn hoàn thành, theo ngày đặt đơn, bao gồm cả hai ngày đã chọn. Mỗi người có cả số liệu mua và bán.
      Doanh thu là tiền hàng; chi tiêu gồm tiền hàng và phí vận chuyển người mua trả.
      Lợi nhuận = doanh thu − giá vốn + phí giao thu của người mua − chi phí giao thực tế.
      Giá vốn và phí giao thực tế chưa nhập được tính bằng 0. Khi lọc mặt hàng, phí giao được phân bổ theo tỷ trọng tiền hàng trong đơn.
    </Typography.Paragraph>
    {(error || inputError) && <Alert type="error" showIcon title={inputError || error} style={{ marginBottom: 16 }} />}
    {data && <Typography.Paragraph type="secondary">{data.totalElements} người dùng · {data.from} đến {data.to}</Typography.Paragraph>}
    <Table<UserRow> rowKey="userId" columns={columns} dataSource={data?.items ?? []} loading={loading} scroll={{ x: 1650 }}
      locale={{ emptyText: error ? "Không tải được thống kê" : "Không tìm thấy người dùng" }}
      pagination={{ current: query.page + 1, pageSize: query.size, total: data?.totalElements ?? 0, showSizeChanger: true, pageSizeOptions: [10, 20, 50], showTotal: (total) => `${total} người dùng` }}
      onChange={(pagination, _, sorter) => {
        const chosen = Array.isArray(sorter) ? sorter[0] : sorter;
        setQuery((current) => ({ ...current, page: pagination.pageSize !== current.size ? 0 : (pagination.current ?? 1) - 1,
          size: pagination.pageSize ?? current.size, sort: chosen.order ? String(chosen.field) : "revenue", direction: chosen.order === "ascend" ? "asc" : "desc" }));
      }} />
    <Drawer title={selected ? `Chi tiết thống kê · ${selected.fullName}` : "Chi tiết thống kê"} open={selected !== null} onClose={() => setSelected(null)} size="large" destroyOnHidden>
      {selected && <>
        <Typography.Paragraph>Doanh thu bán: <strong>{money.format(selected.revenue)}</strong> · Chi tiêu mua hàng: <strong>{money.format(selected.spending)}</strong></Typography.Paragraph>
        <Typography.Paragraph>Lợi nhuận ước tính: <strong>{money.format(selected.profit)}</strong></Typography.Paragraph>
        <Space style={{ marginBottom: 16 }}><Typography.Text>Xem giao dịch:</Typography.Text><Select value={side} options={[{ value: "SELLER", label: "Bán hàng" }, { value: "BUYER", label: "Mua hàng" }]} onChange={setSide} /></Space>
        <Typography.Paragraph type="secondary">Biểu đồ bên dưới thể hiện tiền sản phẩm theo từng phía giao dịch; tổng chi tiêu và lợi nhuận sau vận chuyển xem ở bảng người dùng.</Typography.Paragraph>
        <StatisticsPage key={`${selected.userId}-${side}-${query.from}-${query.to}-${query.productId ?? ""}`} initialFilters={{ from: query.from, to: query.to, userId: selected.userId, userType: side, productId: query.productId }} />
      </>}
    </Drawer>
  </section>;
}
