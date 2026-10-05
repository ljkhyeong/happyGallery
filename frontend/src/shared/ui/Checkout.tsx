import type { ReactNode } from "react";

/**
 * 예약·4회권·주문서가 함께 쓰는 2단 배치.
 * 데스크톱은 입력(왼쪽)과 고정 요약(오른쪽), 모바일은 한 줄로 쌓고 결제 버튼만 하단에 고정한다.
 */
export function CheckoutLayout({ children, summary }: { children: ReactNode; summary?: ReactNode }) {
  return (
    <div className={`checkout-layout${summary ? "" : " is-single"}`}>
      <div className="checkout-main">{children}</div>
      {summary}
    </div>
  );
}

interface PanelProps {
  step?: number;
  title: string;
  meta?: ReactNode;
  children: ReactNode;
}

/** 한 폼의 단계라 랜드마크(region)로 만들지 않고 h2 제목으로만 구분한다. */
export function CheckoutPanel({ step, title, meta, children }: PanelProps) {
  return (
    <section className="checkout-panel">
      <div className="checkout-panel-head">
        {step !== undefined && <span className="checkout-panel-step" aria-hidden="true">{step}</span>}
        <h2>{title}</h2>
        {meta && <span className="checkout-panel-meta">{meta}</span>}
      </div>
      {children}
    </section>
  );
}

interface SummaryProps {
  label: string;
  media?: ReactNode;
  children: ReactNode;
  action: ReactNode;
  /** 모바일 하단 고정 바에 버튼과 함께 보이는 짧은 금액. 요약 본문과 겹치므로 보조기기에는 숨긴다. */
  mobileTotal?: { label: string; amount: string };
  note?: ReactNode;
  className?: string;
}

export function CheckoutSummary({ label, media, children, action, mobileTotal, note, className }: SummaryProps) {
  return (
    <aside className={className ? `checkout-summary ${className}` : "checkout-summary"} aria-label={label}>
      {media && <div className="checkout-summary-media">{media}</div>}
      <div className="checkout-summary-body">{children}</div>
      <div className="checkout-summary-action">
        {mobileTotal && (
          <p className="checkout-summary-mobile-total" aria-hidden="true">
            {mobileTotal.label}
            <b>{mobileTotal.amount}</b>
          </p>
        )}
        {action}
      </div>
      {note && <div className="checkout-summary-note">{note}</div>}
    </aside>
  );
}

interface AmountRow {
  label: string;
  value: string;
}

export function CheckoutAmounts({ rows, total }: { rows: AmountRow[]; total?: AmountRow }) {
  return (
    <dl className="checkout-amounts">
      {rows.map(({ label, value }) => (
        <div key={label}>
          <dt>{label}</dt>
          <dd>{value}</dd>
        </div>
      ))}
      {total && (
        <div className="is-total">
          <dt>{total.label}</dt>
          <dd>{total.value}</dd>
        </div>
      )}
    </dl>
  );
}
