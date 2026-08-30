import type { QueryClient } from "@tanstack/react-query";
import { isCustomerQueryKey, queryKeys } from "./queryKeys";

export function clearAdminQueryCache(queryClient: QueryClient): void {
  void queryClient.cancelQueries({ queryKey: queryKeys.admin.all });
  queryClient.removeQueries({ queryKey: queryKeys.admin.all });
}

export function clearCustomerQueryCache(queryClient: QueryClient): void {
  const predicate = ({ queryKey }: { queryKey: readonly unknown[] }) =>
    isCustomerQueryKey(queryKey);

  void queryClient.cancelQueries({ predicate });
  queryClient.removeQueries({ predicate });
}

export async function invalidateSlotAvailability(queryClient: QueryClient): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({
      queryKey: queryKeys.slotAvailability.upcoming.all,
    }),
    queryClient.invalidateQueries({
      queryKey: queryKeys.slotAvailability.reschedule.all,
    }),
  ]);
}
