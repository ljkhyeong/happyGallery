import type { ReactNode } from "react";
import { Container } from "react-bootstrap";

const QUICK_LINKS = [
  { href: "/classes", title: "클래스", description: "원데이·정규 수업 일정" },
  { href: "/products", title: "작품", description: "공방에서 만든 작품" },
  { href: "/events", title: "이벤트", description: "진행 중인 소식" },
  { href: "/guest", title: "비회원 조회", description: "주문·예약 확인" },
] as const;

interface Props {
  kicker: string;
  title: string;
  description: ReactNode;
  actions?: ReactNode;
  /** 머리글 없이 그려지는 오류 화면(루트 오류·화면 충돌)에서 공방 이름으로 홈 링크를 보여 준다. */
  standalone?: boolean;
}

/**
 * 없는 주소·불러오기 실패·화면 오류가 함께 쓰는 안내 화면.
 * 오류 뒤 남은 화면 상태를 비우도록 바로가기는 전체 페이지 이동(a href)으로 연다.
 */
export function StatusPage({ kicker, title, description, actions, standalone = false }: Props) {
  return (
    <Container className="page-container status-page">
      {standalone && <a href="/" className="status-page-brand">해피갤러리</a>}
      <section className="status-page-card" aria-labelledby="status-page-title">
        <p className="store-section-kicker mb-2">{kicker}</p>
        <h1 id="status-page-title" className="status-page-title">{title}</h1>
        <p className="status-page-desc">{description}</p>
        {actions && <div className="status-page-actions">{actions}</div>}
      </section>
      <nav className="status-page-links" aria-label="주요 화면 바로가기">
        {QUICK_LINKS.map((link) => (
          <a key={link.href} href={link.href}>
            <b>{link.title}</b>
            <span>{link.description}</span>
          </a>
        ))}
      </nav>
    </Container>
  );
}
