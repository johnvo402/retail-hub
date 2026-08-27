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
});
