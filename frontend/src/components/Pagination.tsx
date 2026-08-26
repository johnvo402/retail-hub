import { CaretLeft, CaretRight } from "@phosphor-icons/react";

export function Pagination({ page, totalPages, onChange }: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;
  return <nav className="pagination" aria-label="Pagination">
    <button className="icon-button" type="button" aria-label="Previous page"
      disabled={page === 0} onClick={() => onChange(page - 1)}>
      <CaretLeft size={18} aria-hidden="true" />
    </button>
    <span>Page <strong>{page + 1}</strong> of {totalPages}</span>
    <button className="icon-button" type="button" aria-label="Next page"
      disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
      <CaretRight size={18} aria-hidden="true" />
    </button>
  </nav>;
}

