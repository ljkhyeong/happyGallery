import { useQuery } from "@tanstack/react-query";
import { Button, Card, Col, Row } from "react-bootstrap";
import { Link } from "react-router";
import type { MyCouponResponse } from "@/features/coupon/api";
import { couponConditionLabel, couponDiscountLabel } from "@/features/coupon/presentation";
import { claimableCouponsQuery, myCouponsQuery, useClaimCoupon } from "@/features/coupon/queries";
import { MySectionPage } from "@/features/my/MySectionPage";
import {
  fetchMyRewardWallet,
  type RewardHistoryResponse,
} from "@/features/reward/api";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { useHashScroll } from "@/shared/hooks/useHashScroll";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner, ToneBadge, type CustomerBadgeTone } from "@/shared/ui";

interface CouponStatusBadge {
  label: string;
  tone: CustomerBadgeTone;
}

// 쓸 수 있거나 결제에 묶인 쿠폰은 먹색, 끝난 쿠폰(사용 완료·만료·중지)은 회색 면이다.
const COUPON_STATUS: Record<MyCouponResponse["status"], CouponStatusBadge> = {
  AVAILABLE: { label: "사용 가능", tone: "progress" },
  RESERVED: { label: "결제 처리 중", tone: "progress" },
  REDEEMED: { label: "사용 완료", tone: "done" },
  EXPIRED: { label: "기간 만료", tone: "done" },
  CANCELED: { label: "사용 중지", tone: "done" },
};
const UNKNOWN_COUPON_STATUS: CouponStatusBadge = { label: "상태 확인 필요", tone: "done" };

// 금액 부호: 사용 가능 적립금이 늘어나는 이력(적립·대기 해제·복원)은 +, 쓰거나 줄어드는 이력(사용 대기·사용·만료·적립 취소)은 -다.
const REWARD_HISTORY: Record<RewardHistoryResponse["type"], { label: string; sign: "+" | "-" }> = {
  EARN: { label: "적립", sign: "+" },
  RESERVE: { label: "결제 사용 대기", sign: "-" },
  RELEASE: { label: "결제 대기 해제", sign: "+" },
  USE: { label: "결제 사용", sign: "-" },
  RESTORE: { label: "사용 적립금 복원", sign: "+" },
  EXPIRE: { label: "유효기간 만료", sign: "-" },
  REVOKE: { label: "적립 취소", sign: "-" },
};

const WALLET_BALANCES = [
  { key: "availableBalance", label: "사용 가능" },
  { key: "reservedBalance", label: "결제 처리 중" },
  { key: "debtBalance", label: "회수 예정" },
] as const;

