export const formatCurrency = (value: number) => new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
}).format(value);

export const formatDate = (value: string) => new Intl.DateTimeFormat("en-US", {
  dateStyle: "medium",
  timeStyle: "short",
}).format(new Date(value));

export const shortId = (value: string) => value.slice(0, 8).toUpperCase();

