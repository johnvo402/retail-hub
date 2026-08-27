import axios from "axios";
import { api } from "../../lib/api/client";
import { authStore } from "../../lib/auth/authStore";
import type { AuthResponse, User } from "../../types/api";

function clearUnauthorizedSession(error: unknown) {
  if (axios.isAxiosError(error) && error.response?.status === 401) authStore.clear();
}

export async function login(email: string, password: string) {
  const { data } = await api.post<AuthResponse>("/auth/login", { email, password });
  authStore.setAuthenticated(data.accessToken, data.user);
  return data;
}

export async function register(email: string, password: string) {
  const { data } = await api.post<User>("/auth/register", { email, password });
  return data;
}

export async function getCurrentUser() {
  try {
    const { data } = await api.get<User>("/auth/me");
    authStore.setUser(data);
    return data;
  } catch (error) {
    clearUnauthorizedSession(error);
    throw error;
  }
}

export async function logout() {
  try {
    await api.post("/auth/logout");
  } finally {
    authStore.clear();
  }
}

export async function logoutAll() {
  try {
    await api.post("/auth/logout-all");
    authStore.clear();
  } catch (error) {
    clearUnauthorizedSession(error);
    throw error;
  }
}
