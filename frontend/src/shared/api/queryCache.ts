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

/** 후기 변경 뒤 공개 대상 후기, 회원 후기, 관리자 후기 목록을 함께 다시 읽는다. */
export async function invalidateReviewTarget(
  queryClient: QueryClient,
  target: { targetType: "PRODUCT" | "CLASS"; targetId: number },
): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({
      queryKey: queryKeys.reviews.byTarget(target.targetType, target.targetId),
    }),
    queryClient.invalidateQueries({ queryKey: queryKeys.member.reviews.all }),
    queryClient.invalidateQueries({ queryKey: queryKeys.admin.reviews.all }),
  ]);
}
