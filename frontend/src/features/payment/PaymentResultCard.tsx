import type { ReactNode } from "react";
import { CircleAlert, CircleCheck, Info } from "lucide-react";

interface Props {
  /** notice: 결제 확인 중·추가 인증 필요처럼 성공도 실패도 아닌 상태 */
  tone: "success" | "danger" | "notice";
  title: string;
  lead?: ReactNode;
  children?: ReactNode;
  actions?: ReactNode;
}

/** 결제 완료·실패 화면이 함께 쓰는 결과 카드. 다음 행동 버튼을 결과 바로 아래에 둔다. */
export function PaymentResultCard({ tone, title, lead, children, actions }: Props) {
  const Icon = tone === "success" ? CircleCheck : tone === "notice" ? Info : CircleAlert;
  return (
    <section className={`payment-result is-${tone}`} aria-labelledby="payment-result-title">
      <Icon className="payment-result-icon" size={44} strokeWidth={1.6} aria-hidden="true" />
      <h1 id="payment-result-title">{title}</h1>
      {lead && <p className="payment-result-lead">{lead}</p>}
      {children}
      {actions && <div className="payment-result-actions">{actions}</div>}
    </section>
  );
}
