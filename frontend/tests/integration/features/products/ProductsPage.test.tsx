import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { authStore } from "@/lib/auth/authStore";
import type { UserRole } from "@/types/api";
import ProductsPage from "@/features/products/ProductsPage";
import { listProducts, type ProductFilters } from "@/features/products/productApi";
import { getCategories } from "@/features/categories/categoryApi";

vi.mock("@/features/products/productApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/features/products/productApi")>();
  return { ...actual, listProducts: vi.fn() };
});

vi.mock("@/features/categories/categoryApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/features/categories/categoryApi")>();
  return { ...actual, getCategories: vi.fn() };
});

const CATEGORY_ID = "b5d0dca8-13b4-4ce7-aee5-762c50f84b79";
const listProductsMock = vi.mocked(listProducts);
const getCategoriesMock = vi.mocked(getCategories);

function renderPage(role: UserRole, entry = "/products") {
  authStore.setAuthenticated("access-token", { id: "user-id", email: "user@example.com", role });
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[entry]}>
        <Routes><Route path="/products" element={<ProductsPage />} /></Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

function latestFilters(): ProductFilters {
  const call = listProductsMock.mock.calls.at(-1);
  if (!call) throw new Error("Expected listProducts to have been called");
  return call[0] ?? {};
}

describe("ProductsPage filters", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    listProductsMock.mockResolvedValue({ items: [], page: 0, size: 12, totalItems: 0, totalPages: 0 });
    getCategoriesMock.mockResolvedValue([{
      id: CATEGORY_ID,
      name: "Accessories",
      description: "",
      active: true,
      createdAt: "2026-08-28T00:00:00Z",
      updatedAt: "2026-08-28T00:00:00Z",
    }]);
  });

  it("fetches active products only for a normal user", async () => {
    renderPage("USER");

    await waitFor(() => expect(latestFilters()).toMatchObject({ page: 0, size: 12, active: true }));
    expect(screen.queryByRole("combobox", { name: "Status" })).not.toBeInTheDocument();
  });

  it("lets an admin fetch all statuses without an active filter", async () => {
    renderPage("ADMIN");

    await waitFor(() => expect(listProductsMock).toHaveBeenCalled());
    expect(latestFilters().active).toBeUndefined();
    expect(screen.getByRole("combobox", { name: "Status" })).toHaveValue("");
  });

  it("lets an admin filter inactive products", async () => {
    renderPage("ADMIN");
    await waitFor(() => expect(listProductsMock).toHaveBeenCalled());

    fireEvent.change(screen.getByRole("combobox", { name: "Status" }), { target: { value: "false" } });

    await waitFor(() => expect(latestFilters().active).toBe(false));
  });

  it("preserves the category filter", async () => {
    renderPage("ADMIN", `/products?category=${CATEGORY_ID}`);

    await waitFor(() => expect(latestFilters().category).toBe(CATEGORY_ID));
  });

  it("applies minimum and maximum prices", async () => {
    renderPage("ADMIN");
    await waitFor(() => expect(listProductsMock).toHaveBeenCalled());
    fireEvent.change(screen.getByLabelText("Minimum price"), { target: { value: "10" } });
    fireEvent.change(screen.getByLabelText("Maximum price"), { target: { value: "100.50" } });

    fireEvent.click(screen.getByRole("button", { name: "Apply prices" }));

    await waitFor(() => expect(latestFilters()).toMatchObject({ minPrice: 10, maxPrice: 100.5 }));
  });

  it("sends a supported sort value", async () => {
    renderPage("ADMIN");
    await waitFor(() => expect(listProductsMock).toHaveBeenCalled());

    fireEvent.change(screen.getByRole("combobox", { name: "Sort by" }), { target: { value: "price,asc" } });

    await waitFor(() => expect(latestFilters().sort).toBe("price,asc"));
  });

  it("resets page to zero when a filter changes", async () => {
    renderPage("ADMIN", "/products?page=4");
    await waitFor(() => expect(latestFilters().page).toBe(4));
    await screen.findByRole("option", { name: "Accessories" });

    fireEvent.change(screen.getByRole("combobox", { name: "Category" }), { target: { value: CATEGORY_ID } });

    await waitFor(() => expect(latestFilters()).toMatchObject({ page: 0, category: CATEGORY_ID }));
  });

  it("restores role-appropriate defaults when filters are reset", async () => {
    renderPage("ADMIN", `/products?category=${CATEGORY_ID}&active=false&minPrice=10&maxPrice=100&sort=name,desc&page=2`);
    await waitFor(() => expect(latestFilters().page).toBe(2));

    fireEvent.click(screen.getByRole("button", { name: "Reset" }));

    await waitFor(() => expect(latestFilters()).toEqual({
      page: 0,
      size: 12,
      active: undefined,
      sort: "createdAt,desc",
    }));
    expect(screen.getByRole("combobox", { name: "Status" })).toHaveValue("");
    expect(screen.getByLabelText("Minimum price")).toHaveValue(null);
    expect(screen.getByLabelText("Maximum price")).toHaveValue(null);
  });

  it("keeps active-only enforced when a normal user resets filters", async () => {
    renderPage("USER", `/products?category=${CATEGORY_ID}&active=false&minPrice=10`);
    await waitFor(() => expect(listProductsMock).toHaveBeenCalled());

    fireEvent.click(screen.getByRole("button", { name: "Reset" }));

    await waitFor(() => expect(latestFilters()).toEqual({
      page: 0,
      size: 12,
      active: true,
      sort: "createdAt,desc",
    }));
  });

  it("normalizes malformed URL filters before querying", async () => {
    renderPage("ADMIN", "/products?category=not-a-uuid&active=maybe&minPrice=-1&sort=sku,asc&page=invalid");

    await waitFor(() => expect(listProductsMock).toHaveBeenCalled());
    expect(latestFilters()).toEqual({
      page: 0,
      size: 12,
      active: undefined,
      sort: "createdAt,desc",
    });
  });

  it("keeps every selected filter when a keyword search is entered", async () => {
    renderPage("ADMIN", `/products?category=${CATEGORY_ID}&active=false&minPrice=10&maxPrice=100&sort=price,desc&page=3`);
    await waitFor(() => expect(latestFilters().page).toBe(3));

    fireEvent.change(screen.getByRole("searchbox", { name: "Search products" }), {
      target: { value: "keyboard" },
    });

    await waitFor(() => expect(latestFilters()).toMatchObject({
      page: 0,
      category: CATEGORY_ID,
      active: false,
      minPrice: 10,
      maxPrice: 100,
      sort: "price,desc",
      keyword: "keyboard",
    }));
  });

  it("blocks an invalid price relationship before making another request", async () => {
    renderPage("ADMIN");
    await waitFor(() => expect(listProductsMock).toHaveBeenCalledTimes(1));
    fireEvent.change(screen.getByLabelText("Minimum price"), { target: { value: "100" } });
    fireEvent.change(screen.getByLabelText("Maximum price"), { target: { value: "10" } });

    fireEvent.click(screen.getByRole("button", { name: "Apply prices" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Minimum price cannot be greater than maximum price.");
    expect(listProductsMock).toHaveBeenCalledTimes(1);
  });
});
