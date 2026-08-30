import { Container } from "react-bootstrap";
import { isRouteErrorResponse, useRouteError } from "react-router";
import { NotFoundPage } from "@/pages/NotFoundPage";

export function PublicRouteErrorBoundary() {
  const error = useRouteError();

  if (isRouteErrorResponse(error) && error.status === 404) {
    return <NotFoundPage />;
  }

  return (
    <Container className="page-container text-center py-5">
      <h1 className="h3 mb-3">페이지를 불러오지 못했습니다</h1>
      <p className="text-muted-soft mb-0">잠시 후 다시 시도해 주세요.</p>
    </Container>
  );
}
