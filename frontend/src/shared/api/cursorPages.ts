import type { InfiniteData } from "@tanstack/react-query";

interface CursorPageLike {
  hasMore: boolean;
  nextCursor: string | null;
}

/** 커서 페이지 useInfiniteQuery의 시작 커서와 다음 커서 계산. 마지막 페이지면 더 조회하지 않는다. */
export const cursorPageParams = {
  initialPageParam: undefined as string | undefined,
  getNextPageParam: (lastPage: CursorPageLike): string | undefined =>
    lastPage.hasMore ? lastPage.nextCursor ?? undefined : undefined,
};

/** 불러온 커서 페이지들의 항목을 순서대로 이어 붙인다. */
export function flattenCursorPages<TPage extends { content: unknown[] }>(
  data: InfiniteData<TPage> | undefined,
): TPage["content"] {
  return (data?.pages.flatMap((page) => page.content) ?? []) as TPage["content"];
}
