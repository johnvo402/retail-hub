import { afterAll, beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "@/lib/api/client";
import { adjustStock, getInventory, listInventoryMovements } from "@/features/inventory/inventoryApi";

const get = vi.spyOn(api, "get");
const post = vi.spyOn(api, "post");

describe("inventoryApi", () => {
  beforeEach(() => {
    get.mockReset();
    post.mockReset();
    get.mockResolvedValue({ data: {} });
    post.mockResolvedValue({ data: {} });
  });

  afterAll(() => {
    get.mockRestore();
    post.mockRestore();
  });

  it("loads one inventory item", async () => {
    await getInventory("product-id");

    expect(get).toHaveBeenCalledWith("/inventory/product-id");
  });

  it("loads a movement page", async () => {
    await listInventoryMovements("product-id", 2);

    expect(get).toHaveBeenCalledWith("/inventory/product-id/movements", {
      params: { page: 2, size: 20 },
    });
  });

  it("trims an optional adjustment reason", async () => {
    await adjustStock("product-id", "increase", 5, "  Supplier delivery  ");

    expect(post).toHaveBeenCalledWith("/inventory/product-id/increase", {
      quantity: 5,
      reason: "Supplier delivery",
    });
  });
});
