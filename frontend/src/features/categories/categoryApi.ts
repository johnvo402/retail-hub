import { api } from "../../lib/api/client";
import type { Category } from "../../types/api";

export interface CategoryInput {
  name: string;
  description: string;
  active: boolean;
}

export async function getCategories() {
  return (await api.get<Category[]>("/categories")).data;
}

export async function getCategory(id: string) {
  return (await api.get<Category>(`/categories/${id}`)).data;
}

export async function createCategory(input: CategoryInput) {
  return (await api.post<{ id: string }>("/categories", input)).data;
}

export async function updateCategory(id: string, input: CategoryInput) {
  await api.put(`/categories/${id}`, input);
}

export async function deleteCategory(id: string) {
  await api.delete(`/categories/${id}`);
}
