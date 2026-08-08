import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Badge, Button, Card, Col, Container, Row } from "react-bootstrap";
import { Link } from "react-router";
import {
  claimCoupon,
  fetchClaimableCoupons,
  fetchMyCoupons,
  type ClaimableCouponResponse,
  type MyCouponResponse,
} from "@/features/coupon/api";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { MyAuthGateCard } from "@/features/my/MyAuthGateCard";
import {
  fetchMyRewardWallet,
  type RewardHistoryResponse,
} from "@/features/reward/api";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";

const COUPON_STATUS: Record<MyCouponResponse["status"], { label: string; bg: string }> = {
  AVAILABLE: { label: "사용 가능", bg: "success" },
  RESERVED: { label: "결제 처리 중", bg: "warning" },
  REDEEMED: { label: "사용 완료", bg: "secondary" },
  EXPIRED: { label: "기간 만료", bg: "secondary" },
  CANCELED: { label: "취소됨", bg: "secondary" },
};

const REWARD_HISTORY_LABEL: Record<RewardHistoryResponse["type"], string> = {
  EARN: "적립",
  RESERVE: "결제 사용 예약",
  RELEASE: "결제 예약 해제",
  USE: "주문 사용",
  RESTORE: "주문 취소 복원",
  EXPIRE: "유효기간 만료",
  REVOKE: "적립 취소",
  ADJUST: "관리자 조정",
};

function discountLabel(coupon: MyCouponResponse | ClaimableCouponResponse): string {
  if (coupon.discountType === "FIXED") return `${formatKRW(coupon.discountValue)} 할인`;
  return `${coupon.discountValue}% 할인 · 최대 ${formatKRW(coupon.maxDiscountAmount ?? 0)}`;
}

export function MyBenefitsPage() {
  const { sessionVersion } = useCustomerAuth();
  return <MyBenefitsContent key={sessionVersion} />;
}

