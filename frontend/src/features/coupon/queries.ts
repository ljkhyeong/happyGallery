import { useMutation, useQueryClient } from "@tanstack/react-query";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { useToast } from "@/shared/ui";
import { claimCoupon, fetchClaimableCoupons, fetchMyCoupons } from "./api";

export function myCouponsQuery() {
  return {
    queryKey: queryKeys.member.coupons,
    queryFn: ({ signal }: { signal: AbortSignal }) => fetchMyCoupons(signal),
  };
}

export function claimableCouponsQuery() {
  return {
    queryKey: queryKeys.member.claimableCoupons,
    queryFn: ({ signal }: { signal: AbortSignal }) => fetchClaimableCoupons(signal),
  };
}

/** 쿠폰을 받고 보유·발급 가능 목록을 함께 갱신한다. 발급 가능 key가 보유 key 아래라 한 번에 무효화된다. */
export function useClaimCoupon() {
  const queryClient = useQueryClient();
  const toast = useToast();
  return useMutation({
    mutationFn: (definitionId: number) => runForCurrentCustomer(
      () => claimCoupon(definitionId),
      async (claimed, requireCurrent) => {
        await queryClient.invalidateQueries({ queryKey: queryKeys.member.coupons });
        requireCurrent();
        toast.show(`${claimed.name} 쿠폰을 받았습니다.`, "success");
      },
    ),
  });
}
