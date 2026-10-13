import { useEffect, useRef } from "react";
import { useSearchParams } from "react-router";

interface MyListFilterUpdate {
  q?: string;
  status?: string;
  sort?: string;
}

interface UseMyListFiltersOptions {
  defaultSort: string;
  statusValues: readonly string[];
  sortValues: readonly string[];
}

export interface MyListFilters {
  searchQuery: string;
  statusFilter: string;
  sortValue: string;
  defaultSort: string;
  updateFilters: (next: MyListFilterUpdate) => void;
  resetFilters: () => void;
}

export function useMyListFilters({
  defaultSort,
  statusValues,
  sortValues,
}: UseMyListFiltersOptions): MyListFilters {
  const [searchParams, setSearchParams] = useSearchParams();
  const pendingSearchParams = useRef(searchParams);
  useEffect(() => {
    pendingSearchParams.current = searchParams;
  }, [searchParams]);

  // 주소에 모르는 상태·정렬 값이 있으면 전체·기본 정렬로 읽는다.
  const readFilters = (params: URLSearchParams) => {
    const requestedStatus = params.get("status") ?? "ALL";
    const requestedSort = params.get("sort") ?? defaultSort;
    return {
      searchQuery: params.get("q") ?? "",
      statusFilter: statusValues.includes(requestedStatus) ? requestedStatus : "ALL",
      sortValue: sortValues.includes(requestedSort) ? requestedSort : defaultSort,
    };
  };
  const { searchQuery, statusFilter, sortValue } = readFilters(searchParams);

  function updateFilters(next: MyListFilterUpdate) {
    // 라우터 반영 전의 연속 입력도 직전에 요청한 필터에 합친다.
    const current = readFilters(pendingSearchParams.current);
    const nextSearchParams = new URLSearchParams(pendingSearchParams.current);
    const nextQuery = next.q ?? current.searchQuery;
    const nextStatus = next.status ?? current.statusFilter;
    const nextSort = next.sort ?? current.sortValue;

    if (nextQuery) nextSearchParams.set("q", nextQuery);
    else nextSearchParams.delete("q");

    if (nextStatus !== "ALL") nextSearchParams.set("status", nextStatus);
    else nextSearchParams.delete("status");

    if (nextSort !== defaultSort) nextSearchParams.set("sort", nextSort);
    else nextSearchParams.delete("sort");

    pendingSearchParams.current = nextSearchParams;
    setSearchParams(nextSearchParams, { replace: true });
  }

  function resetFilters() {
    pendingSearchParams.current = new URLSearchParams();
    setSearchParams(pendingSearchParams.current, { replace: true });
  }

  return {
    searchQuery,
    statusFilter,
    sortValue,
    defaultSort,
    updateFilters,
    resetFilters,
  };
}
