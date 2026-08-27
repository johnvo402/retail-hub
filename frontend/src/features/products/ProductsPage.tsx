import { MagnifyingGlass, Plus } from "@phosphor-icons/react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { type FormEvent, useDeferredValue, useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { PageHeader } from "../../components/PageHeader";
import { Pagination } from "../../components/Pagination";
import { EmptyState, ErrorState, LoadingState } from "../../components/States";
import { formatCurrency } from "../../lib/format";
import { useAuthState } from "../../lib/auth/authStore";
import { getCategories } from "../categories/categoryApi";
import {
  isProductSort,
  listProducts,
  PRODUCT_SORT_OPTIONS,
  type ProductFilters,
  type ProductSort,
} from "./productApi";

const DEFAULT_SORT: ProductSort = "createdAt,desc";
const UUID_PATTERN = /^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/i;

function readPage(value: string | null) {
  if (!value || !/^\d+$/.test(value)) return 0;
  const page = Number(value);
  return Number.isSafeInteger(page) ? page : 0;
}

function readPrice(value: string): number | undefined | null {
  if (!value.trim()) return undefined;
  const price = Number(value);
  return Number.isFinite(price) && price >= 0 ? price : null;
}

function validatePrices(minimum: string, maximum: string) {
  const minPrice = readPrice(minimum);
  const maxPrice = readPrice(maximum);
  if (minPrice === null) return "Minimum price must be a number greater than or equal to zero.";
  if (maxPrice === null) return "Maximum price must be a number greater than or equal to zero.";
  if (minPrice !== undefined && maxPrice !== undefined && minPrice > maxPrice) {
    return "Minimum price cannot be greater than maximum price.";
  }
  return null;
}

