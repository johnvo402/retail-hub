import { Minus, Plus } from "@phosphor-icons/react";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { PageHeader } from "../../components/PageHeader";
import { Pagination } from "../../components/Pagination";
import { EmptyState, ErrorState, LoadingState } from "../../components/States";
import { formatDate } from "../../lib/format";
import { problemMessage } from "../../lib/api/client";
import type { InventoryItem } from "../../types/api";
import { adjustStock, listInventory } from "./inventoryApi";

function StockEditor({ item }: { item: InventoryItem }) {
  const [quantity, setQuantity] = useState(1);
  const [direction, setDirection] = useState<"increase" | "decrease" | null>(null);
  const queryClient = useQueryClient();
  const adjust = useMutation({ mutationFn: () => adjustStock(item.productId, direction!, quantity),
    onSuccess: async () => { setDirection(null); setQuantity(1); await queryClient.invalidateQueries({ queryKey: ["inventory"] }); } });

  if (!direction) return <div className="stock-actions">
    <button className="button button-compact button-secondary" type="button" onClick={() => setDirection("increase")}>
      <Plus size={16} /> Add</button>
    <button className="button button-compact button-ghost" type="button" disabled={item.quantity === 0}
      onClick={() => setDirection("decrease")}><Minus size={16} /> Remove</button>
  </div>;

  return <div className="stock-editor">
    <label><span className="sr-only">Quantity to {direction}</span>
      <input type="number" min="1" max={direction === "decrease" ? item.quantity : undefined}
        value={quantity} onChange={(event) => setQuantity(Math.max(1, Number(event.target.value)))} /></label>
    <button className="button button-compact button-primary" type="button" disabled={adjust.isPending}
      onClick={() => adjust.mutate()}>{adjust.isPending ? "Saving…" : direction === "increase" ? "Add stock" : "Remove"}</button>
    <button className="button button-compact button-ghost" type="button" onClick={() => setDirection(null)}>Cancel</button>
    {adjust.isError && <span className="field-error stock-error" role="alert">{problemMessage(adjust.error)}</span>}
  </div>;
}

export default function InventoryPage() {
  const [page, setPage] = useState(0);
  const inventory = useQuery({ queryKey: ["inventory", { page }], queryFn: () => listInventory(page),
    placeholderData: keepPreviousData });

  return <div className="page-stack">
    <PageHeader eyebrow="Stock control" title="Inventory"
      description="Adjust quantities with optimistic concurrency protection on every update." />
    {inventory.isLoading ? <LoadingState label="Loading inventory" /> : inventory.isError ?
      <ErrorState message="Inventory could not be loaded." onRetry={() => void inventory.refetch()} /> :
      inventory.data!.items.length === 0 ? <EmptyState title="No inventory yet" description="Create a product to initialize its stock record." /> : <>
        <section className="table-panel" aria-label="Inventory items">
          <div className="data-table inventory-table" role="table">
            <div className="table-row table-head" role="row"><span>Product</span><span>Available</span><span>Version</span><span>Updated</span><span>Adjustment</span></div>
            {inventory.data!.items.map((item) => <div className="table-row" role="row" key={item.productId}>
              <span data-label="Product"><strong>{item.productName}</strong><small>{item.sku}</small></span>
              <span data-label="Available"><strong className={`quantity ${item.quantity < 5 ? "quantity-low" : ""}`}>{item.quantity}</strong></span>
              <span data-label="Version" className="numeric">v{item.version}</span>
              <span data-label="Updated"><time dateTime={item.updatedAt}>{formatDate(item.updatedAt)}</time></span>
              <span data-label="Adjustment"><StockEditor item={item} /></span>
            </div>)}
          </div>
        </section>
        <Pagination page={inventory.data!.page} totalPages={inventory.data!.totalPages} onChange={setPage} />
      </>}
  </div>;
}

