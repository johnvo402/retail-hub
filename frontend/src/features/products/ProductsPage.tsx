import { MagnifyingGlass, Plus } from "@phosphor-icons/react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useDeferredValue, useState } from "react";
import { Link } from "react-router-dom";
import { PageHeader } from "../../components/PageHeader";
import { Pagination } from "../../components/Pagination";
import { EmptyState, ErrorState, LoadingState } from "../../components/States";
import { formatCurrency } from "../../lib/format";
import { useAuthState } from "../../lib/auth/authStore";
import { getCategories } from "../categories/categoryApi";
import { listProducts, searchProducts } from "./productApi";

export default function ProductsPage() {
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("");
  const [page, setPage] = useState(0);
  const deferredSearch = useDeferredValue(search.trim());
  const auth = useAuthState();
  const products = useQuery({
    queryKey: ["products", { page, search: deferredSearch, category }],
    queryFn: () => deferredSearch ? searchProducts(deferredSearch, page) : listProducts(page, "", category),
    placeholderData: keepPreviousData,
  });
  const categories = useQuery({ queryKey: ["categories"], queryFn: getCategories });

  return <div className="page-stack">
    <PageHeader eyebrow="Catalog" title="Products" description="Search, filter, and maintain the active retail catalog."
      action={auth.user?.role === "ADMIN" && <Link className="button button-primary" to="/products/new">
        <Plus size={18} aria-hidden="true" /> New product</Link>} />

    <section className="toolbar" aria-label="Product filters">
      <label className="search-field"><span className="sr-only">Search products</span>
        <MagnifyingGlass size={19} aria-hidden="true" />
        <input value={search} onChange={(event) => { setSearch(event.target.value); setPage(0); }}
          placeholder="Search name, SKU, description…" type="search" />
      </label>
      <label className="select-field"><span>Category</span>
        <select value={category} onChange={(event) => { setCategory(event.target.value); setPage(0); }}>
          <option value="">All categories</option>
          {categories.data?.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}
        </select>
      </label>
      <span className="result-count" aria-live="polite">{products.data?.totalItems ?? 0} products</span>
    </section>

    {products.isLoading ? <LoadingState label="Loading products" /> : products.isError ?
      <ErrorState message="Products could not be loaded." onRetry={() => void products.refetch()} /> :
      products.data!.items.length === 0 ? <EmptyState title="No matching products"
        description="Try another search or create the first product in this catalog."
        action={auth.user?.role === "ADMIN" && <Link className="button button-primary" to="/products/new">Create product</Link>} /> :
      <>
        <div className="product-grid">{products.data!.items.map((product) =>
          <Link to={`/products/${product.id}`} className="product-card" key={product.id}>
            <div className="product-card-top"><span className="sku">{product.sku}</span>
              <span className={`status ${product.active ? "status-active" : "status-inactive"}`}>{product.active ? "Active" : "Inactive"}</span></div>
            <div><p className="product-category">{product.categoryName}</p><h2>{product.name}</h2>
              <p className="product-description">{product.description || "No description provided."}</p></div>
            <div className="product-card-bottom"><strong className="price">{formatCurrency(product.price)}</strong><span>View details</span></div>
          </Link>)}</div>
        <Pagination page={products.data!.page} totalPages={products.data!.totalPages} onChange={setPage} />
      </>}
  </div>;
}
