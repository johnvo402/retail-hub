import { Devices, ShieldCheck, SignOut, UserCircle } from "@phosphor-icons/react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { PageHeader } from "../../components/PageHeader";
import { LoadingState } from "../../components/States";
import { getCurrentUser, logout, logoutAll } from "../auth/authApi";
import { problemMessage } from "../../lib/api/client";
import { authStore, useAuthState } from "../../lib/auth/authStore";

export default function AccountPage() {
  const auth = useAuthState();
  const navigate = useNavigate();
  const currentUser = useQuery({
    queryKey: ["auth", "me"],
    queryFn: getCurrentUser,
    enabled: auth.status === "authenticated",
    staleTime: 0,
  });
  const signOut = useMutation({
    mutationFn: logout,
    onSettled: () => navigate("/login", { replace: true }),
  });
  const signOutAll = useMutation({
    mutationFn: logoutAll,
    onSuccess: () => navigate("/login", { replace: true }),
    onError: () => {
      if (authStore.getSnapshot().status === "unauthenticated") {
        navigate("/login", { replace: true });
      }
    },
  });

  if (!auth.user) return <LoadingState label="Loading account" />;

  function handleLogoutAll() {
    if (window.confirm("Sign out of all devices? You will need to sign in again everywhere.")) {
      signOutAll.mutate();
    }
  }

  const busy = signOut.isPending || signOutAll.isPending;
  return <div className="page-stack narrow-page account-page">
    <PageHeader eyebrow="Account" title="Account & security"
      description="Review your account identity and control active sign-in sessions." />

    <div className="account-layout">
      <section className="panel account-summary" aria-labelledby="account-details-title">
        <div className="account-section-heading">
          <span className="account-section-icon"><UserCircle size={22} aria-hidden="true" /></span>
          <div><p className="eyebrow">Profile</p><h2 id="account-details-title">Account details</h2></div>
        </div>
        <dl className="account-details">
          <div><dt>Email</dt><dd className="long-token">{auth.user.email}</dd></div>
          <div><dt>Role</dt><dd>{auth.user.role}</dd></div>
        </dl>
        {currentUser.isFetching && <p className="account-sync" role="status">Refreshing account details…</p>}
        {currentUser.isError && <div className="form-alert account-error" role="alert">
          <span>{problemMessage(currentUser.error)}</span>
          <button className="button button-secondary button-compact" type="button"
            onClick={() => void currentUser.refetch()}>Try again</button>
        </div>}
      </section>

      <section className="panel security-panel" aria-labelledby="security-title">
        <div className="account-section-heading">
          <span className="account-section-icon"><ShieldCheck size={22} aria-hidden="true" /></span>
          <div><p className="eyebrow">Security</p><h2 id="security-title">Sign-in sessions</h2></div>
        </div>
        <div className="security-list">
          <article className="security-row">
            <div className="security-copy"><span className="security-row-icon"><SignOut size={20} aria-hidden="true" /></span>
              <div><h3>Current session</h3><p>Sign out from this browser and clear its in-memory access token.</p></div></div>
            <button className="button button-secondary" type="button" disabled={busy}
              onClick={() => signOut.mutate()}>{signOut.isPending ? "Signing out…" : "Sign out"}</button>
          </article>
          <article className="security-row">
            <div className="security-copy"><span className="security-row-icon"><Devices size={20} aria-hidden="true" /></span>
              <div><h3>All sessions</h3><p>Revoke every refresh session. Other devices may keep working briefly with an
                already-issued access token, but they cannot refresh it after it expires.</p></div></div>
            <button className="button button-danger" type="button" disabled={busy}
              onClick={handleLogoutAll}>{signOutAll.isPending ? "Signing out…" : "Sign out all devices"}</button>
            {signOutAll.isError && auth.status === "authenticated"
              && <div className="form-alert security-error" role="alert">{problemMessage(signOutAll.error)}</div>}
          </article>
        </div>
      </section>
    </div>
  </div>;
}
