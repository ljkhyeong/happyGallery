import { Container } from "react-bootstrap";
import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <Container className="page-container text-center py-5">
      <h1 className="display-6 mb-3">404</h1>
      <p className="text-muted-soft mb-4">요청하신 페이지를 찾을 수 없습니다.</p>
      <Link to="/" className="btn btn-primary">
        홈으로 돌아가기
      </Link>
    </Container>
  );
}
