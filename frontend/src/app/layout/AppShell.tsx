import {
  List,
  Package,
  Receipt,
  SignOut,
  SquaresFour,
  Storefront,
  Warehouse,
  X,
} from "@phosphor-icons/react";
import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { logout } from "../../features/auth/authApi";
import { useAuthState } from "../../lib/auth/authStore";

const links = [
  { to: "/dashboard", label: "Overview", icon: SquaresFour },
  { to: "/products", label: "Products", icon: Package },
  { to: "/inventory", label: "Inventory", icon: Warehouse },
  { to: "/orders", label: "Orders", icon: Receipt },
];

export function AppShell() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const auth = useAuthState();
  const navigate = useNavigate();

  async function handleLogout() {
    setLoggingOut(true);
    await logout();
    navigate("/login", { replace: true });
  }

  return <div className="app-shell">
    <a className="skip-link" href="#main-content">Skip to main content</a>
    <aside className={`sidebar ${menuOpen ? "sidebar-open" : ""}`} aria-label="Primary navigation">
      <div className="brand-row">
        <NavLink className="brand" to="/dashboard" onClick={() => setMenuOpen(false)}>
          <span className="brand-mark"><Storefront size={22} weight="bold" aria-hidden="true" /></span>
          <span>RetailHub</span>
        </NavLink>
        <button className="icon-button sidebar-close" type="button" aria-label="Close navigation"
          onClick={() => setMenuOpen(false)}><X size={20} aria-hidden="true" /></button>
      </div>
      <nav className="nav-list">
        {links.map(({ to, label, icon: Icon }) => <NavLink key={to} to={to}
          onClick={() => setMenuOpen(false)}
          className={({ isActive }) => `nav-link ${isActive ? "nav-link-active" : ""}`}>
          <Icon size={20} aria-hidden="true" /> <span>{label}</span>
        </NavLink>)}
      </nav>
      <div className="account-panel">
        <span className="account-avatar" aria-hidden="true">{auth.user?.email.charAt(0).toUpperCase()}</span>
        <div className="account-copy">
          <strong>{auth.user?.email}</strong>
          <span>{auth.user?.role === "ADMIN" ? "Administrator" : "Team member"}</span>
        </div>
        <button className="icon-button" type="button" aria-label="Sign out" disabled={loggingOut}
          onClick={handleLogout}><SignOut size={19} aria-hidden="true" /></button>
      </div>
    </aside>

    {menuOpen && <button className="nav-scrim" type="button" aria-label="Close navigation"
      onClick={() => setMenuOpen(false)} />}

    <div className="app-content">
      <header className="mobile-header">
        <button className="icon-button" type="button" aria-label="Open navigation" aria-expanded={menuOpen}
          onClick={() => setMenuOpen(true)}><List size={22} aria-hidden="true" /></button>
        <span className="mobile-brand"><Storefront size={20} weight="bold" aria-hidden="true" /> RetailHub</span>
      </header>
      <main id="main-content" className="main-content" tabIndex={-1}>
        <Outlet />
      </main>
    </div>
  </div>;
}

