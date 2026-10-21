import React from "react";
import ReactDOM from "react-dom/client";
import { ConfigProvider } from "antd";
import viVN from "antd/locale/vi_VN";
import "antd/dist/reset.css";
import "./index.css";
import App from "./App";
import AuthGate from "./AuthGate";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ConfigProvider
      locale={viVN}
      theme={{
        token: {
          colorPrimary: "#1B6E2A",
          colorSuccess: "#1B6E2A",
          colorText: "#333333",
          colorTextSecondary: "#788477",
          colorBgLayout: "#F6F8F6",
          colorBorder: "#E5EBE4",
          borderRadius: 12,
          controlHeight: 40,
          fontFamily: '"Segoe UI", Arial, sans-serif',
        },
        components: {
          Button: {
            primaryShadow: "none",
          },
          Menu: {
            itemBg: "transparent",
            itemSelectedBg: "#EAF4E7",
            itemSelectedColor: "#1B6E2A",
            itemHoverBg: "#F1F3F1",
            itemBorderRadius: 12,
            itemHeight: 48,
            iconSize: 19,
          },
        },
      }}
    >
      <AuthGate>
  {({ session, onLogout, loggingOut }) => (
    <App
      userName={session.fullName}
      onLogout={onLogout}
      loggingOut={loggingOut}
    />
  )}
</AuthGate>
    </ConfigProvider>
  </React.StrictMode>,
);