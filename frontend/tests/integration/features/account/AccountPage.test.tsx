import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import AccountPage from "@/features/account/AccountPage";
import { getCurrentUser, logout, logoutAll } from "@/features/auth/authApi";
import { ProtectedRoute } from "@/app/router/ProtectedRoute";
import { authStore } from "@/lib/auth/authStore";
import type { UserRole } from "@/types/api";

vi.mock("@/features/auth/authApi", () => ({
  getCurrentUser: vi.fn(),
  logout: vi.fn(),
  logoutAll: vi.fn(),
}));

const getCurrentUserMock = vi.mocked(getCurrentUser);
const logoutMock = vi.mocked(logout);
const logoutAllMock = vi.mocked(logoutAll);

function renderAccount(role: UserRole) {
  const user = { id: "user-id", email: `${role.toLowerCase()}@example.com`, role };
  authStore.setAuthenticated("access-token", user);
  getCurrentUserMock.mockResolvedValue(user);
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/account"]}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/account" element={<AccountPage />} />
          </Route>
          <Route path="/login" element={<h1>Login page</h1>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("AccountPage", () => {
  beforeEach(() => vi.clearAllMocks());
  afterEach(() => vi.restoreAllMocks());

  it("displays the authenticated user's email and role", async () => {
    renderAccount("USER");

    expect(screen.getByText("user@example.com")).toBeInTheDocument();
    expect(screen.getByText("USER")).toBeInTheDocument();
    await waitFor(() => expect(getCurrentUserMock).toHaveBeenCalledTimes(1));
  });

  it("retrieves the latest account state from auth me", async () => {
    renderAccount("ADMIN");

    await waitFor(() => expect(getCurrentUserMock).toHaveBeenCalledTimes(1));
  });

  it("signs out the current session, clears auth state, and redirects with replacement", async () => {
    logoutMock.mockImplementation(async () => authStore.clear());
    renderAccount("USER");

    fireEvent.click(screen.getByRole("button", { name: "Sign out" }));

    await waitFor(() => expect(logoutMock).toHaveBeenCalledTimes(1));
    expect(await screen.findByRole("heading", { name: "Login page" })).toBeInTheDocument();
    expect(authStore.getSnapshot().status).toBe("unauthenticated");
  });

  it("requires confirmation before signing out all devices", async () => {
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
    logoutAllMock.mockImplementation(async () => authStore.clear());
    renderAccount("USER");

    fireEvent.click(screen.getByRole("button", { name: "Sign out all devices" }));
    expect(confirm).toHaveBeenCalledWith("Sign out of all devices? You will need to sign in again everywhere.");
    expect(logoutAllMock).not.toHaveBeenCalled();

    confirm.mockReturnValue(true);
    fireEvent.click(screen.getByRole("button", { name: "Sign out all devices" }));

    await waitFor(() => expect(logoutAllMock).toHaveBeenCalledTimes(1));
    expect(await screen.findByRole("heading", { name: "Login page" })).toBeInTheDocument();
    expect(authStore.getSnapshot().status).toBe("unauthenticated");
  });

  it("shows a logout-all failure without pretending the user signed out", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    logoutAllMock.mockRejectedValue(new Error("All sessions could not be revoked."));
    renderAccount("ADMIN");

    fireEvent.click(screen.getByRole("button", { name: "Sign out all devices" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("All sessions could not be revoked.");
    expect(screen.queryByRole("heading", { name: "Login page" })).not.toBeInTheDocument();
    expect(authStore.getSnapshot().status).toBe("authenticated");
  });

  it("redirects when logout-all failure confirms the current session is invalid", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    logoutAllMock.mockImplementation(async () => {
      authStore.clear();
      throw new Error("Session expired");
    });
    renderAccount("USER");

    fireEvent.click(screen.getByRole("button", { name: "Sign out all devices" }));

    expect(await screen.findByRole("heading", { name: "Login page" })).toBeInTheDocument();
    expect(authStore.getSnapshot().status).toBe("unauthenticated");
  });

  it("allows a normal user to access account security", () => {
    renderAccount("USER");

    expect(screen.getByRole("heading", { name: "Account & security" })).toBeInTheDocument();
  });

  it("allows an administrator to access account security", () => {
    renderAccount("ADMIN");

    expect(screen.getByRole("heading", { name: "Account & security" })).toBeInTheDocument();
  });
});
