import { fetchWorkshopProfile } from "./api";
import { queryKeys, useLoaderBackedQuery } from "@/shared/api";
import { REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import type {
  LoaderBackedQueryResult,
  RequiredLoaderBackedQueryResult,
} from "@/shared/api/useLoaderBackedQuery";
import type { WorkshopProfile } from "@/shared/types";

/** 공개 route loader가 공방 정보를 넘기면 첫 화면부터 값이 있고, 아니면 화면에서 조회한다. */
export function useWorkshopProfile(
  initialData: WorkshopProfile,
): RequiredLoaderBackedQueryResult<WorkshopProfile, Error>;
export function useWorkshopProfile(
  initialData?: WorkshopProfile,
): LoaderBackedQueryResult<WorkshopProfile, Error>;
export function useWorkshopProfile(
  initialData?: WorkshopProfile,
): LoaderBackedQueryResult<WorkshopProfile, Error> | RequiredLoaderBackedQueryResult<WorkshopProfile, Error> {
  return useLoaderBackedQuery({
    queryKey: queryKeys.workshop,
    queryFn: fetchWorkshopProfile,
    staleTime: REFERENCE_DATA_STALE_TIME,
    enabled: typeof window !== "undefined" || initialData !== undefined,
  }, initialData);
}
