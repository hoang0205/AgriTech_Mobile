import { useState } from "react";
import type { ReactNode } from "react";
import { Avatar, Button, Empty, Grid, Input, Menu } from "antd";
import {
  ArrowRightOutlined,
  BarChartOutlined,
  DashboardOutlined,
  FileSearchOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SearchOutlined,
  ShoppingCartOutlined,
  ShopOutlined,
  TeamOutlined,
  HistoryOutlined,
  UserOutlined,
} from "@ant-design/icons";

import FarmMark from "./components/FarmLogo";
import UsersPage from "./pages/UsersPage";
import ProductsPage from "./pages/ProductsPage";
import OrdersPage from "./pages/OrdersPage";
import ReportsPage from "./pages/ReportsPage";
import StatisticsPage from "./pages/StatisticsPage";
import AuditLogs from "./components/AuditLogs";

import DashboardMetrics from "./components/DashboardMetrics";

const sections = [
  {
    key: "users",
    title: "Người dùng",
    description: "Quản lý tài khoản, vai trò và quyền truy cập.",
    icon: <TeamOutlined />,
  },
  {
    key: "products",
    title: "Sản phẩm",
    description: "Theo dõi sản phẩm và kiểm duyệt nội dung đăng bán.",
    icon: <ShopOutlined />,
  },
  {
    key: "orders",
    title: "Đơn hàng",
    description: "Tra cứu đơn hàng và theo dõi trạng thái thanh toán.",
    icon: <ShoppingCartOutlined />,
  },
  {
    key: "reports",
    title: "Báo cáo vi phạm",
    description: "Tiếp nhận phản ánh và xử lý nội dung vi phạm.",
    icon: <FileSearchOutlined />,
  },
  {
    key: "statistics",
    title: "Thống kê",
    description: "Theo dõi kết quả kinh doanh theo từng khoảng thời gian.",
    icon: <BarChartOutlined />,
  },
  {
  key: "audit",
  title: "Nhật ký quản trị",
  description: "Theo dõi người thực hiện và lịch sử thay đổi dữ liệu.",
  icon: <HistoryOutlined />,
},
];

const navigation = [
  {
    key: "dashboard",
    icon: <DashboardOutlined />,
    label: "Tổng quan",
  },
  ...sections.map((section) => ({
    key: section.key,
    icon: section.icon,
    label: section.title,
  })),
];


function Panel({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: ReactNode;
}) {
  return (
    <section className="panel">
      <div className="panel-heading">
        <h2>{title}</h2>
        <p>{subtitle}</p>
      </div>
      {children}
    </section>
  );
}

type AppProps = {
  userName: string;
  onLogout: () => Promise<void>;
  loggingOut: boolean;
};

