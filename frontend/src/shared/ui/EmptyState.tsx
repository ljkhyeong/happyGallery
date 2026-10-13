interface Props {
  message: string;
}

export function EmptyState({ message }: Props) {
  return (
    <div className="empty-state">
      <p className="mb-0">{message}</p>
    </div>
  );
}
