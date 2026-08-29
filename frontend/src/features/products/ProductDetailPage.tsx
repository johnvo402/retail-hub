import { ArrowLeft, PencilSimple, Trash } from "@phosphor-icons/react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ErrorState, LoadingState } from "../../components/States";
import { formatCurrency, formatDate } from "../../lib/format";
import { problemMessage } from "../../lib/api/client";
import { useAuthState } from "../../lib/auth/authStore";
import { deleteProduct, getProduct } from "./productApi";

export default function ProductDetailPage() {
  const { id = "" } = useParams();
  const auth = useAuthState();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const product = useQuery({ queryKey: ["product", id], queryFn: () => getProduct(id), enabled: !!id });
  const remove = useMutation({ mutationFn: () => deleteProduct(id), onSuccess: async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["products"] }),
      queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
    ]);
    navigate("/products");
  }});

  if (product.isLoading) return <LoadingState label="Loading product" />;
  if (product.isError || !product.data) return <ErrorState message="Product details could not be loaded."
    onRetry={() => void product.refetch()} />;
  const item = product.data;

  function handleDelete() {
    if (window.confirm(`Deactivate ${item.name}? Existing order history will be preserved.`)) remove.mutate();
  }

  return <div className="page-stack narrow-page">
    <Link to="/products" className="back-link"><ArrowLeft size={17} /> Back to products</Link>
    <header className="detail-header">
      <div><div className="detail-badges"><span className="sku">{item.sku}</span>
        <span className={`status ${item.active ? "status-active" : "status-inactive"}`}>{item.active ? "Active" : "Inactive"}</span></div>
        <h1>{item.name}</h1><p>{item.description || "No description provided."}</p></div>
      {auth.user?.role === "ADMIN" && <div className="page-actions">
        <Link className="button button-secondary" to={`/products/${item.id}/edit`}><PencilSimple size={18} /> Edit</Link>
        <button className="button button-danger" type="button" onClick={handleDelete} disabled={remove.isPending}>
          <Trash size={18} /> {remove.isPending ? "Deactivating…" : "Deactivate"}</button>
      </div>}
    </header>
    {remove.isError && <div className="form-alert" role="alert">{problemMessage(remove.error)}</div>}
    <section className="detail-grid">
      <article className="detail-card detail-price"><span>Current price</span><strong>{formatCurrency(item.price)}</strong></article>
      <article className="detail-card"><span>Category</span><strong>{item.categoryName}</strong></article>
      <article className="detail-card"><span>Created</span><strong>{formatDate(item.createdAt)}</strong></article>
      <article className="detail-card"><span>Last updated</span><strong>{formatDate(item.updatedAt)}</strong></article>
    </section>
  </div>;
}