export default function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const auth = useAuthState();
  const admin = auth.user?.role === "ADMIN";
  const keyword = searchParams.get("keyword")?.trim() ?? "";
  const categoryValue = searchParams.get("category")?.trim() ?? "";
  const category = UUID_PATTERN.test(categoryValue) ? categoryValue : "";
  const activeValue = searchParams.get("active");
  const active = admin
    ? activeValue === "true" ? true : activeValue === "false" ? false : undefined
    : true;
  const sortValue = searchParams.get("sort");
  const sort = isProductSort(sortValue) ? sortValue : DEFAULT_SORT;
  const page = readPage(searchParams.get("page"));
  const rawMinPrice = searchParams.get("minPrice") ?? "";
  const rawMaxPrice = searchParams.get("maxPrice") ?? "";
  const parsedMinPrice = readPrice(rawMinPrice);
  const parsedMaxPrice = readPrice(rawMaxPrice);
  const appliedPriceRangeValid = parsedMinPrice !== null && parsedMaxPrice !== null
    && !(parsedMinPrice !== undefined && parsedMaxPrice !== undefined && parsedMinPrice > parsedMaxPrice);
  const [searchInput, setSearchInput] = useState(keyword);
  const [minPriceInput, setMinPriceInput] = useState(rawMinPrice);
  const [maxPriceInput, setMaxPriceInput] = useState(rawMaxPrice);
  const [priceError, setPriceError] = useState<string | null>(() => validatePrices(rawMinPrice, rawMaxPrice));
  const deferredSearch = useDeferredValue(searchInput.trim());

  useEffect(() => setSearchInput(keyword), [keyword]);
  useEffect(() => {
    setMinPriceInput(rawMinPrice);
    setMaxPriceInput(rawMaxPrice);
    setPriceError(validatePrices(rawMinPrice, rawMaxPrice));
  }, [rawMinPrice, rawMaxPrice]);

  const filters: ProductFilters = {
    page,
    size: 12,
    active,
    sort,
    ...(category && { category }),
    ...(deferredSearch && { keyword: deferredSearch }),
    ...(appliedPriceRangeValid && parsedMinPrice !== undefined && { minPrice: parsedMinPrice }),
    ...(appliedPriceRangeValid && parsedMaxPrice !== undefined && { maxPrice: parsedMaxPrice }),
  };
  const products = useQuery({
    queryKey: ["products", filters],
    queryFn: () => listProducts(filters),
    placeholderData: keepPreviousData,
  });
  const categories = useQuery({ queryKey: ["categories"], queryFn: getCategories });

  function setFilter(name: string, value: string) {
    const next = new URLSearchParams(searchParams);
    if (value) next.set(name, value);
    else next.delete(name);
    next.delete("page");
    setSearchParams(next);
  }

  function changeSearch(value: string) {
    setSearchInput(value);
    const next = new URLSearchParams(searchParams);
    const normalized = value.trim();
    if (normalized) next.set("keyword", normalized);
    else next.delete("keyword");
    next.delete("page");
    setSearchParams(next, { replace: true });
  }

  function applyPrices(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const error = validatePrices(minPriceInput, maxPriceInput);
    setPriceError(error);
    if (error) return;
    const next = new URLSearchParams(searchParams);
    const minimum = readPrice(minPriceInput);
    const maximum = readPrice(maxPriceInput);
    if (minimum === undefined) next.delete("minPrice");
    else next.set("minPrice", String(minimum));
    if (maximum === undefined) next.delete("maxPrice");
    else next.set("maxPrice", String(maximum));
    next.delete("page");
    setSearchParams(next);
  }

  function resetFilters() {
    setSearchInput("");
    setMinPriceInput("");
    setMaxPriceInput("");
    setPriceError(null);
    setSearchParams(new URLSearchParams());
  }

  function changePage(nextPage: number) {
    const next = new URLSearchParams(searchParams);
    if (nextPage > 0) next.set("page", String(nextPage));
    else next.delete("page");
    setSearchParams(next);
  }

  return <div className="page-stack">
    <PageHeader eyebrow="Catalog" title="Products" description="Search, filter, and maintain the retail catalog."
      action={admin && <Link className="button button-primary" to="/products/new">
        <Plus size={18} aria-hidden="true" /> New product</Link>} />

    <form className="toolbar product-toolbar" aria-label="Product filters" onSubmit={applyPrices} noValidate>
      <label className="search-field"><span className="sr-only">Search products</span>
        <MagnifyingGlass size={19} aria-hidden="true" />
        <input value={searchInput} onChange={(event) => changeSearch(event.target.value)}
          placeholder="Search name, SKU, description…" type="search" />
      </label>
      <label className="select-field"><span>Category</span>
        <select value={category} onChange={(event) => setFilter("category", event.target.value)}>
          <option value="">All categories</option>
          {categories.data?.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}
        </select>
      </label>
      {admin && <label className="select-field status-filter"><span>Status</span>
        <select value={active === undefined ? "" : String(active)}
          onChange={(event) => setFilter("active", event.target.value)}>
          <option value="">All statuses</option>
          <option value="true">Active</option>
          <option value="false">Inactive</option>
        </select>
      </label>}
      <label className="select-field price-filter"><span>Minimum price</span>
        <input type="number" min="0" step="0.01" inputMode="decimal" value={minPriceInput}
          aria-invalid={!!priceError} aria-describedby={priceError ? "price-filter-error" : undefined}
          onChange={(event) => { setMinPriceInput(event.target.value); setPriceError(null); }} />
      </label>
      <label className="select-field price-filter"><span>Maximum price</span>
        <input type="number" min="0" step="0.01" inputMode="decimal" value={maxPriceInput}
          aria-invalid={!!priceError} aria-describedby={priceError ? "price-filter-error" : undefined}
          onChange={(event) => { setMaxPriceInput(event.target.value); setPriceError(null); }} />
      </label>
      <label className="select-field sort-filter"><span>Sort by</span>
        <select value={sort} onChange={(event) => setFilter("sort", event.target.value)}>
          {PRODUCT_SORT_OPTIONS.map((option) => <option value={option.value} key={option.value}>{option.label}</option>)}
        </select>
      </label>
      <div className="filter-actions">
        <button className="button button-secondary" type="submit">Apply prices</button>
        <button className="button button-ghost" type="button" onClick={resetFilters}>Reset</button>
      </div>
      {priceError && <span className="filter-error" id="price-filter-error" role="alert">{priceError}</span>}
      <span className="result-count" aria-live="polite">{products.data?.totalItems ?? 0} products</span>
    </form>

    {products.isLoading ? <LoadingState label="Loading products" /> : products.isError ?
      <ErrorState message="Products could not be loaded." onRetry={() => void products.refetch()} /> :
      products.data!.items.length === 0 ? <EmptyState title="No matching products"
        description="Try another search or create the first product in this catalog."
        action={admin && <Link className="button button-primary" to="/products/new">Create product</Link>} /> :
      <>
        <div className="product-grid">{products.data!.items.map((product) =>
          <Link to={`/products/${product.id}`} className="product-card" key={product.id}>
            <div className="product-card-top"><span className="sku">{product.sku}</span>
              <span className={`status ${product.active ? "status-active" : "status-inactive"}`}>{product.active ? "Active" : "Inactive"}</span></div>
            <div><p className="product-category">{product.categoryName}</p><h2>{product.name}</h2>
              <p className="product-description">{product.description || "No description provided."}</p></div>
            <div className="product-card-bottom"><strong className="price">{formatCurrency(product.price)}</strong><span>View details</span></div>
          </Link>)}</div>
        <Pagination page={products.data!.page} totalPages={products.data!.totalPages} onChange={changePage} />
      </>}
  </div>;
}
