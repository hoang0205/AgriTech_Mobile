import { useEffect, useState } from "react";
import { Alert, Button, Descriptions, Drawer, Image, Input, Modal, Select, Space, Table, Tag, Timeline, Typography, message } from "antd";
import type { TableColumnsType } from "antd";
import { ReloadOutlined } from "@ant-design/icons";
import { api, getErrorMessage } from "../api";

type Status = "OPEN" | "REVIEWING" | "RESOLVED" | "REJECTED";
type Outcome = "KEEP_ORDER" | "COMPLETE_ORDER" | "CANCEL_ORDER";
type Complaint = {
  id: number; orderId: number; reporterName: string; reason: string; description: string;
  imageUrls: string[]; status: Status; adminReply: string | null; createdAt: string; updatedAt: string;
  orderStatus: string; sellerDeliveredAt: string | null; buyerReceivedAt: string | null;
  outcome: Outcome | null; totalAmount: number; receiverName: string; shippingAddress: string; products: string[];
  events: { status: string; message: string; occurredAt: string }[];
};
const labels: Record<Status, string> = { OPEN: "Chờ xử lý", REVIEWING: "Đang xem xét", RESOLVED: "Đã giải quyết", REJECTED: "Đã từ chối" };
const colors: Record<Status, string> = { OPEN: "gold", REVIEWING: "blue", RESOLVED: "green", REJECTED: "red" };
const reasons: Record<string, string> = { NOT_RECEIVED: "Chưa nhận được hàng", DAMAGED: "Hàng hỏng / chất lượng không đúng", MISSING_OR_WRONG: "Thiếu hàng / giao sai", OTHER: "Vấn đề khác" };
const outcomes: Record<Outcome, string> = { KEEP_ORDER: "Giữ nguyên đơn", COMPLETE_ORDER: "Hoàn thành đơn", CANCEL_ORDER: "Đóng / hủy đơn theo kết quả khiếu nại" };
const date = (value: string | null) => value ? new Date(value).toLocaleString("vi-VN") : "Chưa xác nhận";
const closed = (value: Status) => value === "RESOLVED" || value === "REJECTED";

