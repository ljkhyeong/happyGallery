import { fetchWorkshopProfile } from "./api";
import { queryKeys, useLoaderBackedQuery } from "@/shared/api";
import { REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import type { WorkshopProfile } from "@/shared/types";

export function useWorkshopProfile(initialData?: WorkshopProfile) {
  return useLoaderBackedQuery({
    queryKey: queryKeys.workshop,
    queryFn: fetchWorkshopProfile,
    staleTime: REFERENCE_DATA_STALE_TIME,
    enabled: typeof window !== "undefined" || initialData !== undefined,
  }, initialData);
}
