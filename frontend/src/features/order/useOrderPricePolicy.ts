import { useQuery } from "@tanstack/react-query";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { fetchOrderPricePolicy } from "@/features/order/api";

export function useOrderPricePolicy(enabled = true) {
  return useQuery({
    queryKey: ["orders", "price-policy"],
    queryFn: fetchOrderPricePolicy,
    staleTime: PUBLIC_DATA_STALE_TIME,
    enabled,
  });
}
