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
  const searchQuery = searchParams.get("q") ?? "";
  const requestedStatus =
    searchParams.get("status") ??
    (legacyStatusParam ? searchParams.get(legacyStatusParam) : null) ??
    "ALL";
  const requestedSort = searchParams.get("sort") ?? defaultSort;
  const statusFilter = !statusValues || statusValues.includes(requestedStatus) ? requestedStatus : "ALL";
  const sortValue = !sortValues || sortValues.includes(requestedSort) ? requestedSort : defaultSort;

  function updateFilters(next: MyListFilterUpdate) {
    const nextSearchParams = new URLSearchParams(searchParams);
    const nextQuery = next.q ?? searchQuery;
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
