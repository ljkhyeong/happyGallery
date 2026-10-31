import type { ReactNode } from "react";

interface Props {
  message: string;
  /** 빈 목록에서 이어 갈 행동(예: 조건을 풀고 전체 목록 보기). 문구 아래 가운데에 둔다. */
  action?: ReactNode;
}

export function EmptyState({ message, action }: Props) {
  return (
    <div className="empty-state">
      <p className="mb-0">{message}</p>
      {action && <div className="mt-3">{action}</div>}
    </div>
  );
}
