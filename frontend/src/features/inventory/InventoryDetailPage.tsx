import { ArrowLeft, ArrowRight, ArrowUp, ArrowDown, ClockCounterClockwise } from "@phosphor-icons/react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { EmptyState, ErrorState, LoadingState } from "../../components/States";
import { Pagination } from "../../components/Pagination";
import { formatDate, shortId } from "../../lib/format";
import { useAuthState } from "../../lib/auth/authStore";
import type { InventoryMovementType } from "../../types/api";
import { getInventory, listInventoryMovements } from "./inventoryApi";
import { StockEditor } from "./StockEditor";

const movementLabels: Record<InventoryMovementType, string> = {
  MANUAL_INCREASE: "Manual increase",
  MANUAL_DECREASE: "Manual decrease",
  ORDER_CONFIRMATION: "Order confirmation",
};

export default function InventoryDetailPage() {
  const { productId = "" } = useParams();
  const [page, setPage] = useState(0);
  const { user } = useAuthState();
  const inventory = useQuery({
    queryKey: ["inventory", productId],
    queryFn: () => getInventory(productId),
    enabled: !!productId,
  });
  const movements = useQuery({
    queryKey: ["inventory-movements", productId, { page }],
    queryFn: () => listInventoryMovements(productId, page),
    enabled: !!productId,
    placeholderData: keepPreviousData,
  });

  if (inventory.isLoading) return <LoadingState label="Loading inventory details" />;
  if (inventory.isError || !inventory.data) return <ErrorState message="Inventory details could not be loaded."
    onRetry={() => void inventory.refetch()} />;

  const item = inventory.data;
  return <div className="page-stack narrow-page">
    <Link to="/inventory" className="back-link">
      <ArrowLeft size={17} aria-hidden="true" /> Back to inventory
    </Link>

    <header className="detail-header inventory-detail-header">
      <div>
        <p className="eyebrow">SKU {item.sku}</p>
        <h1>{item.productName}</h1>
        <p>Current stock state and immutable adjustment audit history.</p>
      </div>
      <div className="current-stock" aria-label={`${item.quantity} units currently available`}>
        <span>Current stock</span>
        <strong>{item.quantity}</strong>
        <small>units available</small>
      </div>
    </header>

    <section className="detail-grid inventory-meta" aria-label="Inventory state">
      <article className="detail-card"><span>Product</span><strong>{item.productName}</strong></article>
      <article className="detail-card"><span>SKU</span><strong>{item.sku}</strong></article>
      <article className="detail-card"><span>Inventory version</span><strong className="numeric">v{item.version}</strong></article>
      <article className="detail-card"><span>Last updated</span><strong><time dateTime={item.updatedAt}>{formatDate(item.updatedAt)}</time></strong></article>
    </section>

    {user?.role === "ADMIN" && <section className="panel inventory-adjustment" aria-label="Stock adjustment">
      <div className="panel-heading"><div><p className="eyebrow">Administrator action</p>
        <h2>Adjust stock</h2></div></div>
      <StockEditor item={item} />
    </section>}

    <section className="panel movement-history" aria-labelledby="movement-history-title">
      <div className="panel-heading">
        <div><p className="eyebrow">Audit trail</p><h2 id="movement-history-title">Movement history</h2></div>
        <ClockCounterClockwise size={25} aria-hidden="true" />
      </div>
      {movements.isLoading ? <LoadingState label="Loading movement history" /> : movements.isError ?
        <ErrorState message="Movement history could not be loaded." onRetry={() => void movements.refetch()} /> :
        movements.data!.items.length === 0 ? <EmptyState title="No movements yet"
          description="Stock changes will appear here as they happen." /> : <>
          <div className="movement-list">
            {movements.data!.items.map((movement) => {
              const positive = movement.quantityDelta > 0;
              return <article className="movement-row" key={movement.id} data-testid="inventory-movement">
                <div className={`movement-delta ${positive ? "movement-positive" : "movement-negative"}`}>
                  {positive ? <ArrowUp size={18} aria-hidden="true" /> : <ArrowDown size={18} aria-hidden="true" />}
                  <strong>{positive ? "+" : ""}{movement.quantityDelta}</strong>
                </div>
                <div className="movement-copy">
                  <div className="movement-heading"><h3>{movementLabels[movement.type]}</h3>
                    <time dateTime={movement.createdAt}>{formatDate(movement.createdAt)}</time></div>
                  <div className="movement-balance numeric">
                    <strong>{movement.quantityBefore}</strong><ArrowRight size={15} aria-hidden="true" />
                    <strong>{movement.quantityAfter}</strong>
                  </div>
                  <div className="movement-meta">
                    <span>{movement.actorUserId ? `User ${shortId(movement.actorUserId)}` : "System"}</span>
                    {movement.type === "ORDER_CONFIRMATION" && movement.referenceId &&
                      <Link className="text-link" to={`/orders/${movement.referenceId}`}>
                        Order {shortId(movement.referenceId)}
                      </Link>}
                  </div>
                  {movement.reason && <p className="movement-reason"><strong>Reason:</strong> {movement.reason}</p>}
                </div>
              </article>;
            })}
          </div>
          <Pagination page={movements.data!.page} totalPages={movements.data!.totalPages} onChange={setPage} />
        </>}
    </section>
  </div>;
}
