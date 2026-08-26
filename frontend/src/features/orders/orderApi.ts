import { api } from "../../lib/api/client";
import type { Order, PageResponse } from "../../types/api";

export async function listOrders(page = 0) {
  return (await api.get<PageResponse<Order>>("/orders", { params: { page, size: 20 } })).data;
}

export async function getOrder(id: string) {
  return (await api.get<Order>(`/orders/${id}`)).data;
}

export async function createOrder() {
  return (await api.post<{ id: string }>("/orders")).data;
}

export async function addOrderItem(orderId: string, productId: string, quantity: number) {
  return (await api.post<{ id: string }>(`/orders/${orderId}/items`, { productId, quantity })).data;
}

export async function removeOrderItem(orderId: string, itemId: string) {
  await api.delete(`/orders/${orderId}/items/${itemId}`);
}

export async function confirmOrder(id: string) {
  await api.post(`/orders/${id}/confirm`);
}

export async function cancelOrder(id: string) {
  await api.post(`/orders/${id}/cancel`);
}

