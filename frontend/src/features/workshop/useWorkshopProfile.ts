import { useQuery } from "@tanstack/react-query";
import { fetchWorkshopProfile } from "./api";
import { REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";

export function useWorkshopProfile() {
  return useQuery({
    queryKey: ["workshop-profile"],
    queryFn: fetchWorkshopProfile,
    staleTime: REFERENCE_DATA_STALE_TIME,
  });
}
