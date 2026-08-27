export type UserRole = "ADMIN" | "USER";

export interface User {
  id: string;
  email: string;
  role: UserRole;
}

export interface AuthResponse {
  accessToken: string;
  expiresIn: number;
  user: User;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface Category {
  id: string;
  name: string;
  description: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Product {
  id: string;
  name: string;
  description: string;
  sku: string;
  price: number;
  categoryId: string;
  categoryName: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface InventoryItem {
  productId: string;
  sku: string;
  productName: string;
  quantity: number;
  version: number;
  updatedAt: string;
}

export type InventoryMovementType = "MANUAL_INCREASE" | "MANUAL_DECREASE" | "ORDER_CONFIRMATION";

export interface InventoryMovement {
  id: string;
  productId: string;
  type: InventoryMovementType;
  quantityDelta: number;
  quantityBefore: number;
  quantityAfter: number;
  actorUserId: string | null;
  referenceId: string | null;
  reason: string | null;
  createdAt: string;
}

export type OrderStatus = "DRAFT" | "CONFIRMED" | "CANCELLED";

export interface OrderItem {
  id: string;
  productId: string;
  productName: string;
  sku: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Order {
  id: string;
  customerId: string;
  status: OrderStatus;
  totalAmount: number;
  itemCount: number;
  createdAt: string;
  confirmedAt: string | null;
  cancelledAt: string | null;
  items: OrderItem[];
}

export interface ApiProblem {
  type: string;
  title: string;
  status: number;
  detail: string;
  errors?: Record<string, string[]>;
  traceId?: string;
}
