import type { ReactNode } from "react";
import { Button } from "react-bootstrap";
import { Link, NavLink } from "react-router";
import type { GuestLookupKind } from "./GuestLookupForm";

interface Props {
  kind: GuestLookupKind;
  title: string;
  /** 화면에 이미 h1이 있으면(조회 결과의 다른 번호로 찾기) 제목 줄을 그리지 않고 구역 이름으로만 쓴다. */
  showTitle?: boolean;
  children: ReactNode;
  onKindChange?: (kind: GuestLookupKind) => void;
  onHelpRequest?: () => void;
}

export function GuestLookupPanel({ kind, title, showTitle = true, children, onKindChange, onHelpRequest }: Props) {
  return (
    <section
      className="guest-lookup-panel"
      aria-labelledby={showTitle ? "guest-lookup-title" : undefined}
      aria-label={showTitle ? undefined : title}
    >
      {showTitle && (
        <header className="guest-lookup-heading">
          <h1 id="guest-lookup-title">{title}</h1>
        </header>
      )}
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
        {/* 조회 코드는 결제 완료 뒤 상세로 바로 이어질 때 쓰여 화면에 따로 보이지 않는다. 모르면 아래 도움말의 휴대폰 인증으로 찾는다. */}
        <p className="guest-lookup-instruction">
          {kind === "orders" ? "주문" : "예약"} 번호와 조회 코드를 입력해 주세요.
        </p>
        {children}
        <Link
          className="guest-lookup-help-link"
          to={kind === "bookings" ? "/guest?kind=bookings#lookup-help" : "/guest#lookup-help"}
          onClick={onHelpRequest}
        >
          번호나 조회 코드를 잊으셨나요?
        </Link>
      </div>
    </section>
  );
}
