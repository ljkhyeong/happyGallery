import { useSearchParams } from "react-router-dom";

interface MyListFilterUpdate {
  q?: string;
  status?: string;
  sort?: string;
}

interface UseMyListFiltersOptions {
  defaultSort: string;
  legacyStatusParam?: string;
}

export function useMyListFilters({
  defaultSort,
  legacyStatusParam,
}: UseMyListFiltersOptions) {
  const [searchParams, setSearchParams] = useSearchParams();
  const searchQuery = searchParams.get("q") ?? "";
  const statusFilter =
    searchParams.get("status") ??
    (legacyStatusParam ? searchParams.get(legacyStatusParam) : null) ??
    "ALL";
  const sortValue = searchParams.get("sort") ?? defaultSort;

  function updateFilters(next: MyListFilterUpdate) {
    const nextSearchParams = new URLSearchParams(searchParams);
    const nextQuery = (next.q ?? searchQuery).trim();
    const nextStatus = next.status ?? statusFilter;
    const nextSort = next.sort ?? sortValue;

    if (nextQuery) nextSearchParams.set("q", nextQuery);
    else nextSearchParams.delete("q");

    if (nextStatus !== "ALL") nextSearchParams.set("status", nextStatus);
    else nextSearchParams.delete("status");
    if (legacyStatusParam) nextSearchParams.delete(legacyStatusParam);

    if (nextSort !== defaultSort) nextSearchParams.set("sort", nextSort);
    else nextSearchParams.delete("sort");

    setSearchParams(nextSearchParams, { replace: true });
  }

  function resetFilters() {
    setSearchParams({}, { replace: true });
  }

  return {
    searchQuery,
    statusFilter,
    sortValue,
    updateFilters,
    resetFilters,
  };
}
