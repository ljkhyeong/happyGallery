import type { ReactNode } from "react";

export interface DetailRow {
  label: string;
  value: ReactNode;
  /** 배송지·진행 내역처럼 긴 값은 한 줄 전체를 쓴다. */
  wide?: boolean;
}

/** 주문·예약 상세 카드가 함께 쓰는 이름·값 목록. */
export function DetailRows({ rows }: { rows: readonly DetailRow[] }) {
  return (
    <dl className="my-detail-rows">
      {rows.map((row) => (
        <div key={row.label} className={row.wide ? "my-detail-row-wide" : undefined}>
          <dt>{row.label}</dt>
          <dd>{row.value}</dd>
        </div>
      ))}
    </dl>
  );
}
