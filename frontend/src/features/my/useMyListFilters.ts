import { useEffect, useRef } from "react";
import { useSearchParams } from "react-router";

interface MyListFilterUpdate {
  q?: string;
  status?: string;
  sort?: string;
}

interface UseMyListFiltersOptions {
  defaultSort: string;
  legacyStatusParam?: string;
  statusValues?: readonly string[];
  sortValues?: readonly string[];
}

export function useMyListFilters({
  defaultSort,
  legacyStatusParam,
  statusValues,
  sortValues,
}: UseMyListFiltersOptions) {
  const [searchParams, setSearchParams] = useSearchParams();
  const pendingSearchParams = useRef(searchParams);
  useEffect(() => {
    pendingSearchParams.current = searchParams;
  }, [searchParams]);
  const searchQuery = searchParams.get("q") ?? "";
  const requestedStatus =
    searchParams.get("status") ??
    (legacyStatusParam ? searchParams.get(legacyStatusParam) : null) ??
    "ALL";
  const requestedSort = searchParams.get("sort") ?? defaultSort;
  const statusFilter = !statusValues || statusValues.includes(requestedStatus) ? requestedStatus : "ALL";
  const sortValue = !sortValues || sortValues.includes(requestedSort) ? requestedSort : defaultSort;

  function updateFilters(next: MyListFilterUpdate) {
    // 라우터 반영 전의 연속 입력도 직전에 요청한 필터에 합친다.
    const currentSearchParams = pendingSearchParams.current;
    const currentQuery = currentSearchParams.get("q") ?? "";
    const requestedCurrentStatus =
      currentSearchParams.get("status") ??
      (legacyStatusParam ? currentSearchParams.get(legacyStatusParam) : null) ??
      "ALL";
    const currentStatus = !statusValues || statusValues.includes(requestedCurrentStatus)
      ? requestedCurrentStatus
      : "ALL";
    const currentSortValue = currentSearchParams.get("sort") ?? defaultSort;
    const nextSortValue = !sortValues || sortValues.includes(currentSortValue)
      ? currentSortValue
      : defaultSort;
    const nextSearchParams = new URLSearchParams(currentSearchParams);
    const nextQuery = next.q ?? currentQuery;
    const nextStatus = next.status ?? currentStatus;
    const nextSort = next.sort ?? nextSortValue;

    if (nextQuery) nextSearchParams.set("q", nextQuery);
    else nextSearchParams.delete("q");

    if (nextStatus !== "ALL") nextSearchParams.set("status", nextStatus);
    else nextSearchParams.delete("status");
    if (legacyStatusParam) nextSearchParams.delete(legacyStatusParam);

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
    updateFilters,
    resetFilters,
  };
}
