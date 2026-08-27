import { PencilSimple, Plus, Trash } from "@phosphor-icons/react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, Navigate } from "react-router-dom";
import { PageHeader } from "../../components/PageHeader";
import { EmptyState, ErrorState, LoadingState } from "../../components/States";
import { problemMessage } from "../../lib/api/client";
import { useAuthState } from "../../lib/auth/authStore";
import type { Category } from "../../types/api";
import { deleteCategory, getCategories } from "./categoryApi";

export default function CategoriesPage() {
  const auth = useAuthState();
  const queryClient = useQueryClient();
  const categories = useQuery({
    queryKey: ["categories"],
    queryFn: getCategories,
    enabled: auth.user?.role === "ADMIN",
  });
  const deactivate = useMutation({
    mutationFn: (category: Category) => deleteCategory(category.id),
    onSuccess: async (_result, category) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["categories"] }),
        queryClient.invalidateQueries({ queryKey: ["category", category.id] }),
      ]);
    },
  });

  if (auth.user?.role !== "ADMIN") return <Navigate to="/products" replace />;

  function handleDeactivate(category: Category) {
    const confirmed = window.confirm(
      `Deactivate “${category.name}”? It will no longer be available for new catalog assignments.`,
    );
    if (confirmed) deactivate.mutate(category);
  }

  return <div className="page-stack">
    <PageHeader eyebrow="Catalog administration" title="Categories"
      description="Create, edit, and deactivate the categories used to organize products."
      action={<Link className="button button-primary" to="/categories/new">
        <Plus size={18} aria-hidden="true" /> New category
      </Link>} />

    {deactivate.isError && <div className="form-alert" role="alert">{problemMessage(deactivate.error)}</div>}

    {categories.isLoading ? <LoadingState label="Loading categories" /> : categories.isError ?
      <ErrorState message={problemMessage(categories.error)} onRetry={() => void categories.refetch()} /> :
      categories.data!.length === 0 ? <EmptyState title="No categories yet"
        description="Create the first category to start organizing the product catalog."
        action={<Link className="button button-primary" to="/categories/new">Create category</Link>} /> :
      <section className="table-panel" aria-label="Categories">
        <div className="data-table category-table" role="table">
          <div className="table-row table-head" role="row">
            <span>Name</span><span>Description</span><span>Status</span><span>Actions</span>
          </div>
          {categories.data!.map((category) => {
            const isDeactivating = deactivate.isPending && deactivate.variables?.id === category.id;
            return <div className="table-row" role="row" key={category.id}>
              <span data-label="Name"><strong>{category.name}</strong></span>
              <span data-label="Description" className="category-description">
                {category.description || "No description provided."}
              </span>
              <span data-label="Status">
                <span className={`status ${category.active ? "status-active" : "status-inactive"}`}>
                  {category.active ? "Active" : "Inactive"}
                </span>
              </span>
              <span data-label="Actions">
                <span className="row-actions">
                  <Link className="button button-compact button-secondary" to={`/categories/${category.id}/edit`}>
                    <PencilSimple size={16} aria-hidden="true" /> Edit
                  </Link>
                  {category.active ? <button className="button button-compact button-danger" type="button"
                    disabled={deactivate.isPending} onClick={() => handleDeactivate(category)}>
                    <Trash size={16} aria-hidden="true" /> {isDeactivating ? "Deactivating…" : "Deactivate"}
                  </button> : <span className="inactive-note">Edit to reactivate</span>}
                </span>
              </span>
            </div>;
          })}
        </div>
      </section>}
  </div>;
}
