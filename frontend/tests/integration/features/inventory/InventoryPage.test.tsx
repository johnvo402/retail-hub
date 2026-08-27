import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import InventoryPage from "@/features/inventory/InventoryPage";
import { listInventory } from "@/features/inventory/inventoryApi";
import { authStore } from "@/lib/auth/authStore";
import type { UserRole } from "@/types/api";

vi.mock("@/features/inventory/inventoryApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/features/inventory/inventoryApi")>();
  return { ...actual, listInventory: vi.fn() };
});

const PRODUCT_ID = "11111111-1111-4111-8111-111111111111";
const listInventoryMock = vi.mocked(listInventory);

function renderPage(role: UserRole) {
  authStore.setAuthenticated("access-token", { id: "user-id", email: "user@example.com", role });
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  return render(<QueryClientProvider client={queryClient}>
    <MemoryRouter><InventoryPage /></MemoryRouter>
  </QueryClientProvider>);
}

describe("InventoryPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    listInventoryMock.mockResolvedValue({
      items: [{ productId: PRODUCT_ID, sku: "KEY-001", productName: "Atlas Keyboard",
        quantity: 12, version: 4, updatedAt: "2026-08-28T08:00:00Z" }],
      page: 0,
      size: 20,
      totalItems: 1,
      totalPages: 1,
    });
  });

  it("links product identity to inventory detail", async () => {
    renderPage("USER");

    expect(await screen.findByRole("link", { name: /Atlas Keyboard/ }))
      .toHaveAttribute("href", `/inventory/${PRODUCT_ID}`);
  });

  it("shows inline adjustment controls only to admins", async () => {
    const admin = renderPage("ADMIN");
    expect(await screen.findByRole("button", { name: "Add" })).toBeInTheDocument();
    admin.unmount();

    renderPage("USER");
    await screen.findByRole("link", { name: /Atlas Keyboard/ });
    expect(screen.queryByRole("button", { name: "Add" })).not.toBeInTheDocument();
    expect(screen.queryByText("Adjustment")).not.toBeInTheDocument();
  });
});
