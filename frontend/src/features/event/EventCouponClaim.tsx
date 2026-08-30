import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Card } from "react-bootstrap";
import { Link } from "react-router";
import {
  claimCoupon,
  fetchClaimableCoupons,
  fetchMyCoupons,
} from "@/features/coupon/api";
import { couponDiscountLabel } from "@/features/coupon/presentation";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";

interface Props {
  eventId: number;
  couponDefinitionId: number;
}

export function EventCouponClaim({ eventId, couponDefinitionId }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const {
    error: authError,
    isAuthenticated,
    isLoading: authLoading,
    isRefreshing,
    refresh,
    status,
  } = useCustomerAuth();
  const couponsQuery = useQuery({
    queryKey: queryKeys.member.coupons,
    queryFn: ({ signal }) => fetchMyCoupons(signal),
    enabled: isAuthenticated,
  });
  const claimableQuery = useQuery({
    queryKey: queryKeys.member.claimableCoupons,
    queryFn: ({ signal }) => fetchClaimableCoupons(signal),
    enabled: isAuthenticated,
  });
  const claimMutation = useMutation({
    mutationFn: () => runForCurrentCustomer(
      () => claimCoupon(couponDefinitionId),
      async (claimed, requireCurrent) => {
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: queryKeys.member.coupons }),
          queryClient.invalidateQueries({ queryKey: queryKeys.member.claimableCoupons }),
        ]);
        requireCurrent();
        toast.show(`${claimed.name} 쿠폰을 받았습니다.`, "success");
      },
    ),
  });

  const loginHref = buildAuthPageHref("/login", {
    redirectTo: `/events/${eventId}`,
  });
  const ownedCoupon = couponsQuery.data?.find(
    (coupon) => coupon.definitionId === couponDefinitionId,
  );
  const claimableCoupon = claimableQuery.data?.find(
    (coupon) => coupon.definitionId === couponDefinitionId,
  );

  return (
    <Card className="event-coupon-panel mt-5 border-0" as="section" aria-labelledby="event-coupon-heading">
      <Card.Body>
        <div className="event-coupon-kicker">Event Benefit</div>
        <h2 id="event-coupon-heading" className="h4 mb-2">이벤트 쿠폰</h2>

        {status === "error" && (
          <ErrorAlert
            error={authError}
            onRetry={() => { void refresh(); }}
            retrying={isRefreshing}
          />
        )}
        {status !== "error" && authLoading && (
          <LoadingSpinner text="쿠폰 정보를 확인하는 중입니다" />
        )}
        {!authLoading && !isAuthenticated && (
          <div className="d-flex flex-wrap justify-content-between align-items-center gap-3">
            <p className="mb-0">
              로그인하면 이 이벤트에 연결된 쿠폰을 받을 수 있습니다.
            </p>
            <Link className="btn btn-primary" to={loginHref}>
              로그인하고 쿠폰 받기
            </Link>
          </div>
        )}
        {isAuthenticated && (
          <>
            <ErrorAlert error={couponsQuery.error ?? claimableQuery.error ?? claimMutation.error} />
            {(couponsQuery.isLoading || claimableQuery.isLoading) && (
              <LoadingSpinner text="받을 수 있는 쿠폰을 확인하는 중입니다" />
            )}
            {ownedCoupon && (
              <div className="d-flex flex-wrap justify-content-between align-items-center gap-3">
                <div>
                  <strong className="d-block mb-1">{ownedCoupon.name}</strong>
                  <div>{couponDiscountLabel(ownedCoupon)}</div>
                  <small className="text-muted-soft">
                    {ownedCoupon.minOrderAmount > 0
                      ? `${formatKRW(ownedCoupon.minOrderAmount)} 이상 · `
                      : ""}
                    {formatDateTime(ownedCoupon.validUntil)}까지
                  </small>
                </div>
                <Link className="btn btn-outline-dark" to="/my/benefits">
                  받은 쿠폰 확인
                </Link>
              </div>
            )}
            {!ownedCoupon && claimableCoupon && (
              <div className="d-flex flex-wrap justify-content-between align-items-center gap-3">
                <div>
                  <strong className="d-block mb-1">{claimableCoupon.name}</strong>
                  <div>{couponDiscountLabel(claimableCoupon)}</div>
                  <small className="text-muted-soft">
                    {claimableCoupon.minOrderAmount > 0
                      ? `${formatKRW(claimableCoupon.minOrderAmount)} 이상 · `
                      : ""}
                    {formatDateTime(claimableCoupon.validUntil)}까지
                  </small>
                </div>
                <Button
                  type="button"
                  disabled={claimMutation.isPending}
                  onClick={() => claimMutation.mutate()}
                >
                  {claimMutation.isPending ? "쿠폰 받는 중..." : "쿠폰 받기"}
                </Button>
              </div>
            )}
            {!couponsQuery.isLoading
              && !claimableQuery.isLoading
              && !couponsQuery.error
              && !claimableQuery.error
              && !ownedCoupon
              && !claimableCoupon && (
                <p className="mb-0 text-muted-soft">
                  현재 받을 수 없는 쿠폰입니다. 발급 기간과 쿠폰 상태를 확인해 주세요.
                </p>
              )}
          </>
        )}
      </Card.Body>
    </Card>
  );
}
