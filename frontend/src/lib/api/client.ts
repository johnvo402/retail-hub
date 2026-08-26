import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { authStore } from "../auth/authStore";
import type { ApiProblem, AuthResponse } from "../../types/api";

const baseURL = import.meta.env.VITE_API_URL ?? "/api";

export const api = axios.create({
  baseURL,
  withCredentials: true,
  timeout: 15_000,
});

const refreshClient = axios.create({
  baseURL,
  withCredentials: true,
  timeout: 15_000,
});

interface RetriableConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

let refreshPromise: Promise<string> | null = null;

export function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = refreshClient
      .post<AuthResponse>("/auth/refresh")
      .then(({ data }) => {
        authStore.setAuthenticated(data.accessToken, data.user);
        return data.accessToken;
      })
      .catch((error) => {
        authStore.clear();
        throw error;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

api.interceptors.request.use((config) => {
  const token = authStore.getSnapshot().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiProblem>) => {
    const config = error.config as RetriableConfig | undefined;
    const isAuthRequest = config?.url?.startsWith("/auth/");
    if (error.response?.status !== 401 || !config || config._retry || isAuthRequest) {
      return Promise.reject(error);
    }
    config._retry = true;
    try {
      const token = await refreshAccessToken();
      config.headers.Authorization = `Bearer ${token}`;
      return api(config);
    } catch {
      return Promise.reject(error);
    }
  },
);

export function problemMessage(error: unknown): string {
  if (axios.isAxiosError<ApiProblem>(error)) {
    return error.response?.data?.detail ?? (error.code === "ECONNABORTED"
      ? "The request timed out. Please try again."
      : "The request could not be completed.");
  }
  return error instanceof Error ? error.message : "Something went wrong.";
}

