import type { Key, ReactNode } from "react";
import { Link } from "react-router";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";

const PREVIEW_SIZE = 3;

interface Props<T> {
  id: string;
  title: string;
  /** '전체 보기'가 여는 목록 경로 */
  listTo: string;
  emptyMessage: string;
  items: T[] | undefined;
  isLoading: boolean;
  error: Error | null;
  isFetching: boolean;
  onRetry: () => void;
  itemKey: (item: T) => Key;
  itemTo: (item: T) => string;
  renderMain: (item: T) => ReactNode;
  renderSide: (item: T) => ReactNode;
}

/** 내 정보 홈의 '최근 N건' 구역. 머리·로딩·오류·빈 목록과 카드 틀을 맡고, 카드 안 내용은 호출부가 그린다. */
export function MyRecentSection<T>({
  id,
  title,
  listTo,
  emptyMessage,
  items,
  isLoading,
  error,
  isFetching,
  onRetry,
  itemKey,
  itemTo,
  renderMain,
  renderSide,
}: Props<T>) {
  return (
    <section id={id} className="my-recent" aria-labelledby={`${id}-title`}>
      <div className="my-recent-head">
        <h2 id={`${id}-title`}>{title}</h2>
        <Link to={listTo}>전체 보기</Link>
      </div>
      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} onRetry={onRetry} retrying={isFetching} />
      {items && items.length === 0 && <EmptyState message={emptyMessage} />}
      {items?.slice(0, PREVIEW_SIZE).map((item) => (
        <Link key={itemKey(item)} to={itemTo(item)} className="my-list-card my-recent-item">
          <div className="my-recent-main">{renderMain(item)}</div>
          <div className="my-recent-side">{renderSide(item)}</div>
        </Link>
      ))}
    </section>
  );
}
