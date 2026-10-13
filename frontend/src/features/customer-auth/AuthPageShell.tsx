import type { ReactNode } from "react";
import { Card, Col, Container, Row } from "react-bootstrap";
import { useSearchParams } from "react-router";
import { resolveSafeReturnTo } from "./navigation";

/** 로그인·회원가입 뒤 돌아갈 안전한 경로와 비회원 기록 가져오기 의도를 주소에서 읽는다. */
export function useAuthPageTarget() {
  const [searchParams] = useSearchParams();
  const returnTo = resolveSafeReturnTo(searchParams.get("redirect"));
  const claimIntent = searchParams.get("claim") === "1" || returnTo.includes("claim=1");
  return { returnTo, claimIntent };
}

interface Props {
  title: string;
  /** 오류 안내와 입력 폼. */
  children: ReactNode;
  /** "또는" 구분선 아래의 소셜 로그인 버튼. */
  social: ReactNode;
  footer: ReactNode;
  hero: {
    title: string;
    description?: string;
    benefits?: readonly string[];
    links: ReactNode;
  };
}

/** 로그인·회원가입 화면 틀: 입력 카드와 안내 카드를 2단으로 놓는다(모바일은 입력 카드가 먼저). */
export function AuthPageShell({ title, children, social, footer, hero }: Props) {
  return (
    <Container className="page-container auth-shell" style={{ maxWidth: 980 }}>
      <Row className="g-4 align-items-stretch">
        <Col lg={{ span: 7, order: 2 }}>
          <Card className="auth-form-card border-0 h-100">
            <Card.Body className="p-4 p-lg-5">
              <h1 className="h3 mb-3">{title}</h1>
              {children}
              <div className="d-flex align-items-center my-4">
                <hr className="flex-grow-1" />
                <span className="px-3 text-muted-soft small">또는</span>
                <hr className="flex-grow-1" />
              </div>
              {social}
              <div className="auth-footer-link mt-4">{footer}</div>
            </Card.Body>
          </Card>
        </Col>
        <Col lg={{ span: 5, order: 1 }}>
          <Card className="auth-hero-card border-0 h-100">
            <Card.Body className="p-4 p-lg-5 d-flex flex-column">
              <h2 className="mb-3">{hero.title}</h2>
              {hero.description && <p className="text-muted-soft mb-4">{hero.description}</p>}
              {hero.benefits && (
                <ul className="auth-benefit-list mb-4">
                  {hero.benefits.map((benefit) => (
                    <li key={benefit} className="auth-benefit-item">{benefit}</li>
                  ))}
                </ul>
              )}
              <div className="d-flex flex-wrap gap-3 mt-auto small">{hero.links}</div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
}
