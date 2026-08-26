import { api } from "../../lib/api/client";
import type { InventoryItem, PageResponse } from "../../types/api";

export async function listInventory(page = 0) {
  return (await api.get<PageResponse<InventoryItem>>("/inventory", { params: { page, size: 20 } })).data;
}

export async function adjustStock(productId: string, direction: "increase" | "decrease", quantity: number) {
  return (await api.post<{ productId: string; quantity: number; version: number }>(
    `/inventory/${productId}/${direction}`, { quantity },
  )).data;
}

