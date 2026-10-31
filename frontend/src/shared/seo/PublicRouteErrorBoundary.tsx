import { useRouteError } from "react-router";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { isNotFoundRouteError, RouteLoadErrorStatus } from "@/shared/ui/RouteLoadErrorStatus";

export function PublicRouteErrorBoundary() {
  const error = useRouteError();
  return isNotFoundRouteError(error) ? <NotFoundPage /> : <RouteLoadErrorStatus />;
}