export default function OrderComplaintsPage() {
  const [rows, setRows] = useState<Complaint[]>([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState<{ page: number; size: number; status?: Status }>({ page: 1, size: 10 });
  const [revision, setRevision] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string>();
  const [selected, setSelected] = useState<Complaint>();
  const [status, setStatus] = useState<Status>("REVIEWING");
  const [outcome, setOutcome] = useState<Outcome>("KEEP_ORDER");
  const [reply, setReply] = useState("");
  const [confirm, setConfirm] = useState(false);
  const [notice, contextHolder] = message.useMessage();

  useEffect(() => {
    const controller = new AbortController();
    api.get<{ content: Complaint[]; totalElements: number }>("/admin/order-complaints", {
      params: { ...query, page: query.page - 1 }, signal: controller.signal,
    }).then(({ data }) => { setRows(data.content); setTotal(data.totalElements); })
      .catch((cause: unknown) => { if (!controller.signal.aborted) setError(getErrorMessage(cause)); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [query, revision]);

  function reload() {
    setLoading(true); setError(undefined); setRevision(value => value + 1);
  }

  function inspect(row: Complaint) {
    setSelected(row); setStatus(closed(row.status) ? row.status : "REVIEWING");
    setOutcome("KEEP_ORDER"); setReply(""); setError(undefined);
  }

  async function save() {
    if (!selected || saving || closed(selected.status)) return;
    setSaving(true); setError(undefined);
    try {
      const { data } = await api.patch<Complaint>(`/admin/order-complaints/${selected.id}`, {
        status, reply: reply.trim(), outcome: status === "RESOLVED" ? outcome : "KEEP_ORDER",
      });
      setSelected(data); setConfirm(false); setReply(""); reload();
      void notice.success("Đã lưu kết quả và thông báo cho hai bên.");
    } catch (cause: unknown) { setError(getErrorMessage(cause)); }
    finally { setSaving(false); }
  }

  const columns: TableColumnsType<Complaint> = [
    { title: "Khiếu nại", dataIndex: "id", width: 110, render: value => `#${value}` },
    { title: "Đơn hàng", dataIndex: "orderId", width: 130, render: value => `#ORD-${value}` },
    { title: "Người mua", dataIndex: "reporterName" },
    { title: "Lý do", dataIndex: "reason", render: value => reasons[value] ?? value },
    { title: "Trạng thái", dataIndex: "status", width: 150, render: (value: Status) => <Tag color={colors[value]}>{labels[value]}</Tag> },
    { title: "Ngày gửi", dataIndex: "createdAt", render: value => date(value) },
    { title: "", width: 100, render: (_, row) => <Button type="link" onClick={() => inspect(row)}>Xử lý</Button> },
  ];

  return <section className="panel">
    {contextHolder}
    <div className="panel-heading"><h2>Khiếu nại đơn hàng</h2><p>Xem bằng chứng, phản hồi và xử lý các vấn đề giao nhận.</p></div>
    <Space wrap style={{ marginBottom: 16 }}>
      <Select allowClear placeholder="Tất cả trạng thái" style={{ width: 190 }} value={query.status}
        options={Object.entries(labels).map(([value, label]) => ({ value, label }))}
        onChange={(value: Status | undefined) => { setLoading(true); setError(undefined); setQuery(current => ({ ...current, page: 1, status: value })); }} />
      <Button icon={<ReloadOutlined />} onClick={reload} loading={loading}>Tải lại</Button>
    </Space>
    {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 16 }} />}
    <Table<Complaint> rowKey="id" columns={columns} dataSource={rows} loading={loading} scroll={{ x: 1000 }}
      locale={{ emptyText: "Chưa có khiếu nại phù hợp" }}
      pagination={{ current: query.page, pageSize: query.size, total, showSizeChanger: true,
        onChange: (page, size) => { setLoading(true); setError(undefined); setQuery(current => ({ ...current, page, size })); } }} />
    <Drawer title={selected ? `Khiếu nại #${selected.id} · Đơn #ORD-${selected.orderId}` : "Khiếu nại"}
      open={!!selected} onClose={() => { if (!saving) { setSelected(undefined); setError(undefined); } }} size={620}>
      {selected && <Space orientation="vertical" size="large" style={{ width: "100%" }}>
        {error && <Alert type="error" showIcon title={error} />}
        <Tag color={colors[selected.status]}>{labels[selected.status]}</Tag>
        <Descriptions column={1} bordered size="small" items={[
          { key: "buyer", label: "Người mua", children: selected.reporterName },
          { key: "reason", label: "Lý do", children: reasons[selected.reason] ?? selected.reason },
          { key: "state", label: "Trạng thái đơn", children: selected.orderStatus },
          { key: "products", label: "Sản phẩm", children: selected.products.join("; ") },
          { key: "total", label: "Tổng đơn", children: selected.totalAmount.toLocaleString("vi-VN") + " đ" },
          { key: "receiver", label: "Người nhận", children: selected.receiverName },
          { key: "address", label: "Địa chỉ giao", children: selected.shippingAddress },
          { key: "seller", label: "Người bán đã giao", children: date(selected.sellerDeliveredAt) },
          { key: "received", label: "Người mua đã nhận", children: date(selected.buyerReceivedAt) },
          { key: "date", label: "Ngày gửi", children: date(selected.createdAt) },
          { key: "outcome", label: "Kết quả", children: selected.outcome ? outcomes[selected.outcome] : "Chưa có quyết định" },
        ]} />
        <div><Typography.Title level={5}>Nội dung khiếu nại</Typography.Title><Typography.Paragraph style={{ whiteSpace: "pre-wrap" }}>{selected.description}</Typography.Paragraph>
          <Image.PreviewGroup><Space wrap>{selected.imageUrls.map(url => <Image key={url} src={url} width={110} height={110} style={{ objectFit: "cover" }} />)}</Space></Image.PreviewGroup></div>
        <div><Typography.Title level={5}>Lịch sử xử lý</Typography.Title>
          <Timeline items={selected.events.map((event, index) => ({ key: index, content: <><strong>{labels[event.status as Status] ?? event.status}</strong> · {date(event.occurredAt)}<p style={{ whiteSpace: "pre-wrap" }}>{event.message}</p></> }))} /></div>
        {!closed(selected.status) && <>
          <Alert type="info" showIcon title="Đơn đang tạm dừng hoàn thành. Khi đóng khiếu nại và giữ nguyên đơn, người mua có thêm 48 giờ phản hồi sau xác nhận giao hàng." />
          <Select style={{ width: "100%" }} value={status} onChange={(value: Status) => { setStatus(value); setOutcome("KEEP_ORDER"); }}
            options={["REVIEWING", "RESOLVED", "REJECTED"].map(value => ({ value, label: labels[value as Status] }))} />
          {status === "RESOLVED" && <Select style={{ width: "100%" }} value={outcome} onChange={setOutcome} options={[
            { value: "KEEP_ORDER", label: "Giữ nguyên trạng thái đơn" },
            { value: "COMPLETE_ORDER", label: "Xác nhận hoàn thành đơn sau đối chiếu" },
            { value: "CANCEL_ORDER", label: "Đóng / hủy đơn theo kết quả khiếu nại" },
          ]} />}
          <Input.TextArea rows={4} value={reply} onChange={event => setReply(event.target.value)} maxLength={2000} showCount placeholder="Phản hồi và kết quả xử lý gửi cho hai bên (tối thiểu 5 ký tự)" />
          <Typography.Text type="secondary">Kết quả xử lý này không tự chuyển hoặc hoàn tiền cho người mua.</Typography.Text>
          {status === "RESOLVED" && outcome === "CANCEL_ORDER" && <Alert type="warning" showIcon title="Hủy đơn không tự cộng tồn kho: chỉ nhập lại hàng sau khi đã nhận lại và kiểm tra chất lượng. Các khoản hoàn tiền phải được xử lý, đối chiếu riêng." />}
          <Button type="primary" loading={saving} disabled={reply.trim().length < 5} onClick={() => setConfirm(true)}>Lưu kết quả xử lý</Button>
        </>}
      </Space>}
    </Drawer>
    <Modal title="Xác nhận lưu kết quả xử lý" open={confirm} onCancel={() => { if (!saving) setConfirm(false); }}
      onOk={() => void save()} confirmLoading={saving} okText="Xác nhận" cancelText="Quay lại">
      <p>{status === "RESOLVED" ? `Quyết định: ${outcomes[outcome]}.` : `Khiếu nại sẽ chuyển sang: ${labels[status]}.`}</p>
      <p>Phản hồi sẽ được lưu vào lịch sử và thông báo cho hai bên.</p>
    </Modal>
  </section>;
}
