import { Plus } from "@phosphor-icons/react";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { PageHeader } from "../../components/PageHeader";
import { Pagination } from "../../components/Pagination";
import { EmptyState, ErrorState, LoadingState } from "../../components/States";
import { formatCurrency, formatDate, shortId } from "../../lib/format";
import { problemMessage } from "../../lib/api/client";
import { createOrder, listOrders } from "./orderApi";

export default function OrdersPage() {
  const [page, setPage] = useState(0);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const orders = useQuery({ queryKey: ["orders", { page }], queryFn: () => listOrders(page), placeholderData: keepPreviousData });
  const create = useMutation({ mutationFn: createOrder, onSuccess: async ({ id }) => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["orders"] }),
      queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
    ]);
    navigate(`/orders/${id}`);
  }});

  return <div className="page-stack">
    <PageHeader eyebrow="Order lifecycle" title="Orders" description="Draft, review, confirm, and audit customer orders."
      action={<button className="button button-primary" type="button" onClick={() => create.mutate()} disabled={create.isPending}>
        <Plus size={18} /> {create.isPending ? "Creating…" : "New order"}</button>} />
    {create.isError && <div className="form-alert" role="alert">{problemMessage(create.error)}</div>}
    {orders.isLoading ? <LoadingState label="Loading orders" /> : orders.isError ?
      <ErrorState message="Orders could not be loaded." onRetry={() => void orders.refetch()} /> :
      orders.data!.items.length === 0 ? <EmptyState title="No orders yet" description="Create a draft order to begin its traceable lifecycle."
        action={<button className="button button-primary" type="button" onClick={() => create.mutate()}>Create order</button>} /> : <>
        <section className="table-panel">
          <div className="data-table order-table" role="table">
            <div className="table-row table-head" role="row"><span>Order</span><span>Status</span><span>Items</span><span>Total</span><span>Created</span></div>
            {orders.data!.items.map((order) => <Link className="table-row table-link" role="row" to={`/orders/${order.id}`} key={order.id}>
              <span data-label="Order"><strong>#{shortId(order.id)}</strong><small className="long-token">{order.id}</small></span>
              <span data-label="Status"><span className={`status status-${order.status.toLowerCase()}`}>{order.status}</span></span>
              <span data-label="Items" className="numeric">{order.itemCount}</span>
              <span data-label="Total" className="numeric"><strong>{formatCurrency(order.totalAmount)}</strong></span>
              <span data-label="Created"><time dateTime={order.createdAt}>{formatDate(order.createdAt)}</time></span>
            </Link>)}
          </div>
        </section>
        <Pagination page={orders.data!.page} totalPages={orders.data!.totalPages} onChange={setPage} />
      </>}
  </div>;
}
