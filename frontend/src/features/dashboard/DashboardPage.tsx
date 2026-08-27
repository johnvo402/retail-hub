import { ArrowRight, Package, Receipt, TrendDown, Warehouse } from "@phosphor-icons/react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { ErrorState, LoadingState } from "../../components/States";
import { PageHeader } from "../../components/PageHeader";
import { formatCurrency, formatDate, shortId } from "../../lib/format";
import { listInventory } from "../inventory/inventoryApi";
import { listOrders } from "../orders/orderApi";
import { listProducts } from "../products/productApi";

export default function DashboardPage() {
  const products = useQuery({ queryKey: ["products", "dashboard"], queryFn: () => listProducts({ active: true }) });
  const inventory = useQuery({ queryKey: ["inventory", "dashboard"], queryFn: () => listInventory(0) });
  const orders = useQuery({ queryKey: ["orders", "dashboard"], queryFn: () => listOrders(0) });

  if (products.isLoading || inventory.isLoading || orders.isLoading) return <LoadingState label="Building your overview" />;
  if (products.isError || inventory.isError || orders.isError) {
    return <ErrorState message="The operational overview could not be loaded."
      onRetry={() => void Promise.all([products.refetch(), inventory.refetch(), orders.refetch()])} />;
  }

  const lowStock = inventory.data!.items.filter((item) => item.quantity < 5);
  const openOrders = orders.data!.items.filter((order) => order.status === "DRAFT");

  return <div className="page-stack">
    <PageHeader eyebrow="Operations overview" title="Good to see you."
      description="A concise view of catalog activity, available stock, and current orders." />

    <section className="metric-grid" aria-label="Operational metrics">
      <article className="metric-card"><span className="metric-icon"><Package size={21} /></span>
        <div><span>Active products</span><strong>{products.data!.totalItems}</strong></div>
        <Link to="/products" aria-label="View products"><ArrowRight size={19} /></Link></article>
      <article className="metric-card"><span className="metric-icon"><Warehouse size={21} /></span>
        <div><span>Inventory lines</span><strong>{inventory.data!.totalItems}</strong></div>
        <Link to="/inventory" aria-label="View inventory"><ArrowRight size={19} /></Link></article>
      <article className="metric-card"><span className="metric-icon metric-icon-warn"><TrendDown size={21} /></span>
        <div><span>Low stock</span><strong>{lowStock.length}</strong></div>
        <Link to="/inventory" aria-label="Review low stock"><ArrowRight size={19} /></Link></article>
      <article className="metric-card"><span className="metric-icon"><Receipt size={21} /></span>
        <div><span>Draft orders</span><strong>{openOrders.length}</strong></div>
        <Link to="/orders" aria-label="View orders"><ArrowRight size={19} /></Link></article>
    </section>

    <div className="dashboard-grid">
      <section className="panel">
        <div className="panel-heading"><div><p className="eyebrow">Latest activity</p><h2>Recent orders</h2></div>
          <Link className="text-link" to="/orders">View all <ArrowRight size={16} /></Link></div>
        {orders.data!.items.length === 0 ? <p className="panel-empty">No orders have been created yet.</p> :
          <div className="compact-list">{orders.data!.items.slice(0, 5).map((order) =>
            <Link to={`/orders/${order.id}`} key={order.id} className="compact-row">
              <span><strong>Order {shortId(order.id)}</strong><small>{formatDate(order.createdAt)}</small></span>
              <span className={`status status-${order.status.toLowerCase()}`}>{order.status}</span>
              <strong className="numeric">{formatCurrency(order.totalAmount)}</strong>
            </Link>)}</div>}
      </section>
      <section className="panel">
        <div className="panel-heading"><div><p className="eyebrow">Attention</p><h2>Low-stock products</h2></div></div>
        {lowStock.length === 0 ? <p className="panel-empty">Stock levels look healthy.</p> :
          <div className="compact-list">{lowStock.slice(0, 6).map((item) =>
            <Link to="/inventory" key={item.productId} className="compact-row">
              <span><strong>{item.productName}</strong><small>{item.sku}</small></span>
              <span className="stock-count">{item.quantity} left</span>
            </Link>)}</div>}
      </section>
    </div>
  </div>;
}
