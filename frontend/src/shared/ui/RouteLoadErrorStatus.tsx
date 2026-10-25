import { Button } from "react-bootstrap";
import { isRouteErrorResponse } from "react-router";
import { StatusPage } from "./StatusPage";

interface Props {
  /** 머리글 없이 그려지는 루트 오류에서 공방 이름 홈 링크를 함께 보여 준다. */
  standalone?: boolean;
}

/**
 * loader·렌더 오류로 라우트를 그릴 수 없을 때의 안내. 공개 라우트와 루트 ErrorBoundary가 함께 쓴다.
 * 제목 "페이지를 불러오지 못했습니다"는 E2E가 선택자로 쓴다.
 */
export function RouteLoadErrorStatus({ standalone = false }: Props) {
  return (
    <StatusPage
      standalone={standalone}
      title="페이지를 불러오지 못했습니다"
      description="잠시 후 다시 시도해 주세요. 계속 열리지 않으면 공방에 문의해 주세요."
      actions={<Button onClick={() => window.location.reload()}>다시 시도</Button>}
    />
  );
}

/** loader가 404 Response를 던진 라우트 오류인지 판정한다. */
export function isNotFoundRouteError(error: unknown): boolean {
  return isRouteErrorResponse(error) && error.status === 404;
}
