import { api } from "../../lib/api/client";
import type { OrderStatus } from "../../types/api";

export interface DashboardOrderSummary {
  id: string;
  status: OrderStatus;
  totalAmount: number;
  createdAt: string;
}

export interface DashboardLowStockItem {
  productId: string;
  productName: string;
  sku: string;
  quantity: number;
}

export interface DashboardOverview {
  activeProductCount: number;
  inventoryLineCount: number;
  lowStockCount: number;
  draftOrderCount: number;
  confirmedOrderCount: number;
  confirmedOrderValue: number;
  recentOrders: DashboardOrderSummary[];
  lowStockItems: DashboardLowStockItem[];
}

export async function getDashboardOverview() {
  return (await api.get<DashboardOverview>("/dashboard/overview")).data;
}
