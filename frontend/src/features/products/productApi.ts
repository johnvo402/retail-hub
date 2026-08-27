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

export async function listProducts(page = 0, keyword = "", category = "") {
  const { data } = await api.get<PageResponse<Product>>("/products", {
    params: { page, size: 12, active: true, keyword: keyword || undefined, category: category || undefined },
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
