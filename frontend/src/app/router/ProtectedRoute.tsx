import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuthState } from "../../lib/auth/authStore";
import { LoadingState } from "../../components/States";

export function ProtectedRoute() {
  const auth = useAuthState();
  const location = useLocation();

  if (auth.status === "initializing") {
    return <main className="centered-page"><LoadingState label="Restoring your session" /></main>;
  }
  if (auth.status !== "authenticated") {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <Outlet />;
}