export function MyBenefitsPage() {
  const couponsQuery = useQuery(myCouponsQuery());
  const claimableQuery = useQuery(claimableCouponsQuery());
  const rewardsQuery = useQuery({
    queryKey: queryKeys.member.rewards,
    queryFn: ({ signal }) => runForCurrentCustomer(() => fetchMyRewardWallet(signal)),
  });
  const claimMutation = useClaimCoupon();
  // 이벤트의 "받은 쿠폰 확인"은 #owned-coupons로 보유 쿠폰 칸을 연다.
  useHashScroll("owned-coupons");

  const coupons = couponsQuery.data ?? [];
  const availableCoupons = coupons.filter((coupon) => coupon.status === "AVAILABLE");
  const wallet = rewardsQuery.data;

  return (
    <MySectionPage
      path="/my/benefits"
    >
      <section className="mb-5" aria-labelledby="reward-wallet-heading">
        <div className="d-flex flex-wrap justify-content-between align-items-end gap-1 gap-sm-3 mb-3">
          <h2 id="reward-wallet-heading" className="h5 display-heading mb-0">적립금</h2>
          <small className="text-muted-soft">1원 단위로 상품 금액 내에서 사용</small>
        </div>
        {rewardsQuery.isLoading && <LoadingSpinner text="적립금 조회 중..." />}
        <ErrorAlert
          error={rewardsQuery.error}
          onRetry={() => { void rewardsQuery.refetch(); }}
          retrying={rewardsQuery.isFetching}
        />
        {wallet && (
          <>
            {/* 390px에서도 세 숫자를 한 줄에 두어 비교해 읽게 한다. */}
            <Row className="g-2 g-sm-3 mb-3">
              {WALLET_BALANCES.map(({ key, label }) => (
                <Col key={key} xs={4}>
                  <Card className="my-stat-card h-100">
                    <Card.Body>
                      <div className="text-muted-soft small mb-1">{label}</div>
                      <strong className="my-stat-card-value">{formatKRW(wallet[key])}</strong>
                    </Card.Body>
                  </Card>
                </Col>
              ))}
            </Row>

            <h3 className="h6 mt-4 mb-2">최근 적립금 이력</h3>
            {wallet.history.length === 0 && <EmptyState message="적립금 이력이 없습니다." />}
            {wallet.history.map((history: RewardHistoryResponse) => {
              const kind = REWARD_HISTORY[history.type] ?? { label: "적립금 변경", sign: "" };
              return (
                <Card key={history.id} className="my-list-card mb-2">
                  <Card.Body className="my-list-row py-3">
                    <div className="my-list-main">
                      <strong className="small">{kind.label}</strong>
                      <div className="text-muted-soft small">
                        {formatDateTime(history.createdAt)}
                        {history.orderId !== null && (
                          <> · <Link to={`/my/orders/${history.orderId}`} className="text-nowrap">주문 #{history.orderId}</Link></>
                        )}
                      </div>
                    </div>
                    <div className="my-list-side">
                      <strong className="my-reward-amount">{kind.sign}{formatKRW(history.amount)}</strong>
                      <small className="text-muted-soft">
                        사용 가능 잔액 {formatKRW(history.availableAfter)}
                      </small>
                    </div>
                  </Card.Body>
                </Card>
              );
            })}
          </>
        )}
      </section>

      <section className="mb-5" aria-labelledby="claimable-coupons-heading">
        <h2 id="claimable-coupons-heading" className="h5 display-heading mb-3">받을 수 있는 쿠폰</h2>
        <ErrorAlert error={claimMutation.error} />
        {claimableQuery.isLoading && <LoadingSpinner text="발급 가능 쿠폰 조회 중..." />}
        <ErrorAlert
          error={claimableQuery.error}
          onRetry={() => { void claimableQuery.refetch(); }}
          retrying={claimableQuery.isFetching}
        />
        {claimableQuery.data?.length === 0 && (
          <EmptyState message="현재 새로 받을 수 있는 쿠폰이 없습니다." />
        )}
        {claimableQuery.data?.map((coupon) => (
          <Card key={coupon.definitionId} className="my-list-card mb-2">
            <Card.Body className="d-flex flex-wrap justify-content-between align-items-center gap-3">
              <div>
                <strong>{coupon.name}</strong>
                <div className="small mt-1">{couponDiscountLabel(coupon)}</div>
                <small className="text-muted-soft">{couponConditionLabel(coupon)}</small>
              </div>
              <Button
                type="button"
                size="sm"
                disabled={claimMutation.isPending}
                onClick={() => claimMutation.mutate(coupon.definitionId)}
              >
                쿠폰 받기
              </Button>
            </Card.Body>
          </Card>
        ))}
      </section>

      <section id="owned-coupons" className="my-anchor-section" aria-labelledby="owned-coupons-heading">
        <div className="d-flex flex-wrap justify-content-between gap-2 align-items-end mb-3">
          <div>
            <h2 id="owned-coupons-heading" className="h5 display-heading mb-0">보유 쿠폰</h2>
          </div>
          <span className="text-muted-soft small">사용 가능 {availableCoupons.length}장 · 조회 {coupons.length}장</span>
        </div>
        <p className="small text-muted-soft">사용 가능·결제 처리 중 쿠폰과 최근 발급 이력 100장을 표시합니다.</p>
        {couponsQuery.isLoading && <LoadingSpinner text="보유 쿠폰 조회 중..." />}
        <ErrorAlert
          error={couponsQuery.error}
          onRetry={() => { void couponsQuery.refetch(); }}
          retrying={couponsQuery.isFetching}
        />
        {couponsQuery.data?.length === 0 && <EmptyState message="보유한 쿠폰이 없습니다." />}
        {couponsQuery.data?.map((coupon) => {
          const status = COUPON_STATUS[coupon.status] ?? UNKNOWN_COUPON_STATUS;
          return (
            <Card key={coupon.id} className="my-list-card mb-2">
              {/* 쿠폰 번호는 이름 길이와 상관없이 오른쪽 위 같은 자리에 둔다. */}
              <Card.Body className="my-list-row">
                <div className="my-list-main">
                  <div className="d-flex flex-wrap gap-2 align-items-center mb-1">
                    <strong>{coupon.name}</strong>
                    <ToneBadge tone={status.tone}>{status.label}</ToneBadge>
                  </div>
                  <div className="small">{couponDiscountLabel(coupon)}</div>
                  <small className="text-muted-soft">{couponConditionLabel(coupon)}</small>
                </div>
                <small className="my-list-side text-muted-soft">쿠폰 #{coupon.id}</small>
              </Card.Body>
            </Card>
          );
        })}
      </section>
    </MySectionPage>
  );
}
