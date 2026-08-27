import { afterAll, beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "@/lib/api/client";
import { getCurrentUser, logout, logoutAll } from "@/features/auth/authApi";
import { authStore } from "@/lib/auth/authStore";

const get = vi.spyOn(api, "get");
const post = vi.spyOn(api, "post");
const originalUser = { id: "user-id", email: "old@example.com", role: "USER" as const };

describe("authApi account endpoints", () => {
  beforeEach(() => {
    get.mockReset();
    post.mockReset();
    authStore.setAuthenticated("access-token", originalUser);
  });

  afterAll(() => {
    get.mockRestore();
    post.mockRestore();
  });

  it("loads the current user and syncs it without replacing the access token", async () => {
    const currentUser = { ...originalUser, email: "latest@example.com", role: "ADMIN" as const };
    get.mockResolvedValue({ data: currentUser });

    await expect(getCurrentUser()).resolves.toEqual(currentUser);

    expect(get).toHaveBeenCalledWith("/auth/me");
    expect(authStore.getSnapshot()).toEqual({
      accessToken: "access-token",
      user: currentUser,
      status: "authenticated",
    });
  });

  it("clears stale local auth state when current-user lookup is unauthorized", async () => {
    const error = { isAxiosError: true, response: { status: 401 } };
    get.mockRejectedValue(error);

    await expect(getCurrentUser()).rejects.toBe(error);

    expect(authStore.getSnapshot().status).toBe("unauthenticated");
  });

  it("logs out the current refresh session and always clears local auth state", async () => {
    post.mockResolvedValue({ data: undefined });

    await logout();

    expect(post).toHaveBeenCalledWith("/auth/logout");
    expect(authStore.getSnapshot().status).toBe("unauthenticated");
  });

  it("clears local auth state even when current-session logout fails", async () => {
    post.mockRejectedValue(new Error("Network unavailable"));

    await expect(logout()).rejects.toThrow("Network unavailable");

    expect(authStore.getSnapshot().status).toBe("unauthenticated");
  });

  it("revokes all sessions and clears local auth state on success", async () => {
    post.mockResolvedValue({ data: undefined });

    await logoutAll();

    expect(post).toHaveBeenCalledWith("/auth/logout-all");
    expect(authStore.getSnapshot().status).toBe("unauthenticated");
  });

  it("keeps local state when logout-all fails without invalidating the session", async () => {
    const error = { isAxiosError: true, response: { status: 500 } };
    post.mockRejectedValue(error);

    await expect(logoutAll()).rejects.toBe(error);

    expect(authStore.getSnapshot().status).toBe("authenticated");
  });

  it("clears local state when logout-all confirms the access session is unauthorized", async () => {
    const error = { isAxiosError: true, response: { status: 401 } };
    post.mockRejectedValue(error);

    await expect(logoutAll()).rejects.toBe(error);

    expect(authStore.getSnapshot().status).toBe("unauthenticated");
  });
});
