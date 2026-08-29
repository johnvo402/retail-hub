import { ArrowRight, Package, Receipt, TrendDown, Warehouse } from "@phosphor-icons/react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { ErrorState, LoadingState } from "../../components/States";
import { PageHeader } from "../../components/PageHeader";
import { formatCurrency, formatDate, shortId } from "../../lib/format";
import { getDashboardOverview } from "./dashboardApi";

export default function DashboardPage() {
  const overview = useQuery({
    queryKey: ["dashboard"],
    queryFn: getDashboardOverview,
    staleTime: 30_000,
  });

  if (overview.isLoading) return <LoadingState label="Building your overview" />;
  if (overview.isError) {
    return <ErrorState message="The operational overview could not be loaded."
      onRetry={() => void overview.refetch()} />;
  }
  const data = overview.data!;

  return <div className="page-stack">
    <PageHeader eyebrow="Operations overview" title="Good to see you."
      description="A concise view of catalog activity, available stock, and current orders." />

    <section className="metric-grid" aria-label="Operational metrics">
      <article className="metric-card"><span className="metric-icon"><Package size={21} aria-hidden="true" /></span>
        <div><span>Active products</span><strong>{data.activeProductCount}</strong></div>
        <Link to="/products" aria-label="View products"><ArrowRight size={19} aria-hidden="true" /></Link></article>
      <article className="metric-card"><span className="metric-icon"><Warehouse size={21} aria-hidden="true" /></span>
        <div><span>Inventory lines</span><strong>{data.inventoryLineCount}</strong></div>
        <Link to="/inventory" aria-label="View inventory"><ArrowRight size={19} aria-hidden="true" /></Link></article>
      <article className="metric-card"><span className="metric-icon metric-icon-warn"><TrendDown size={21} aria-hidden="true" /></span>
        <div><span>Low stock</span><strong>{data.lowStockCount}</strong></div>
        <Link to="/inventory" aria-label="Review low stock"><ArrowRight size={19} aria-hidden="true" /></Link></article>
      <article className="metric-card"><span className="metric-icon"><Receipt size={21} aria-hidden="true" /></span>
        <div><span>Draft orders</span><strong>{data.draftOrderCount}</strong></div>
        <Link to="/orders" aria-label="View orders"><ArrowRight size={19} aria-hidden="true" /></Link></article>
    </section>

    <div className="dashboard-grid">
      <section className="panel">
        <div className="panel-heading"><div><p className="eyebrow">Latest activity</p><h2>Recent orders</h2></div>
          <Link className="text-link" to="/orders">View all <ArrowRight size={16} aria-hidden="true" /></Link></div>
        {data.recentOrders.length === 0 ? <p className="panel-empty">No orders have been created yet.</p> :
          <div className="compact-list">{data.recentOrders.map((order) =>
            <Link to={`/orders/${order.id}`} key={order.id} className="compact-row">
              <span><strong>Order {shortId(order.id)}</strong><small>{formatDate(order.createdAt)}</small></span>
              <span className={`status status-${order.status.toLowerCase()}`}>{order.status}</span>
              <strong className="numeric">{formatCurrency(order.totalAmount)}</strong>
            </Link>)}</div>}
      </section>
      <section className="panel">
        <div className="panel-heading"><div><p className="eyebrow">Attention</p><h2>Low-stock products</h2></div></div>
        {data.lowStockItems.length === 0 ? <p className="panel-empty">Stock levels look healthy.</p> :
          <div className="compact-list">{data.lowStockItems.map((item) =>
            <Link to={`/inventory/${item.productId}`} key={item.productId} className="compact-row">
              <span><strong>{item.productName}</strong><small>{item.sku}</small></span>
              <span className="stock-count">{item.quantity} left</span>
            </Link>)}</div>}
      </section>
    </div>
  </div>;
}
