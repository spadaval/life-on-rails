export function EllipsisIcon({ className }: { className?: string }) {
  return (
    <svg
      width="24"
      height="24"
      viewBox="0 0 24 24"
      role="presentation"
      className={className ?? "fill-current text-gray-300"}
    >
      <g fill="currentColor" fillRule="evenodd">
        <circle cx="5" cy="12" r="2"></circle>
        <circle cx="12" cy="12" r="2"></circle>
        <circle cx="19" cy="12" r="2"></circle>
      </g>
    </svg>
  );
}
