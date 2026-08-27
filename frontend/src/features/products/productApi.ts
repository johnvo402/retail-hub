import { api } from "../../lib/api/client";
import type { PageResponse, Product } from "../../types/api";

export interface ProductInput {
  name: string;
  description: string;
  sku: string;
  price: number;
  categoryId: string;
  active: boolean;
}

export const PRODUCT_SORT_OPTIONS = [
  { value: "createdAt,desc", label: "Newest" },
  { value: "createdAt,asc", label: "Oldest" },
  { value: "price,asc", label: "Price: Low to High" },
  { value: "price,desc", label: "Price: High to Low" },
  { value: "name,asc", label: "Name: A-Z" },
  { value: "name,desc", label: "Name: Z-A" },
] as const;

export type ProductSort = typeof PRODUCT_SORT_OPTIONS[number]["value"];

export interface ProductFilters {
  page?: number;
  size?: number;
  category?: string;
  minPrice?: number;
  maxPrice?: number;
  active?: boolean;
  keyword?: string;
  sort?: ProductSort;
}

const productSortValues = new Set<string>(PRODUCT_SORT_OPTIONS.map((option) => option.value));

export function isProductSort(value: string | null | undefined): value is ProductSort {
  return typeof value === "string" && productSortValues.has(value);
}

function validPageValue(value: number, name: string, maximum?: number) {
  if (!Number.isInteger(value) || value < 0 || (maximum !== undefined && value > maximum)) {
    throw new RangeError(`${name} is outside the supported range`);
  }
}

function validPriceValue(value: number | undefined, name: string) {
  if (value !== undefined && (!Number.isFinite(value) || value < 0)) {
    throw new RangeError(`${name} must be a non-negative number`);
  }
}

export async function listProducts(filters: ProductFilters = {}) {
  const page = filters.page ?? 0;
  const size = filters.size ?? 12;
  validPageValue(page, "page");
  validPageValue(size, "size", 100);
  if (size === 0) throw new RangeError("size is outside the supported range");
  validPriceValue(filters.minPrice, "minPrice");
  validPriceValue(filters.maxPrice, "maxPrice");
  if (filters.minPrice !== undefined && filters.maxPrice !== undefined && filters.minPrice > filters.maxPrice) {
    throw new RangeError("minPrice cannot be greater than maxPrice");
  }
  if (filters.active !== undefined && typeof filters.active !== "boolean") {
    throw new TypeError("active must be a boolean");
  }
  if (filters.sort !== undefined && !isProductSort(filters.sort)) {
    throw new RangeError("sort is not supported");
  }

  const params: Record<string, string | number | boolean> = { page, size };
  const category = filters.category?.trim();
  const keyword = filters.keyword?.trim();
  if (category) params.category = category;
  if (filters.minPrice !== undefined) params.minPrice = filters.minPrice;
  if (filters.maxPrice !== undefined) params.maxPrice = filters.maxPrice;
  if (filters.active !== undefined) params.active = filters.active;
  if (keyword) params.keyword = keyword;
  if (filters.sort) params.sort = filters.sort;

  const { data } = await api.get<PageResponse<Product>>("/products", {
    params,
  });
  return data;
}

export async function searchProducts(query: string, page = 0) {
  const { data } = await api.get<PageResponse<Product>>("/products/search", { params: { q: query, page, size: 12 } });
  return data;
}

export async function getProduct(id: string) {
  return (await api.get<Product>(`/products/${id}`)).data;
}

export async function createProduct(input: ProductInput) {
  return (await api.post<{ id: string }>("/products", input)).data;
}

export async function updateProduct(id: string, input: ProductInput) {
  await api.put(`/products/${id}`, input);
}

export async function deleteProduct(id: string) {
  await api.delete(`/products/${id}`);
}
