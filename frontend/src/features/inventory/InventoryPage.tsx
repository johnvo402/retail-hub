import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Link } from "react-router-dom";
import { PageHeader } from "../../components/PageHeader";
import { Pagination } from "../../components/Pagination";
import { EmptyState, ErrorState, LoadingState } from "../../components/States";
import { formatDate } from "../../lib/format";
import { useAuthState } from "../../lib/auth/authStore";
import { listInventory } from "./inventoryApi";
import { StockEditor } from "./StockEditor";

export default function InventoryPage() {
  const [page, setPage] = useState(0);
  const { user } = useAuthState();
  const isAdmin = user?.role === "ADMIN";
  const inventory = useQuery({ queryKey: ["inventory", { page }], queryFn: () => listInventory(page),
    placeholderData: keepPreviousData });

  return <div className="page-stack">
    <PageHeader eyebrow="Stock control" title="Inventory"
      description="Review current stock and its auditable movement history." />
    {inventory.isLoading ? <LoadingState label="Loading inventory" /> : inventory.isError ?
      <ErrorState message="Inventory could not be loaded." onRetry={() => void inventory.refetch()} /> :
      inventory.data!.items.length === 0 ? <EmptyState title="No inventory yet" description="Create a product to initialize its stock record." /> : <>
        <section className="table-panel" aria-label="Inventory items">
          <div className={`data-table inventory-table ${isAdmin ? "" : "inventory-table-readonly"}`} role="table">
            <div className="table-row table-head" role="row"><span>Product</span><span>Available</span><span>Version</span><span>Updated</span>{isAdmin && <span>Adjustment</span>}</div>
            {inventory.data!.items.map((item) => <div className="table-row" role="row" key={item.productId}>
              <span data-label="Product"><Link className="inventory-product-link" to={`/inventory/${item.productId}`}>
                <strong>{item.productName}</strong><small>{item.sku}</small>
              </Link></span>
              <span data-label="Available"><strong className={`quantity ${item.quantity < 5 ? "quantity-low" : ""}`}>{item.quantity}</strong></span>
              <span data-label="Version" className="numeric">v{item.version}</span>
              <span data-label="Updated"><time dateTime={item.updatedAt}>{formatDate(item.updatedAt)}</time></span>
              {isAdmin && <span data-label="Adjustment"><StockEditor item={item} /></span>}
            </div>)}
          </div>
        </section>
        <Pagination page={inventory.data!.page} totalPages={inventory.data!.totalPages} onChange={setPage} />
      </>}
  </div>;
}
