import axios from "axios";

export type AdminSession = {
  accessToken: string;
  refreshToken: string;
  fullName: string;
};

type LoginResponse = AdminSession & {
  role: string;
};

const STORAGE_KEY = "agritech-admin-session";

export const SESSION_EXPIRED_EVENT = "agritech-admin-session-expired";

export function readSession(): AdminSession | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);

    if (!raw) return null;

    const value = JSON.parse(raw);

    if (
      typeof value.accessToken !== "string" ||
      !value.accessToken ||
      typeof value.refreshToken !== "string" ||
      !value.refreshToken ||
      typeof value.fullName !== "string"
    ) {
      throw new Error("Phiên không hợp lệ");
    }

    return {
      accessToken: value.accessToken,
      refreshToken: value.refreshToken,
      fullName: value.fullName,
    };
  } catch {
    sessionStorage.removeItem(STORAGE_KEY);
    return null;
  }
}

export function clearSession() {
  sessionStorage.removeItem(STORAGE_KEY);
}

export const api = axios.create({
  baseURL: "/api",
  timeout: 15000,
});

api.interceptors.request.use((config) => {
  const session = readSession();

  if (session && !config.url?.startsWith("/auth/")) {
    config.headers.Authorization = `Bearer ${session.accessToken}`;
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      !error.config?.url?.startsWith("/auth/")
    ) {
      clearSession();
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    }

    return Promise.reject(error);
  },
);

export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    if (!error.response) {
      return "Không kết nối được backend. Kiểm tra server cổng 2507.";
    }

    const data = error.response.data;

    if (typeof data?.message === "string") return data.message;
    if (typeof data?.detail === "string") return data.detail;

    if (error.response.status === 401) {
      return "Phiên đăng nhập không hợp lệ hoặc đã hết hạn.";
    }

    if (error.response.status === 403) {
      return "Tài khoản không có quyền thực hiện thao tác này.";
    }

    return "Yêu cầu thất bại. Vui lòng thử lại.";
  }

  return error instanceof Error
    ? error.message
    : "Đã xảy ra lỗi. Vui lòng thử lại.";
}

export async function verifyAdmin(accessToken: string) {
  const { data } = await api.get<{ user_id: string; role: string }>(
    "/user/session",
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  );

  if (data.role !== "ADMIN") {
    throw new Error("Chỉ tài khoản quản trị viên được truy cập web này.");
  }
}

export async function loginAdmin(
  phoneNumber: string,
  password: string,
): Promise<AdminSession> {
  const { data } = await api.post<LoginResponse>("/auth/login", {
    phoneNumber: phoneNumber.trim(),
    password,
  });

  await verifyAdmin(data.accessToken);

  const session: AdminSession = {
    accessToken: data.accessToken,
    refreshToken: data.refreshToken,
    fullName: data.fullName,
  };

  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));

  return session;
}

export async function logoutAdmin() {
  const session = readSession();

  if (session) {
    await api.post("/auth/logout", {
      accessToken: session.accessToken,
      refreshToken: session.refreshToken,
    });
  }

  clearSession();
}