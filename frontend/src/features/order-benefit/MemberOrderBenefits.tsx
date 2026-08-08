import { useEffect, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { Alert, Button, Form } from "react-bootstrap";
import { fetchMyCoupons, type MyCouponResponse } from "@/features/coupon/api";
import { fetchMyRewardWallet } from "@/features/reward/api";
import { queryKeys } from "@/shared/api";
import { formatKRW } from "@/shared/lib";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import {
  calculateCouponDiscount,
  maximumRewardPoints,
  normalizeRewardPoints,
} from "./policy";

interface Props {
  productAmount: number;
  selectedCouponId: number | null;
  rewardPointsToUse: number;
  disabled?: boolean;
  onCouponChange: (couponId: number | null) => void;
  onRewardPointsChange: (points: number) => void;
}

function couponLabel(coupon: MyCouponResponse): string {
  const discount = coupon.discountType === "FIXED"
    ? `${formatKRW(coupon.discountValue)} 할인`
    : `${coupon.discountValue}% 할인 (최대 ${formatKRW(coupon.maxDiscountAmount ?? 0)})`;
  const minimum = coupon.minOrderAmount > 0
    ? ` · ${formatKRW(coupon.minOrderAmount)} 이상`
    : "";
  return `${coupon.name} · ${discount}${minimum}`;
}

export function MemberOrderBenefits({
  productAmount,
  selectedCouponId,
  rewardPointsToUse,
  disabled = false,
  onCouponChange,
  onRewardPointsChange,
}: Props) {
  const couponsQuery = useQuery({
    queryKey: queryKeys.member.coupons,
    queryFn: ({ signal }) => fetchMyCoupons(signal),
  });
  const rewardQuery = useQuery({
    queryKey: queryKeys.member.rewards,
    queryFn: ({ signal }) => fetchMyRewardWallet(signal),
  });

  const selectableCoupons = useMemo(
    () => couponsQuery.data?.filter((coupon) => (
      coupon.status === "AVAILABLE"
      && productAmount >= coupon.minOrderAmount
      && calculateCouponDiscount(coupon, productAmount) > 0
    )) ?? [],
    [couponsQuery.data, productAmount],
  );
  const selectedCoupon = selectableCoupons.find((coupon) => coupon.id === selectedCouponId);
  const couponDiscountAmount = calculateCouponDiscount(selectedCoupon, productAmount);
  const maximumPoints = maximumRewardPoints(
    rewardQuery.data?.availableBalance ?? 0,
    productAmount,
    couponDiscountAmount,
  );

  useEffect(() => {
    if (selectedCouponId !== null && !selectedCoupon) onCouponChange(null);
  }, [onCouponChange, selectedCoupon, selectedCouponId]);

  useEffect(() => {
    const normalized = normalizeRewardPoints(rewardPointsToUse, maximumPoints);
    if (normalized !== rewardPointsToUse) onRewardPointsChange(normalized);
  }, [maximumPoints, onRewardPointsChange, rewardPointsToUse]);

  const isLoading = couponsQuery.isLoading || rewardQuery.isLoading;
  const estimatedProductPayment = Math.max(
    0,
    productAmount - couponDiscountAmount - rewardPointsToUse,
  );

  return (
    <div>
      <h6 className="mb-1">쿠폰·적립금</h6>
      <p className="text-muted-soft small mb-3">
        회원 상품 주문에만 적용됩니다. 배송비를 제외한 상품 금액 기준이며 최종 할인액은 서버에서 다시 확정합니다.
      </p>

      {isLoading && <LoadingSpinner text="사용 가능한 혜택을 확인하는 중입니다" />}
      <ErrorAlert
        error={couponsQuery.error}
        onRetry={() => { void couponsQuery.refetch(); }}
        retrying={couponsQuery.isFetching}
      />
      <ErrorAlert
        error={rewardQuery.error}
        onRetry={() => { void rewardQuery.refetch(); }}
        retrying={rewardQuery.isFetching}
      />

      <Form.Group className="mb-3" controlId="member-order-coupon">
        <Form.Label>쿠폰</Form.Label>
        <Form.Select
          aria-label="사용할 쿠폰"
          value={selectedCouponId ?? ""}
          disabled={disabled || couponsQuery.isLoading || couponsQuery.isError}
          onChange={(event) => {
            const value = Number(event.target.value);
            onCouponChange(Number.isSafeInteger(value) && value > 0 ? value : null);
          }}
        >
          <option value="">쿠폰을 사용하지 않음</option>
          {selectableCoupons.map((coupon) => (
            <option key={coupon.id} value={coupon.id}>{couponLabel(coupon)}</option>
          ))}
        </Form.Select>
        <Form.Text>
          {selectableCoupons.length > 0
            ? `현재 주문에 사용할 수 있는 쿠폰 ${selectableCoupons.length}장`
            : "현재 상품 금액에 사용할 수 있는 쿠폰이 없습니다."}
        </Form.Text>
      </Form.Group>

      <Form.Group className="mb-3" controlId="member-order-reward-points">
        <Form.Label>사용할 적립금</Form.Label>
        <div className="d-flex gap-2">
          <Form.Control
            type="number"
            inputMode="numeric"
            aria-label="사용할 적립금"
            min={0}
            max={maximumPoints}
            step={1}
            value={rewardPointsToUse}
            disabled={disabled || rewardQuery.isLoading || rewardQuery.isError}
            onChange={(event) => {
              onRewardPointsChange(normalizeRewardPoints(Number(event.target.value), maximumPoints));
            }}
          />
          <Button
            type="button"
            variant="outline-secondary"
            disabled={disabled || maximumPoints === 0 || rewardQuery.isError}
            onClick={() => onRewardPointsChange(maximumPoints)}
          >
            전액 사용
          </Button>
        </div>
        <Form.Text>
          사용 가능 {formatKRW(rewardQuery.data?.availableBalance ?? 0)} · 이번 주문 최대 {formatKRW(maximumPoints)}
        </Form.Text>
      </Form.Group>

      {(couponDiscountAmount > 0 || rewardPointsToUse > 0) && (
        <Alert variant="success" className="mb-0 py-2">
          쿠폰 -{formatKRW(couponDiscountAmount)} · 적립금 -{formatKRW(rewardPointsToUse)} ·
          예상 상품 결제액 {formatKRW(estimatedProductPayment)}
        </Alert>
      )}
    </div>
  );
}
