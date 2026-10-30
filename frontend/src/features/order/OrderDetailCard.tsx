import { useId, type ReactNode } from "react";
import { Badge, Table } from "react-bootstrap";
import { Link } from "react-router";
import { productDetailHref } from "@/features/product/navigation";
import { DetailRows, StatusBadge, type DetailRow } from "@/shared/ui";
import { formatKRW, formatDateTime, formatDate, formatPhone, FULFILLMENT_TYPE_LABEL } from "@/shared/lib";
import { orderNumberLabel } from "@/shared/lib/labels";
import type { OrderDetailResponse } from "@/generated/api/order";
import { RefundProgressAlert } from "@/features/refund/RefundProgressAlert";
import { ShipmentTrackingActions } from "./ShipmentTrackingActions";
import { ProductPurchaseTerms } from "@/features/product/ProductPurchaseTerms";
import { OrderReorderButton } from "./OrderReorderButton";
import { OrderOptionList } from "./OrderOptionList";

interface Props {
  order: OrderDetailResponse;
  /** 결제 요약 아래에 둘 버튼(회원 주문의 결제 영수증 등) */
  actions?: ReactNode;
}

/** 회원·비회원 주문 상세가 함께 쓰는 주문 정보 카드. 예약 정보 카드(BookingInfoCard)와 같은 머리·행·구역으로 쓴다. */
export function OrderDetailCard({ order, actions }: Props) {
  const itemsTitleId = useId();
  const fulfillmentTitleId = useId();
  const summaryRows: DetailRow[] = [
    { label: "카드·간편결제 금액", value: formatKRW(order.pgPaidAmount) },
    { label: "결제일", value: order.paidAt ? formatDateTime(order.paidAt) : "-" },
  ];
  // 서버는 승인한 뒤에도 기한 값을 지우지 않아 공방 승인을 기다리는 동안에만 보인다.
  if (order.status === "PAID_APPROVAL_PENDING" && order.approvalDeadlineAt) {
    summaryRows.push({ label: "공방 주문 승인 기한", value: formatDateTime(order.approvalDeadlineAt) });
  }

  return (
    <article className="my-detail-card">
      <header className="my-detail-card-head">
        <span>{orderNumberLabel(order.orderId)}</span>
        <StatusBadge status={order.status} />
      </header>
      <DetailRows rows={summaryRows} />
      {actions && <div className="my-detail-card-actions">{actions}</div>}
      <RefundProgressAlert refund={order.refund} couponStatus={order.couponStatus} />

      <section className="my-detail-section" aria-labelledby={itemsTitleId}>
        <h2 id={itemsTitleId}>주문한 작품</h2>
        <Table responsive className="my-detail-table">
          <thead>
            <tr>
              <th>작품</th>
              <th className="text-end">수량</th>
              <th className="text-end">단가</th>
              <th className="text-end">소계</th>
            </tr>
          </thead>
          <tbody>
            {order.items.map((item) => (
              <tr key={item.orderItemId}>
                <td>
                  <Link to={productDetailHref(item.productId, item.productVariantId)} className="order-item-name">
                    {item.productName}
                  </Link>
                  <OrderOptionList options={item.options} />
                  {/* 575px 이하에서는 수량·단가·소계 칸을 숨기고 이 한 줄로 보인다. */}
                  <div className="my-detail-item-calc">
                    {item.qty}개 × {formatKRW(item.unitPrice)} = <strong>{formatKRW(item.grossAmount)}</strong>
                  </div>
                  <div className="mt-2">
                    <ProductPurchaseTerms
                      productName={item.productName}
                      type={item.productType}
                      specification={item.specification}
                      careInstructions={item.careInstructions}
                      productionLeadDays={item.productionLeadDays}
                      compact
                      showCustomizationInquiry={false}
                    />
                  </div>
                  <OrderReorderButton item={item} />
                </td>
                <td className="text-end my-detail-item-num">{item.qty}</td>
                <td className="text-end my-detail-item-num">{formatKRW(item.unitPrice)}</td>
                <td className="text-end my-detail-item-num">{formatKRW(item.grossAmount)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <th colSpan={3} className="text-end">상품 금액</th>
              <td className="text-end">{formatKRW(order.productAmount)}</td>
            </tr>
            <tr>
              <th colSpan={3} className="text-end">배송비</th>
              <td className="text-end">{order.shippingFee === 0 ? "무료" : formatKRW(order.shippingFee)}</td>
            </tr>
            <tr>
              <th colSpan={3} className="text-end">쿠폰 할인</th>
              <td className="text-end">
                {order.couponDiscountAmount > 0
                  ? `-${formatKRW(order.couponDiscountAmount)}`
                  : formatKRW(0)}
              </td>
            </tr>
            <tr>
              <th colSpan={3} className="text-end">적립금 사용</th>
              <td className="text-end">
                {order.rewardUsedAmount > 0
                  ? `-${formatKRW(order.rewardUsedAmount)}`
                  : formatKRW(0)}
              </td>
            </tr>
            <tr>
              <th colSpan={3} className="text-end">카드·간편결제 금액</th>
              <td className="text-end">{formatKRW(order.pgPaidAmount)}</td>
            </tr>
          </tfoot>
        </Table>
      </section>

      {order.fulfillment && (
        <section className="my-detail-section" aria-labelledby={fulfillmentTitleId}>
          <h2 id={fulfillmentTitleId}>배송·수령 정보</h2>
          <DetailRows rows={fulfillmentRows(order.fulfillment)} />
        </section>
      )}
    </article>
  );
}

function fulfillmentRows(fulfillment: NonNullable<OrderDetailResponse["fulfillment"]>): DetailRow[] {
  const rows: (DetailRow | null)[] = [
    {
      label: "수령 방법",
      value: (
        <Badge bg="light" className="badge-status">
          {FULFILLMENT_TYPE_LABEL[fulfillment.type] ?? "수령 방법 확인 필요"}
        </Badge>
      ),
    },
    fulfillment.expectedShipDate ? { label: "예상 출고일", value: formatDate(fulfillment.expectedShipDate) } : null,
    fulfillment.shippingAddress ? {
      label: "배송지",
      wide: true,
      value: (
        <>
          {fulfillment.shippingAddress.recipientName} · {formatPhone(fulfillment.shippingAddress.phone)}
          <span className="d-block">
            ({fulfillment.shippingAddress.postalCode}) {fulfillment.shippingAddress.addressLine1}
            {fulfillment.shippingAddress.addressLine2 ? ` ${fulfillment.shippingAddress.addressLine2}` : ""}
          </span>
        </>
      ),
    } : null,
    fulfillment.pickupDeadlineAt ? { label: "매장 수령 기한", value: formatDateTime(fulfillment.pickupDeadlineAt) } : null,
    fulfillment.carrier ? { label: "택배사", value: fulfillment.carrier } : null,
    fulfillment.trackingNumber ? {
      label: "운송장 번호",
      // 복사·배송조회 버튼과 안내가 붙어 좁은 칸에서는 세로로 쪼개진다.
      wide: true,
      value: (
        <>
          {fulfillment.trackingNumber}
          <ShipmentTrackingActions carrierCode={fulfillment.carrierCode} trackingNumber={fulfillment.trackingNumber} />
        </>
      ),
    } : null,
    fulfillment.trackingStatusText ? {
      label: "택배사 배송 상태",
      wide: true,
      value: (
        <>
          {fulfillment.trackingStatusText}
          {fulfillment.trackingUpdatedAt && (
            <small className="text-muted-soft fw-normal ms-2">{formatDateTime(fulfillment.trackingUpdatedAt)} 기준</small>
          )}
        </>
      ),
    } : null,
    fulfillment.trackingEvents.length > 0 ? {
      label: "배송 진행 내역",
      wide: true,
      value: (
        <>
          {fulfillment.carrierCode === "KOREA_POST" && (
            <small className="text-muted-soft fw-normal d-block mb-1">
              출처: <a href="https://www.data.go.kr/data/15000390/openapi.do" target="_blank" rel="noreferrer">우정사업본부 국내우편물 종적 조회</a>
            </small>
          )}
          <ol className="mb-0 ps-3 fw-normal">
            {[...fulfillment.trackingEvents].reverse().map((event) => (
              <li key={`${event.occurredAt}-${event.status}-${event.location ?? ""}`} className="mb-1">
                <span>{event.statusText}</span>
                {event.location && <span> · {event.location}</span>}
                <small className="text-muted-soft ms-2">{formatDateTime(event.occurredAt)}</small>
              </li>
            ))}
          </ol>
        </>
      ),
    } : null,
  ];
  return rows.filter((row): row is DetailRow => row !== null);
}
