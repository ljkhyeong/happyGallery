import { Button } from "react-bootstrap";
import { isRouteErrorResponse, useRouteError } from "react-router";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { StatusPage } from "@/shared/ui/StatusPage";

export function PublicRouteErrorBoundary() {
  const error = useRouteError();

  if (isRouteErrorResponse(error) && error.status === 404) {
    return <NotFoundPage />;
  }

  return (
    <StatusPage
      kicker="ERROR"
      title="페이지를 불러오지 못했습니다"
      description="잠시 후 다시 시도해 주세요. 계속 열리지 않으면 공방에 문의해 주세요."
      actions={<Button onClick={() => window.location.reload()}>다시 시도</Button>}
    />
  );
}
