import { Minus, Plus } from "@phosphor-icons/react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import { problemMessage } from "../../lib/api/client";
import type { InventoryItem } from "../../types/api";
import { adjustStock } from "./inventoryApi";

export function StockEditor({ item }: { item: InventoryItem }) {
  const [quantity, setQuantity] = useState(1);
  const [reason, setReason] = useState("");
  const [direction, setDirection] = useState<"increase" | "decrease" | null>(null);
  const queryClient = useQueryClient();
  const adjust = useMutation({
    mutationFn: () => adjustStock(item.productId, direction!, quantity, reason),
    onSuccess: async () => {
      setDirection(null);
      setQuantity(1);
      setReason("");
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["inventory"] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-movements", item.productId] }),
      ]);
    },
  });

  if (!direction) return <div className="stock-actions">
    <button className="button button-secondary stock-button" type="button"
      onClick={() => setDirection("increase")}>
      <Plus size={16} aria-hidden="true" /> Add
    </button>
    <button className="button button-ghost stock-button" type="button" disabled={item.quantity === 0}
      onClick={() => setDirection("decrease")}>
      <Minus size={16} aria-hidden="true" /> Remove
    </button>
  </div>;

  const submit = (event: FormEvent) => {
    event.preventDefault();
    adjust.mutate();
  };

  return <form className="stock-editor" onSubmit={submit}>
    <label className="stock-editor-field stock-quantity-field">
      <span>Quantity</span>
      <input type="number" min="1" max={direction === "decrease" ? item.quantity : undefined}
        value={quantity} onChange={(event) => setQuantity(Math.max(1, Number(event.target.value)))} />
    </label>
    <label className="stock-editor-field stock-reason-field">
      <span>Reason (optional)</span>
      <input type="text" maxLength={500} value={reason}
        onChange={(event) => setReason(event.target.value)} placeholder="e.g. Supplier delivery" />
    </label>
    <button className="button button-primary stock-button" type="submit" disabled={adjust.isPending}>
      {adjust.isPending ? "Saving…" : direction === "increase" ? "Add stock" : "Remove stock"}
    </button>
    <button className="button button-ghost stock-button" type="button" disabled={adjust.isPending}
      onClick={() => { setDirection(null); setReason(""); setQuantity(1); }}>Cancel</button>
    {adjust.isError && <span className="field-error stock-error" role="alert">
      {problemMessage(adjust.error)}
    </span>}
  </form>;
}
