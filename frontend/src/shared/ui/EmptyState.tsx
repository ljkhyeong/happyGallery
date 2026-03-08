interface Props {
  message?: string;
}

export function EmptyState({ message = "데이터가 없습니다." }: Props) {
  return (
    <div className="text-center py-5 text-muted-soft">
      <p className="mb-0">{message}</p>
    </div>
  );
}
