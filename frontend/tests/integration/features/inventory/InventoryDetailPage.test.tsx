import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import InventoryDetailPage from "@/features/inventory/InventoryDetailPage";
import { StockEditor } from "@/features/inventory/StockEditor";
import { adjustStock, getInventory, listInventoryMovements } from "@/features/inventory/inventoryApi";
import { authStore } from "@/lib/auth/authStore";
import type { InventoryItem, InventoryMovement, PageResponse, UserRole } from "@/types/api";

vi.mock("@/features/inventory/inventoryApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/features/inventory/inventoryApi")>();
  return {
    ...actual,
    getInventory: vi.fn(),
    listInventoryMovements: vi.fn(),
    adjustStock: vi.fn(),
  };
});

const PRODUCT_ID = "11111111-1111-4111-8111-111111111111";
const ORDER_ID = "22222222-2222-4222-8222-222222222222";
const ACTOR_ID = "33333333-3333-4333-8333-333333333333";
const inventoryItem: InventoryItem = {
  productId: PRODUCT_ID,
  sku: "KEY-001",
  productName: "Atlas Keyboard",
  quantity: 12,
  version: 4,
  updatedAt: "2026-08-28T08:00:00Z",
};
const movementItems: InventoryMovement[] = [
  {
    id: "44444444-4444-4444-8444-444444444444",
    productId: PRODUCT_ID,
    type: "ORDER_CONFIRMATION",
    quantityDelta: -3,
    quantityBefore: 15,
    quantityAfter: 12,
    actorUserId: ACTOR_ID,
    referenceId: ORDER_ID,
    reason: null,
    createdAt: "2026-08-28T08:00:00Z",
  },
  {
    id: "55555555-5555-4555-8555-555555555555",
    productId: PRODUCT_ID,
    type: "MANUAL_INCREASE",
    quantityDelta: 10,
    quantityBefore: 5,
    quantityAfter: 15,
    actorUserId: ACTOR_ID,
    referenceId: null,
    reason: "Supplier delivery",
    createdAt: "2026-08-27T08:00:00Z",
  },
];

const getInventoryMock = vi.mocked(getInventory);
const listMovementsMock = vi.mocked(listInventoryMovements);
const adjustStockMock = vi.mocked(adjustStock);

function movementPage(page = 0, totalPages = 1, items = movementItems): PageResponse<InventoryMovement> {
  return { items, page, size: 20, totalItems: totalPages * 20, totalPages };
}

function renderDetail(role: UserRole = "ADMIN") {
  authStore.setAuthenticated("access-token", { id: ACTOR_ID, email: "user@example.com", role });
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  return render(<QueryClientProvider client={queryClient}>
    <MemoryRouter initialEntries={[`/inventory/${PRODUCT_ID}`]}>
      <Routes>
        <Route path="/inventory/:productId" element={<InventoryDetailPage />} />
        <Route path="/orders/:id" element={<h1>Order detail</h1>} />
      </Routes>
    </MemoryRouter>
  </QueryClientProvider>);
}

describe("InventoryDetailPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getInventoryMock.mockResolvedValue(inventoryItem);
    listMovementsMock.mockResolvedValue(movementPage());
    adjustStockMock.mockResolvedValue({ productId: PRODUCT_ID, quantity: 13, version: 5 });
  });

  it("loads and renders the current inventory state", async () => {
    renderDetail();

    expect(await screen.findByRole("heading", { name: "Atlas Keyboard" })).toBeInTheDocument();
    expect(screen.getByText("KEY-001")).toBeInTheDocument();
    expect(screen.getByText("v4")).toBeInTheDocument();
    expect(getInventoryMock).toHaveBeenCalledWith(PRODUCT_ID);
  });

  it("renders movement history in the newest-first API order", async () => {
    renderDetail();

    const rows = await screen.findAllByTestId("inventory-movement");
    expect(rows[0]).toHaveTextContent("Order confirmation");
    expect(rows[1]).toHaveTextContent("Manual increase");
  });

  it("formats positive and negative deltas with their signs", async () => {
    renderDetail();

    expect(await screen.findByText("+10")).toBeInTheDocument();
    expect(screen.getByText("-3")).toBeInTheDocument();
  });

  it("links order-confirmation movements to the order detail", async () => {
    renderDetail();

    expect(await screen.findByRole("link", { name: "Order 22222222" }))
      .toHaveAttribute("href", `/orders/${ORDER_ID}`);
  });

  it("shows stock adjustment controls to an admin", async () => {
    renderDetail("ADMIN");

    expect(await screen.findByRole("button", { name: "Add" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Remove" })).toBeInTheDocument();
  });

  it("does not show stock adjustment controls to a normal user", async () => {
    renderDetail("USER");

    await screen.findByRole("heading", { name: "Atlas Keyboard" });
    expect(screen.queryByRole("button", { name: "Add" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Remove" })).not.toBeInTheDocument();
  });

  it("loads the next movement page", async () => {
    listMovementsMock.mockImplementation(async (_productId, page = 0) =>
      page === 0 ? movementPage(0, 2) : movementPage(1, 2, [movementItems[1]]));
    renderDetail();

    fireEvent.click(await screen.findByRole("button", { name: "Next page" }));

    await waitFor(() => expect(listMovementsMock).toHaveBeenCalledWith(PRODUCT_ID, 1));
    await waitFor(() => expect(screen.getByRole("navigation", { name: "Pagination" }))
      .toHaveTextContent("Page 2 of 2"));
  });

  it("invalidates inventory list, detail, and movement queries after adjustment", async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const listKey = ["inventory", { page: 0 }] as const;
    const detailKey = ["inventory", PRODUCT_ID] as const;
    const movementKey = ["inventory-movements", PRODUCT_ID, { page: 0 }] as const;
    queryClient.setQueryData(listKey, movementPage());
    queryClient.setQueryData(detailKey, inventoryItem);
    queryClient.setQueryData(movementKey, movementPage());
    render(<QueryClientProvider client={queryClient}><StockEditor item={inventoryItem} /></QueryClientProvider>);

    fireEvent.click(screen.getByRole("button", { name: "Add" }));
    fireEvent.change(screen.getByLabelText("Reason (optional)"), { target: { value: "Supplier delivery" } });
    fireEvent.click(screen.getByRole("button", { name: "Add stock" }));

    await waitFor(() => expect(adjustStockMock).toHaveBeenCalledWith(
      PRODUCT_ID, "increase", 1, "Supplier delivery"));
    await waitFor(() => {
      expect(queryClient.getQueryState(listKey)?.isInvalidated).toBe(true);
      expect(queryClient.getQueryState(detailKey)?.isInvalidated).toBe(true);
      expect(queryClient.getQueryState(movementKey)?.isInvalidated).toBe(true);
    });
  });
});
