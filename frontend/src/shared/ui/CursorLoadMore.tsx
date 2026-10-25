import { Button, type ButtonProps } from "react-bootstrap";
import type { UseInfiniteQueryResult } from "@tanstack/react-query";
import { ErrorAlert } from "./ErrorAlert";

type CursorQueryState = Pick<
  UseInfiniteQueryResult<unknown, unknown>,
  "error" | "fetchNextPage" | "hasNextPage" | "isFetchNextPageError" | "isFetchingNextPage"
>;

interface Props {
  query: CursorQueryState;
  label: string;
  loadingLabel?: string;
  variant?: ButtonProps["variant"];
  size?: ButtonProps["size"];
  className?: string;
  /** 버튼을 감싸는 div 클래스. null이면 감싸지 않는다. */
  wrapperClassName?: string | null;
  /** 기본은 다음 페이지를 받는 동안만 막는다. */
  disabled?: boolean;
  retryLabel?: string;
  retrying?: boolean;
  /** 다음 페이지 조회가 실패하면 재시도 안내만 남기고 더 보기 버튼을 숨긴다. */
  hideOnNextPageError?: boolean;
}

/**
 * 커서 목록 아래의 "더 보기" 버튼과 다음 페이지 조회 실패 안내.
 * 다음 페이지 실패는 목록 전체를 다시 받지 않고 실패한 페이지부터 이어서 조회한다.
 * 목록 위의 첫 페이지·재조회 실패 안내는 CursorQueryErrorAlert가 맡는다.
 */
export function CursorLoadMore({
  query,
  label,
  loadingLabel = "불러오는 중...",
  variant = "outline-primary",
  size,
  className,
  wrapperClassName = "d-grid mt-3",
  disabled = query.isFetchingNextPage,
  retryLabel,
  retrying = query.isFetchingNextPage,
  hideOnNextPageError = false,
}: Props) {
  const loadMore = () => { void query.fetchNextPage(); };
  const button = (
    <Button type="button" variant={variant} size={size} className={className} disabled={disabled} onClick={loadMore}>
      {query.isFetchingNextPage ? loadingLabel : label}
    </Button>
  );
  const showButton = query.hasNextPage && !(hideOnNextPageError && query.isFetchNextPageError);
  return (
    <>
      {query.isFetchNextPageError && (
        <ErrorAlert error={query.error} onRetry={loadMore} retrying={retrying} retryLabel={retryLabel} />
      )}
      {showButton && (wrapperClassName === null ? button : <div className={wrapperClassName}>{button}</div>)}
    </>
  );
}

interface ErrorAlertProps {
  query: Pick<
    UseInfiniteQueryResult<unknown, unknown>,
    "error" | "isFetchNextPageError" | "isFetching" | "isFetchingNextPage" | "refetch"
  >;
  /** 기본은 첫 페이지를 다시 받는 동안만 재시도를 막는다. */
  retrying?: boolean;
  retryLabel?: string;
}

/**
 * 커서 목록 위의 첫 페이지·재조회 실패 안내.
 * 다음 페이지 실패는 CursorLoadMore가 목록 아래에서 이어서 조회하므로 여기서는 숨긴다.
 */
export function CursorQueryErrorAlert({
  query,
  retrying = query.isFetching && !query.isFetchingNextPage,
  retryLabel,
}: ErrorAlertProps) {
  return (
    <ErrorAlert
      error={query.isFetchNextPageError ? null : query.error}
      onRetry={() => { void query.refetch(); }}
      retrying={retrying}
      retryLabel={retryLabel}
    />
  );
}