export default function App({
  userName,
  onLogout,
  loggingOut,
}: AppProps) {
  const [page, setPage] = useState("dashboard");
  const [collapsed, setCollapsed] = useState(false);
  const [search, setSearch] = useState("");
  const screens = Grid.useBreakpoint();
  const compact = collapsed || !screens.lg;

  const title = navigation.find((item) => item.key === page)?.label;
  const activeSection = sections.find((section) => section.key === page);

  const filteredSections = sections.filter((section) =>
    `${section.title} ${section.description}`
      .toLocaleLowerCase("vi")
      .includes(search.trim().toLocaleLowerCase("vi")),
  );

  return (
    <div className={`admin-shell ${compact ? "is-collapsed" : ""}`}>
      <aside className="sidebar">
        <a
          href="#"
          className="brand"
          aria-label="AgriTech — về tổng quan"
          onClick={(event) => {
            event.preventDefault();
            setPage("dashboard");
          }}
        >
          <span className="brand-mark">
            <FarmMark />
          </span>
          <span className="brand-copy">
            <strong>AgriTech<span>.</span></strong>
            <small>TRANG QUẢN TRỊ</small>
          </span>
        </a>

        <div className="sidebar-label">KHÔNG GIAN QUẢN TRỊ</div>

        <Menu
          mode="inline"
          inlineCollapsed={compact}
          selectedKeys={[page]}
          items={navigation}
          onClick={({ key }) => setPage(key)}
        />

        <div className="sidebar-bottom">
          <div className="sidebar-note">
            <span className="note-dot" />
            <span>Đồng hành cùng nông sản Việt</span>
          </div>
          <div className="sidebar-account">
            <Avatar
              icon={<UserOutlined />}
              style={{ background: "#EAF4E7", color: "#1B6E2A" }}
            />
            <div className="account-copy">
              <strong>{userName || "Quản trị viên"}</strong>
              <small>Quản trị viên</small>
            </div>
          </div>
        </div>
      </aside>

      <div className="workspace">
        <header className="topbar">
          <div className="topbar-left">
            <Button
              type="text"
              aria-label={collapsed ? "Mở rộng menu" : "Thu gọn menu"}
              icon={
                collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />
              }
              onClick={() => setCollapsed((current) => !current)}
            />
            <span className="breadcrumb">
              Quản trị <span>/</span> <strong>{title}</strong>
            </span>
          </div>

          <Button
  onClick={() => void onLogout()}
  loading={loggingOut}
>
  Đăng xuất
</Button>
        </header>

        <main className="main-content">
          <div className="page-heading">
            <div>
              <div className="eyebrow">AGRITECH ADMIN</div>
              <h1>{title}</h1>
              <p>
                {activeSection?.description ??
                  "Theo dõi và quản lý hoạt động của nền tảng."}
              </p>
            </div>
          </div>

          {page === "dashboard" ? (
            <>
              <section className="welcome-banner">
                <div className="welcome-copy">
                  <span className="welcome-label">
                    TỪ NÔNG TRẠI ĐẾN MỌI NHÀ
                  </span>
                  <h2>
                    Chăm chút từng sản phẩm.
                    <br />
                    Kết nối những giá trị xanh.
                  </h2>
                  <p>
                    Một nơi để quản lý người dùng, sản phẩm và hoạt động
                    kinh doanh của AgriTech.
                  </p>
                  <Button
                    className="welcome-button"
                    icon={<ArrowRightOutlined />}
                    onClick={() => setPage("products")}
                  >
                    Quản lý sản phẩm
                  </Button>
                </div>

                <div className="banner-art" aria-hidden="true">
                  <div className="art-orbit orbit-one" />
                  <div className="art-orbit orbit-two" />
                  <div className="farm-emblem">
                    <FarmMark />
                  </div>
                  <span className="art-caption">GROW TOGETHER</span>
                </div>
              </section>

<DashboardMetrics />

              <div className="dashboard-grid">
                <Panel
                  title="Quản lý nền tảng"
                  subtitle="Truy cập nhanh các chức năng quản trị"
                >
                  <Input
                    prefix={<SearchOutlined />}
                    placeholder="Tìm chức năng..."
                    value={search}
                    allowClear
                    onChange={(event) => setSearch(event.target.value)}
                    className="module-search"
                    aria-label="Tìm chức năng quản trị"
                  />

                  <div className="module-list">
                    {filteredSections.map((section) => (
                      <button
                        key={section.key}
                        className="module-link"
                        onClick={() => setPage(section.key)}
                      >
                        <span className="module-icon">{section.icon}</span>
                        <span className="module-copy">
                          <strong>{section.title}</strong>
                          <small>{section.description}</small>
                        </span>
                        <ArrowRightOutlined className="module-arrow" />
                      </button>
                    ))}

                    {filteredSections.length === 0 && (
                      <Empty
                        image={Empty.PRESENTED_IMAGE_SIMPLE}
                        description="Không tìm thấy chức năng phù hợp"
                      />
                    )}
                  </div>
                </Panel>

               <AuditLogs
  compact
  onViewAll={() => setPage("audit")}
/>
              </div>
                        </>
          ) : page === "users" ? (
            <UsersPage />
          ) : page === "products" ? (
            <ProductsPage />
          ) : page === "orders" ? (
            <OrdersPage />
          ) : page === "reports" ? (
            <ReportsPage />
          ) : page === "statistics" ? (
            <StatisticsPage />
          ) : page === "audit" ? (
            <AuditLogs />
          ) : (
            <Panel
              title={`Quản lý ${title?.toLowerCase()}`}
              subtitle="Không gian làm việc dành cho quản trị viên"
            >
              <div className="section-empty">
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description="Chức năng đang được triển khai"
                />
                <p>
                  Bảng dữ liệu và thao tác quản lý sẽ được bổ sung khi
                  kết nối API.
                </p>
                <Button onClick={() => setPage("dashboard")}>
                  Về tổng quan
                </Button>
              </div>
            </Panel>
          )}

          <footer className="page-footer">
            <span>AgriTech · Nền tảng kết nối nông sản</span>
            <span>Không gian quản trị</span>
          </footer>
        </main>
      </div>
    </div>
  );
}