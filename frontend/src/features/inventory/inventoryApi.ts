import { api } from "../../lib/api/client";
import type { InventoryItem, InventoryMovement, PageResponse } from "../../types/api";

export async function listInventory(page = 0) {
  return (await api.get<PageResponse<InventoryItem>>("/inventory", { params: { page, size: 20 } })).data;
}

export async function getInventory(productId: string) {
  return (await api.get<InventoryItem>(`/inventory/${productId}`)).data;
}

export async function listInventoryMovements(productId: string, page = 0) {
  return (await api.get<PageResponse<InventoryMovement>>(`/inventory/${productId}/movements`, {
    params: { page, size: 20 },
  })).data;
}

export async function adjustStock(productId: string, direction: "increase" | "decrease", quantity: number,
  reason?: string) {
  return (await api.post<{ productId: string; quantity: number; version: number }>(
    `/inventory/${productId}/${direction}`, { quantity, reason: reason?.trim() || undefined },
  )).data;
}
