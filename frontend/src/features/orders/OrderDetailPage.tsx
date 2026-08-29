import { ArrowLeft, Check, Plus, Trash, X } from "@phosphor-icons/react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ErrorState, LoadingState } from "../../components/States";
import { formatCurrency, formatDate, shortId } from "../../lib/format";
import { problemMessage } from "../../lib/api/client";
import { getCategories } from "../categories/categoryApi";
import { listProducts } from "../products/productApi";
import { addOrderItem, cancelOrder, confirmOrder, getOrder, removeOrderItem } from "./orderApi";

export default function OrderDetailPage() {
  const { id = "" } = useParams();
  const [productId, setProductId] = useState("");
  const [quantity, setQuantity] = useState(1);
  const queryClient = useQueryClient();
  const order = useQuery({ queryKey: ["order", id], queryFn: () => getOrder(id), enabled: !!id });
  const products = useQuery({ queryKey: ["products", "order-picker"], queryFn: () => listProducts({ active: true }) });
  useQuery({ queryKey: ["categories"], queryFn: getCategories });
  const refresh = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["order", id] }),
      queryClient.invalidateQueries({ queryKey: ["orders"] }),
      queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
    ]);
  };
  const add = useMutation({ mutationFn: () => addOrderItem(id, productId, quantity), onSuccess: async () => {
    setProductId(""); setQuantity(1); await refresh();
  }});
  const remove = useMutation({ mutationFn: (itemId: string) => removeOrderItem(id, itemId), onSuccess: refresh });
  const confirm = useMutation({ mutationFn: () => confirmOrder(id), onSuccess: refresh });
  const cancel = useMutation({ mutationFn: () => cancelOrder(id), onSuccess: refresh });
  const mutationError = add.error ?? remove.error ?? confirm.error ?? cancel.error;

  if (order.isLoading) return <LoadingState label="Loading order" />;
  if (order.isError || !order.data) return <ErrorState message="Order details could not be loaded." onRetry={() => void order.refetch()} />;
  const data = order.data;
  const isDraft = data.status === "DRAFT";

  return <div className="page-stack narrow-page">
    <Link to="/orders" className="back-link"><ArrowLeft size={17} /> Back to orders</Link>
    <header className="detail-header order-heading"><div><p className="eyebrow">Order #{shortId(data.id)}</p>
      <div className="heading-with-status"><h1>Order details</h1><span className={`status status-${data.status.toLowerCase()}`}>{data.status}</span></div>
      <p>Created {formatDate(data.createdAt)}</p></div>
      {isDraft && <div className="page-actions">
        <button className="button button-secondary" type="button" disabled={data.items.length === 0 || confirm.isPending}
          onClick={() => confirm.mutate()}><Check size={18} /> {confirm.isPending ? "Confirming…" : "Confirm order"}</button>
        <button className="button button-danger" type="button" disabled={cancel.isPending}
          onClick={() => window.confirm("Cancel this order? It cannot be modified afterward.") && cancel.mutate()}>
          <X size={18} /> {cancel.isPending ? "Cancelling…" : "Cancel"}</button>
      </div>}
    </header>
    {mutationError && <div className="form-alert" role="alert">{problemMessage(mutationError)}</div>}

    {isDraft && <section className="panel add-item-panel"><div><p className="eyebrow">Draft editor</p><h2>Add a product</h2></div>
      <div className="add-item-form"><label><span>Product</span><select value={productId} onChange={(event) => setProductId(event.target.value)}>
        <option value="">Choose a product</option>{products.data?.items.map((product) =>
          <option value={product.id} key={product.id}>{product.name} · {product.sku}</option>)}</select></label>
        <label><span>Quantity</span><input type="number" min="1" value={quantity}
          onChange={(event) => setQuantity(Math.max(1, Number(event.target.value)))} /></label>
        <button className="button button-primary" type="button" disabled={!productId || add.isPending}
          onClick={() => add.mutate()}><Plus size={18} /> {add.isPending ? "Adding…" : "Add item"}</button></div>
    </section>}

    <section className="panel order-items-panel"><div className="panel-heading"><div><p className="eyebrow">Line items</p><h2>{data.itemCount} items</h2></div>
      <strong className="order-total">{formatCurrency(data.totalAmount)}</strong></div>
      {data.items.length === 0 ? <p className="panel-empty">This draft has no products yet.</p> :
        <div className="line-items">{data.items.map((item) => <div className="line-item" key={item.id}>
          <div><strong>{item.productName}</strong><small>{item.sku} · {formatCurrency(item.unitPrice)} each</small></div>
          <span className="numeric">× {item.quantity}</span><strong className="numeric">{formatCurrency(item.lineTotal)}</strong>
          {isDraft && <button className="icon-button icon-danger" type="button" aria-label={`Remove ${item.productName}`}
            disabled={remove.isPending} onClick={() => remove.mutate(item.id)}><Trash size={18} /></button>}
        </div>)}</div>}
    </section>

    <section className="detail-grid order-meta">
      <article className="detail-card"><span>Order ID</span><strong className="long-token">{data.id}</strong></article>
      <article className="detail-card"><span>Customer</span><strong className="long-token">{data.customerId}</strong></article>
      {data.confirmedAt && <article className="detail-card"><span>Confirmed</span><strong>{formatDate(data.confirmedAt)}</strong></article>}
      {data.cancelledAt && <article className="detail-card"><span>Cancelled</span><strong>{formatDate(data.cancelledAt)}</strong></article>}
    </section>
  </div>;
}
