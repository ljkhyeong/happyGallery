import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router";
import { useDebouncedValue } from "@/shared/hooks/useDebouncedValue";
import { readProductFilters } from "./productFilters";

export function useProductListFilters() {
  const [searchParams, setSearchParams] = useSearchParams();
  const filters = useMemo(() => readProductFilters(searchParams), [searchParams]);
  const urlKeyword = filters.keyword ?? "";
  const [input, setInput] = useState({ urlKeyword, value: urlKeyword });
  // 뒤로 가기·링크 진입으로 URL이 바뀌면 진행 중인 입력도 해당 검색으로 맞춘다.
  if (input.urlKeyword !== urlKeyword) {
    setInput({ urlKeyword, value: urlKeyword });
  }
  const keyword = input.urlKeyword === urlKeyword ? input.value : urlKeyword;
  const debouncedKeyword = useDebouncedValue(keyword, 300);

  useEffect(() => {
    if (input.urlKeyword !== urlKeyword || input.value !== debouncedKeyword) return;
    const normalized = debouncedKeyword.trim().slice(0, 100);
    if (normalized === urlKeyword) return;
    setSearchParams((current) => {
      const next = new URLSearchParams(current);
      if (normalized) next.set("keyword", normalized);
      else next.delete("keyword");
      return next;
    }, { replace: true, preventScrollReset: true });
  }, [debouncedKeyword, input, setSearchParams, urlKeyword]);

  function updateFilter(name: "type" | "category" | "sort", value: string) {
    const next = new URLSearchParams(searchParams);
    if (value === "ALL" || value === "newest") next.delete(name);
    else next.set(name, value);
    const normalized = keyword.trim().slice(0, 100);
    if (normalized) next.set("keyword", normalized);
    else next.delete("keyword");
    setSearchParams(next, { replace: true, preventScrollReset: true });
  }

  function resetFilters() {
    setInput({ urlKeyword, value: "" });
    const next = new URLSearchParams(searchParams);
    for (const name of ["keyword", "type", "category", "sort"]) next.delete(name);
    setSearchParams(next, { replace: true, preventScrollReset: true });
  }

  return {
    filters,
    keyword,
    setKeyword: (value: string) => setInput({ urlKeyword, value }),
    updateFilter,
    resetFilters,
  };
}