function MyBenefitsContent() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { isAuthenticated, isLoading: authLoading } = useCustomerAuth();
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
  const rewardsQuery = useQuery({
    queryKey: queryKeys.member.rewards,
    queryFn: ({ signal }) => fetchMyRewardWallet(signal),
    enabled: isAuthenticated,
  });
  const claimMutation = useMutation({
    mutationFn: (definitionId: number) => runForCurrentCustomer(
      () => claimCoupon(definitionId),
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

  if (authLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }
  if (!isAuthenticated) {
    return (
      <Container className="page-container" style={{ maxWidth: 760 }}>
        <MyAuthGateCard
          title="로그인이 필요합니다"
          description="보유 쿠폰과 적립금은 로그인 후 내 정보에서 확인할 수 있습니다."
        />
      </Container>
    );
  }

  const coupons = couponsQuery.data ?? [];
  const availableCoupons = coupons.filter((coupon) => coupon.status === "AVAILABLE");
  const wallet = rewardsQuery.data;

  return (
    <Container className="page-container" style={{ maxWidth: 820 }}>
      <div className="my-detail-header mb-4">
        <Link to="/my" className="text-decoration-none small d-inline-block mb-3">
          &larr; 내 정보
        </Link>
        <div className="my-section-kicker mb-2">My Benefits</div>
        <h4 className="mb-2">쿠폰·적립금</h4>
        <p className="text-muted-soft small mb-0">
          상품 주문에 사용할 수 있는 혜택과 적립·사용 이력을 확인합니다.
        </p>
      </div>

      <section className="mb-5" aria-labelledby="reward-wallet-heading">
        <div className="d-flex justify-content-between align-items-end gap-3 mb-3">
          <div>
            <div className="my-section-kicker mb-1">Reward Wallet</div>
            <h5 id="reward-wallet-heading" className="mb-0">적립금 지갑</h5>
          </div>
          <small className="text-muted-soft">1원 단위로 상품 금액 내에서 사용</small>
        </div>
        {rewardsQuery.isLoading && <LoadingSpinner text="적립금 지갑을 불러오는 중입니다" />}
        <ErrorAlert
          error={rewardsQuery.error}
          onRetry={() => { void rewardsQuery.refetch(); }}
          retrying={rewardsQuery.isFetching}
        />
        {wallet && (
          <>
            <Row className="g-3 mb-3">
              <Col md={4}>
                <Card className="my-stat-card h-100 border-0">
                  <Card.Body>
                    <div className="text-muted-soft small mb-1">사용 가능</div>
                    <strong className="fs-4">{formatKRW(wallet.availableBalance)}</strong>
                  </Card.Body>
                </Card>
              </Col>
              <Col md={4}>
                <Card className="my-stat-card h-100 border-0">
                  <Card.Body>
                    <div className="text-muted-soft small mb-1">결제 처리 중</div>
                    <strong className="fs-4">{formatKRW(wallet.reservedBalance)}</strong>
                  </Card.Body>
                </Card>
              </Col>
              <Col md={4}>
                <Card className="my-stat-card h-100 border-0">
                  <Card.Body>
                    <div className="text-muted-soft small mb-1">회수 예정</div>
                    <strong className="fs-4">{formatKRW(wallet.debtBalance)}</strong>
                  </Card.Body>
                </Card>
              </Col>
            </Row>

            <h6 className="mt-4 mb-2">최근 적립금 이력</h6>
            {wallet.history.length === 0 && <EmptyState message="적립금 이력이 없습니다." />}
            {wallet.history.map((history: RewardHistoryResponse) => (
              <Card key={history.id} className="my-list-card border-0 mb-2">
                <Card.Body className="py-3 d-flex flex-wrap justify-content-between gap-3">
                  <div>
                    <strong className="small">{REWARD_HISTORY_LABEL[history.type]}</strong>
                    <div className="text-muted-soft small">
                      {formatDateTime(history.createdAt)}
                      {history.orderId !== null ? ` · 주문 #${history.orderId}` : ""}
                    </div>
                  </div>
                  <div className="text-end">
                    <div>{formatKRW(history.amount)}</div>
                    <small className="text-muted-soft">
                      사용 가능 잔액 {formatKRW(history.availableAfter)}
                    </small>
                  </div>
                </Card.Body>
              </Card>
            ))}
          </>
        )}
      </section>

      <section className="mb-5" aria-labelledby="claimable-coupons-heading">
        <div className="my-section-kicker mb-1">Claim Coupons</div>
        <h5 id="claimable-coupons-heading" className="mb-3">받을 수 있는 쿠폰</h5>
        <ErrorAlert error={claimMutation.error} />
        {claimableQuery.isLoading && <LoadingSpinner text="발급 가능한 쿠폰을 찾는 중입니다" />}
        <ErrorAlert
          error={claimableQuery.error}
          onRetry={() => { void claimableQuery.refetch(); }}
          retrying={claimableQuery.isFetching}
        />
        {claimableQuery.data?.length === 0 && (
          <EmptyState message="현재 새로 받을 수 있는 쿠폰이 없습니다." />
        )}
        {claimableQuery.data?.map((coupon) => (
          <Card key={coupon.definitionId} className="my-list-card border-0 mb-2">
            <Card.Body className="d-flex flex-wrap justify-content-between align-items-center gap-3">
              <div>
                <strong>{coupon.name}</strong>
                <div className="small mt-1">{discountLabel(coupon)}</div>
                <small className="text-muted-soft">
                  {coupon.minOrderAmount > 0 ? `${formatKRW(coupon.minOrderAmount)} 이상 · ` : ""}
                  {formatDateTime(coupon.validUntil)}까지
                </small>
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

      <section aria-labelledby="owned-coupons-heading">
        <div className="d-flex flex-wrap justify-content-between gap-2 align-items-end mb-3">
          <div>
            <div className="my-section-kicker mb-1">My Coupons</div>
            <h5 id="owned-coupons-heading" className="mb-0">보유 쿠폰</h5>
          </div>
          <span className="text-muted-soft small">사용 가능 {availableCoupons.length}장 · 전체 {coupons.length}장</span>
        </div>
        {couponsQuery.isLoading && <LoadingSpinner text="보유 쿠폰을 불러오는 중입니다" />}
        <ErrorAlert
          error={couponsQuery.error}
          onRetry={() => { void couponsQuery.refetch(); }}
          retrying={couponsQuery.isFetching}
        />
        {couponsQuery.data?.length === 0 && <EmptyState message="보유한 쿠폰이 없습니다." />}
        {couponsQuery.data?.map((coupon) => {
          const status = COUPON_STATUS[coupon.status]
            ?? { label: coupon.status, bg: "secondary" };
          return (
            <Card key={coupon.id} className="my-list-card border-0 mb-2">
              <Card.Body className="d-flex flex-wrap justify-content-between align-items-start gap-3">
                <div>
                  <div className="d-flex flex-wrap gap-2 align-items-center mb-1">
                    <strong>{coupon.name}</strong>
                    <Badge bg={status.bg}>{status.label}</Badge>
                  </div>
                  <div className="small">{discountLabel(coupon)}</div>
                  <small className="text-muted-soft">
                    {coupon.minOrderAmount > 0 ? `${formatKRW(coupon.minOrderAmount)} 이상 · ` : ""}
                    {formatDateTime(coupon.validUntil)}까지
                  </small>
                </div>
                <small className="text-muted-soft">쿠폰 #{coupon.id}</small>
              </Card.Body>
            </Card>
          );
        })}
      </section>
    </Container>
  );
}
