import { useQuery } from "@tanstack/react-query";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { fetchOrderPricePolicy } from "./api";
import { queryKeys } from "@/shared/api";

export function useOrderPricePolicy(enabled = true) {
  return useQuery({
    queryKey: queryKeys.policies.orderPrice,
    queryFn: fetchOrderPricePolicy,
    staleTime: PUBLIC_DATA_STALE_TIME,
    enabled,
  });
}
