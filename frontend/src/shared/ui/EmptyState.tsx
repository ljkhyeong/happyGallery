interface Props {
  message?: string;
}

export function EmptyState({ message = "데이터가 없습니다." }: Props) {
  return (
    <div className="empty-state">
      <p className="mb-0">{message}</p>
    </div>
  );
}
