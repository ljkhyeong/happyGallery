import type { ReactNode } from "react";
import { Button } from "react-bootstrap";
import { Link, NavLink } from "react-router";
import type { GuestLookupKind } from "./GuestLookupForm";

interface Props {
  kind: GuestLookupKind;
  title: string;
  children: ReactNode;
  onKindChange?: (kind: GuestLookupKind) => void;
  onHelpRequest?: () => void;
}

export function GuestLookupPanel({ kind, title, children, onKindChange, onHelpRequest }: Props) {
  return (
    <section className="guest-lookup-panel" aria-labelledby="guest-lookup-title">
      <header className="guest-lookup-heading">
        <h1 id="guest-lookup-title">{title}</h1>
        <p>회원가입 없이 주문과 수업 예약 내역을 확인하세요.</p>
      </header>
      <div className="guest-lookup-tabs" aria-label="조회 종류">
        {(["orders", "bookings"] as const).map((value) => {
          const label = value === "orders" ? "주문 조회" : "예약 조회";
          return onKindChange ? (
            <Button key={value} variant="link" aria-pressed={kind === value}
              className={kind === value ? "active" : ""} onClick={() => onKindChange(value)}>
              {label}
            </Button>
          ) : (
            <NavLink key={value} to={`/guest/${value}`}>{label}</NavLink>
          );
        })}
      </div>
      <div className="guest-lookup-fields">
        <p className="guest-lookup-instruction">
          {kind === "orders" ? "주문 완료" : "예약 완료"} 화면에서 안내받은 번호와 조회 코드를 입력해 주세요.
        </p>
        {children}
        <Link className="guest-lookup-help-link" to="/guest#lookup-help" onClick={onHelpRequest}>
          번호나 조회 코드를 잊으셨나요?
        </Link>
      </div>
    </section>
  );
}
