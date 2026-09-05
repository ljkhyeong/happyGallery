import type { ReactNode } from "react";
import { Container } from "react-bootstrap";
import { Link } from "react-router";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { MyAuthGateCard } from "./MyAuthGateCard";
import { LoadingSpinner } from "@/shared/ui";

export function MySectionPage({ title, children }: { title: string; children: ReactNode }) {
  const { isAuthenticated, isLoading, sessionVersion } = useCustomerAuth();
  return (
    <Container className="page-container" style={{ maxWidth: 720 }}>
      {isLoading ? <LoadingSpinner /> : !isAuthenticated ? (
        <MyAuthGateCard title="로그인이 필요합니다" description={`${title}은 로그인 후 확인할 수 있습니다.`} />
      ) : (
        <div key={sessionVersion}>
          <Link to="/my" className="text-decoration-none small">← 내 정보</Link>
          <h1 className="h4 mt-3 mb-4">{title}</h1>
          {children}
        </div>
      )}
    </Container>
  );
}
