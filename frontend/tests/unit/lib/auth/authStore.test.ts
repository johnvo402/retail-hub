import { beforeEach, describe, expect, it, vi } from "vitest";
import { authStore } from "@/lib/auth/authStore";

describe("in-memory auth store", () => {
  beforeEach(() => authStore.clear());

  it("keeps the access token in RAM without browser persistence", () => {
    const localStorageSpy = vi.spyOn(Storage.prototype, "setItem");
    authStore.setAuthenticated("access-token", { id: "user-id", email: "user@example.com", role: "USER" });

    expect(authStore.getSnapshot().accessToken).toBe("access-token");
    expect(localStorageSpy).not.toHaveBeenCalled();
  });

  it("clears all authentication state", () => {
    authStore.setAuthenticated("access-token", { id: "user-id", email: "user@example.com", role: "USER" });
    authStore.clear();

    expect(authStore.getSnapshot()).toEqual({ accessToken: null, user: null, status: "unauthenticated" });
  });

  it("updates server-side user details without replacing the in-memory token", () => {
    authStore.setAuthenticated("access-token", { id: "user-id", email: "old@example.com", role: "USER" });

    authStore.setUser({ id: "user-id", email: "latest@example.com", role: "ADMIN" });

    expect(authStore.getSnapshot()).toEqual({
      accessToken: "access-token",
      user: { id: "user-id", email: "latest@example.com", role: "ADMIN" },
      status: "authenticated",
    });
  });

  it("does not restore a user from a late response after local auth was cleared", () => {
    authStore.clear();

    authStore.setUser({ id: "user-id", email: "late@example.com", role: "USER" });

    expect(authStore.getSnapshot()).toEqual({ accessToken: null, user: null, status: "unauthenticated" });
  });
});
