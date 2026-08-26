import { api } from "../../lib/api/client";
import { authStore } from "../../lib/auth/authStore";
import type { AuthResponse, User } from "../../types/api";

export async function login(email: string, password: string) {
  const { data } = await api.post<AuthResponse>("/auth/login", { email, password });
  authStore.setAuthenticated(data.accessToken, data.user);
  return data;
}

export async function register(email: string, password: string) {
  const { data } = await api.post<User>("/auth/register", { email, password });
  return data;
}

export async function logout() {
  try {
    await api.post("/auth/logout");
  } finally {
    authStore.clear();
  }
}

