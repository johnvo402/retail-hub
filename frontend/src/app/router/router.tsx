import { lazy, Suspense } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";
import { AppShell } from "../layout/AppShell";
import { ProtectedRoute } from "./ProtectedRoute";
import { LoadingState } from "../../components/States";

const LoginPage = lazy(() => import("../../features/auth/LoginPage"));
const DashboardPage = lazy(() => import("../../features/dashboard/DashboardPage"));
const ProductsPage = lazy(() => import("../../features/products/ProductsPage"));
const ProductDetailPage = lazy(() => import("../../features/products/ProductDetailPage"));
const ProductFormPage = lazy(() => import("../../features/products/ProductFormPage"));
const CategoriesPage = lazy(() => import("../../features/categories/CategoriesPage"));
const CategoryFormPage = lazy(() => import("../../features/categories/CategoryFormPage"));
const InventoryPage = lazy(() => import("../../features/inventory/InventoryPage"));
const OrdersPage = lazy(() => import("../../features/orders/OrdersPage"));
const OrderDetailPage = lazy(() => import("../../features/orders/OrderDetailPage"));

function pending(element: React.ReactNode) {
  return <Suspense fallback={<LoadingState label="Loading page" />}>{element}</Suspense>;
}

export const router = createBrowserRouter([
  { path: "/login", element: pending(<LoginPage />) },
  {
    element: <ProtectedRoute />,
    children: [{
      element: <AppShell />,
      children: [
        { index: true, element: <Navigate to="/dashboard" replace /> },
        { path: "/dashboard", element: pending(<DashboardPage />) },
        { path: "/products", element: pending(<ProductsPage />) },
        { path: "/products/new", element: pending(<ProductFormPage />) },
        { path: "/products/:id", element: pending(<ProductDetailPage />) },
        { path: "/products/:id/edit", element: pending(<ProductFormPage />) },
        { path: "/categories/manage", element: pending(<CategoriesPage />) },
        { path: "/categories/new", element: pending(<CategoryFormPage />) },
        { path: "/categories/:id/edit", element: pending(<CategoryFormPage />) },
        { path: "/inventory", element: pending(<InventoryPage />) },
        { path: "/orders", element: pending(<OrdersPage />) },
        { path: "/orders/:id", element: pending(<OrderDetailPage />) },
      ],
    }],
  },
  { path: "*", element: <Navigate to="/dashboard" replace /> },
]);
