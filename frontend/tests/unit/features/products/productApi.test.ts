import { afterAll, beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "@/lib/api/client";
import { listProducts, type ProductFilters } from "@/features/products/productApi";

const get = vi.spyOn(api, "get");

describe("productApi listProducts", () => {
  beforeEach(() => {
    get.mockReset();
    get.mockResolvedValue({ data: { items: [], page: 0, size: 12, totalItems: 0, totalPages: 0 } });
  });

  afterAll(() => get.mockRestore());

  it("sends all supported filters and trims text values", async () => {
    await listProducts({
      page: 2,
      category: "  category-id  ",
      minPrice: 10,
      maxPrice: 100.5,
      active: false,
      keyword: "  keyboard  ",
      sort: "price,asc",
    });

    expect(get).toHaveBeenCalledWith("/products", { params: {
      page: 2,
      size: 12,
      category: "category-id",
      minPrice: 10,
      maxPrice: 100.5,
      active: false,
      keyword: "keyboard",
      sort: "price,asc",
    } });
  });

  it("omits empty optional parameters", async () => {
    await listProducts({ category: "  ", keyword: "" });

    expect(get).toHaveBeenCalledWith("/products", { params: { page: 0, size: 12 } });
  });

  it("rejects malformed prices and unsupported sorts before Axios is called", async () => {
    await expect(listProducts({ minPrice: Number.NaN })).rejects.toThrow("minPrice must be a non-negative number");
    await expect(listProducts({ minPrice: 20, maxPrice: 10 })).rejects.toThrow("minPrice cannot be greater than maxPrice");
    await expect(listProducts({ sort: "unknown,asc" } as unknown as ProductFilters)).rejects.toThrow("sort is not supported");

    expect(get).not.toHaveBeenCalled();
  });
});
