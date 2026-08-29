import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import DashboardPage from "@/features/dashboard/DashboardPage";
import {
  getDashboardOverview,
  type DashboardOverview,
} from "@/features/dashboard/dashboardApi";

vi.mock("@/features/dashboard/dashboardApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/features/dashboard/dashboardApi")>();
  return { ...actual, getDashboardOverview: vi.fn() };
});

const getDashboardOverviewMock = vi.mocked(getDashboardOverview);
const overview: DashboardOverview = {
  activeProductCount: 125,
  inventoryLineCount: 120,
  lowStockCount: 47,
  draftOrderCount: 35,
  confirmedOrderCount: 52,
  confirmedOrderValue: 1_250_000,
  recentOrders: [{
    id: "12345678-1234-1234-1234-123456789abc",
    status: "CONFIRMED",
    totalAmount: 249.9,
    createdAt: "2026-08-29T00:00:00Z",
  }],
  lowStockItems: [{
    productId: "87654321-4321-4321-4321-cba987654321",
    productName: "Critical keyboard",
    sku: "KEY-LOW",
    quantity: 2,
  }],
};

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter><DashboardPage /></MemoryRouter>
    </QueryClientProvider>,
  );
}

function metric(label: string) {
  return within(screen.getByText(label).closest("article")!);
}

describe("DashboardPage", () => {
  beforeEach(() => vi.clearAllMocks());

  it("renders API aggregates, recent orders, and product-specific low-stock links", async () => {
    getDashboardOverviewMock.mockResolvedValue(overview);
    renderPage();

    await screen.findByText("Recent orders");
    expect(getDashboardOverviewMock).toHaveBeenCalledOnce();
    expect(metric("Active products").getByText("125")).toBeInTheDocument();
    expect(metric("Inventory lines").getByText("120")).toBeInTheDocument();
    expect(metric("Low stock").getByText("47")).toBeInTheDocument();
    expect(metric("Draft orders").getByText("35")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Order 12345678/ })).toHaveAttribute(
      "href", "/orders/12345678-1234-1234-1234-123456789abc",
    );
    expect(screen.getByText("CONFIRMED")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Critical keyboard/ })).toHaveAttribute(
      "href", "/inventory/87654321-4321-4321-4321-cba987654321",
    );
    expect(screen.getByText("2 left")).toBeInTheDocument();
  });

  it("shows the existing accessible loading state", () => {
    getDashboardOverviewMock.mockImplementation(() => new Promise(() => undefined));

    renderPage();

    expect(screen.getByRole("status")).toHaveTextContent("Building your overview");
  });

  it("shows an error and retries the single overview query", async () => {
    getDashboardOverviewMock.mockRejectedValueOnce(new Error("Unavailable"));
    getDashboardOverviewMock.mockResolvedValueOnce(overview);
    renderPage();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The operational overview could not be loaded.",
    );
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    await waitFor(() => expect(getDashboardOverviewMock).toHaveBeenCalledTimes(2));
    expect(await screen.findByText("125")).toBeInTheDocument();
  });
});
