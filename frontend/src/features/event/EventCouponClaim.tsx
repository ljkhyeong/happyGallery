import { useQuery } from "@tanstack/react-query";
import { Button, Card } from "react-bootstrap";
import { CouponSummary } from "@/features/coupon/CouponSummary";
import { claimableCouponsQuery, myCouponsQuery, useClaimCoupon } from "@/features/coupon/queries";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { ErrorAlert, LinkButton, LoadingSpinner } from "@/shared/ui";

interface Props {
  eventId: number;
  couponDefinitionId: number;
}

export function EventCouponClaim({ eventId, couponDefinitionId }: Props) {
  const {
    error: authError,
    isAuthenticated,
    isLoading: authLoading,
    isRefreshing,
    refresh,
    status,
  } = useCustomerAuth();
  const couponsQuery = useQuery({ ...myCouponsQuery(), enabled: isAuthenticated });
  const claimableQuery = useQuery({ ...claimableCouponsQuery(), enabled: isAuthenticated });
  const claimMutation = useClaimCoupon();

  // 로그인 뒤 이벤트 쿠폰 칸으로 돌아온다.
  const loginHref = buildAuthPageHref("/login", {
    redirectTo: `/events/${eventId}#event-coupon-heading`,
  });
  const ownedCoupon = couponsQuery.data?.find(
    (coupon) => coupon.definitionId === couponDefinitionId,
  );
  const claimableCoupon = claimableQuery.data?.find(
    (coupon) => coupon.definitionId === couponDefinitionId,
  );

  return (
    <Card className="event-coupon-panel mt-5" as="section" aria-labelledby="event-coupon-heading">
      <Card.Body>
        <h2 id="event-coupon-heading" className="h4 display-heading mb-2">이벤트 쿠폰</h2>

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
            <LinkButton to={loginHref}>
              로그인하고 쿠폰 받기
            </LinkButton>
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
                <CouponSummary coupon={ownedCoupon} />
                <LinkButton variant="outline-dark" to="/my/benefits#owned-coupons">
                  받은 쿠폰 확인
                </LinkButton>
              </div>
            )}
            {!ownedCoupon && claimableCoupon && (
              <div className="d-flex flex-wrap justify-content-between align-items-center gap-3">
                <CouponSummary coupon={claimableCoupon} />
                <Button
                  type="button"
                  disabled={claimMutation.isPending}
                  onClick={() => claimMutation.mutate(couponDefinitionId)}
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
                  지금은 받을 수 없는 쿠폰입니다.
                </p>
              )}
          </>
        )}
      </Card.Body>
    </Card>
  );
}
