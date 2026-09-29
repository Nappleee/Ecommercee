import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import type { ApiResponse, TokenResponse } from "@/types";

const API_BASE_URL = "";

function getStoredAuthTokens() {
  if (typeof window === "undefined") {
    return { accessToken: null, refreshToken: null };
  }

  try {
    const persisted = JSON.parse(localStorage.getItem("ecommerce-auth") ?? "{}");
    return {
      accessToken: localStorage.getItem("access_token") ?? persisted?.state?.token ?? null,
      refreshToken: localStorage.getItem("refresh_token") ?? persisted?.state?.refreshToken ?? null,
    };
  } catch {
    return {
      accessToken: localStorage.getItem("access_token"),
      refreshToken: localStorage.getItem("refresh_token"),
    };
  }
}

function persistAuthTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem("access_token", accessToken);
  localStorage.setItem("refresh_token", refreshToken);

  try {
    const persisted = JSON.parse(localStorage.getItem("ecommerce-auth") ?? "{}");
    if (persisted.state) {
      persisted.state.token = accessToken;
      persisted.state.refreshToken = refreshToken;
      localStorage.setItem("ecommerce-auth", JSON.stringify(persisted));
    }
  } catch {
    // Standalone token storage remains available if persisted state is malformed.
  }
}

function clearAuthTokens() {
  localStorage.removeItem("access_token");
  localStorage.removeItem("refresh_token");
  try {
    const persisted = JSON.parse(localStorage.getItem("ecommerce-auth") ?? "{}");
    if (persisted.state) {
      persisted.state.token = null;
      persisted.state.refreshToken = null;
      persisted.state.isAuthenticated = false;
      localStorage.setItem("ecommerce-auth", JSON.stringify(persisted));
    }
  } catch {
    // No persisted state needs clearing when the stored value is malformed.
  }
}

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { "Content-Type": "application/json" },
  timeout: 10000,
});

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (typeof window !== "undefined") {
    const { accessToken: token } = getStoredAuthTokens();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    const isAuthRequest = (originalRequest.url ?? "").includes("/api/v1/auth/");
    if (error.response?.status === 401 && !originalRequest._retry && !isAuthRequest) {
      originalRequest._retry = true;
      try {
        const { refreshToken } = getStoredAuthTokens();
        if (refreshToken) {
          const { data } = await axios.post<ApiResponse<TokenResponse>>(`${API_BASE_URL}/api/v1/auth/refresh`, {
            refreshToken,
          });
          const newToken = data.data?.accessToken ?? data.data?.access_token;
          const newRefreshToken = data.data?.refreshToken ?? data.data?.refresh_token;
          if (newToken && newRefreshToken) {
            persistAuthTokens(newToken, newRefreshToken);
          }
          if (newToken) {
            localStorage.setItem("access_token", newToken);
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            return apiClient(originalRequest);
          }
        }
      } catch {
        clearAuthTokens();
        if (window.location.pathname !== "/login") {
          window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname)}`;
        }
      }
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authApi = {
  login: (username: string, password: string) =>
    apiClient.post("/api/v1/auth/signin", { username, password }),
  register: (data: {
    fullName: string;
    userName: string;
    email: string;
    password: string;
    gender: string;
    phone?: string;
  }) => apiClient.post("/api/v1/auth/signup", data),
  logout: (refreshToken: string) =>
    apiClient.post("/api/v1/auth/logout", { refreshToken }),
  refreshToken: (refreshToken: string) =>
    apiClient.post("/api/v1/auth/refresh", { refreshToken }),
  getProfile: () => apiClient.get("/api/v1/users/me"),
};

// User management API
export const userApi = {
  getAll: (params?: { page?: number; size?: number; sortBy?: string; sortOrder?: string }) =>
    apiClient.get("/api/v1/users/all", { params }),
  delete: (id: number) => apiClient.delete(`/api/v1/users/${id}`),
};

// Product API
export const productApi = {
  getAll: (params?: { page?: number; size?: number; sort?: string; categoryId?: number }) =>
    apiClient.get("/api/products", { params }),
  getById: (id: number) => apiClient.get(`/api/products/${id}`),
  create: (data: unknown) => apiClient.post("/api/products", data),
  update: (id: number, data: unknown) => apiClient.put(`/api/products/${id}`, data),
  decrementQuantity: (id: number, amount: number) =>
    apiClient.patch(`/api/products/${id}/decrement`, undefined, { params: { amount } }),
  delete: (id: number) => apiClient.delete(`/api/products/${id}`),
};

// Category API
export const categoryApi = {
  getAll: (params?: { page?: number; size?: number }) =>
    apiClient.get("/api/categories", { params }),
  getById: (id: number) => apiClient.get(`/api/categories/${id}`),
  create: (data: unknown) => apiClient.post("/api/categories", data),
  update: (id: number, data: unknown) => apiClient.put(`/api/categories/${id}`, data),
  delete: (id: number) => apiClient.delete(`/api/categories/${id}`),
};

// Order API
export const orderApi = {
  getAll: (params?: { page?: number; size?: number }) =>
    apiClient.get("/api/orders", { params }),
  getAllPaged: (params?: { page?: number; size?: number; sortBy?: string; sortOrder?: string }) =>
    apiClient.get("/api/orders/all", { params }),
  getById: (id: number) => apiClient.get(`/api/orders/${id}`),
  create: (data: unknown) => apiClient.post("/api/orders", data),
  update: (id: number, data: unknown) => apiClient.put(`/api/orders/${id}`, data),
  delete: (id: number) => apiClient.delete(`/api/orders/${id}`),
  cancel: (id: number) => apiClient.patch(`/api/orders/${id}/cancel`),
};

// Cart API
export const cartApi = {
  getAll: (params?: { page?: number; size?: number }) =>
    apiClient.get("/api/carts", { params }),
  getById: (id: number) => apiClient.get(`/api/carts/${id}`),
  create: (data: unknown) => apiClient.post("/api/carts", data),
  update: (id: number, data: unknown) => apiClient.put(`/api/carts/${id}`, data),
  delete: (id: number) => apiClient.delete(`/api/carts/${id}`),
};

// Payment API
export const paymentApi = {
  getAll: (params?: { page?: number; size?: number }) =>
    apiClient.get("/api/payments", { params }),
  getAllPaged: (params?: { page?: number; size?: number; sortBy?: string; sortOrder?: string }) =>
    apiClient.get("/api/payments/all", { params }),
  getById: (id: number) => apiClient.get(`/api/payments/${id}`),
  create: (data: unknown) => apiClient.post("/api/payments", data),
};

// Favourite API
export const favouriteApi = {
  getAll: () => apiClient.get("/api/favourites"),
  add: (userId: number, productId: number) =>
    apiClient.post("/api/favourites", { userId, productId }),
  remove: (userId: number, productId: number) =>
    apiClient.delete(`/api/favourites/${userId}/${productId}`),
};

// Rating API
export const ratingApi = {
  getByProduct: (productId: number) =>
    apiClient.get(`/storefront/ratings`, { params: { productId } }),
  create: (data: unknown) => apiClient.post("/storefront/ratings", data),
};

// Inventory API
export const inventoryApi = {
  checkStock: (skuCode: string) =>
    apiClient.get(`/api/inventory`, { params: { skuCode } }),
};

// Search API
export const searchApi = {
  search: (keyword: string, params?: { page?: number; size?: number; sort?: string }) =>
    apiClient.get("/storefront/catalog-search", { params: { keyword, ...params } }),
  suggest: (keyword: string) =>
    apiClient.get("/storefront/search_suggest", { params: { keyword } }),
};
