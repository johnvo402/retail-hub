import { ArrowClockwise, Package } from "@phosphor-icons/react";
import type { ReactNode } from "react";

export function LoadingState({ label = "Loading" }: { label?: string }) {
  return <div className="state-panel" role="status" aria-live="polite">
    <span className="spinner" aria-hidden="true" />
    <p>{label}…</p>
  </div>;
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return <div className="state-panel state-error" role="alert">
    <p>{message}</p>
    {onRetry && <button className="button button-secondary" type="button" onClick={onRetry}>
      <ArrowClockwise size={18} aria-hidden="true" /> Retry
    </button>}
  </div>;
}

export function EmptyState({ title, description, action }: {
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return <div className="state-panel empty-state">
    <Package size={32} aria-hidden="true" />
    <h2>{title}</h2>
    <p>{description}</p>
    {action}
  </div>;
}

