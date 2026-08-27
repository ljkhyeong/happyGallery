import { Card, Table, Row, Col, Badge } from "react-bootstrap";
import { StatusBadge } from "@/shared/ui";
import { formatKRW, formatDateTime, formatDate, FULFILLMENT_TYPE_LABEL } from "@/shared/lib";
import type { OrderDetailResponse } from "@/shared/types";
import { RefundProgressAlert } from "@/features/refund/RefundProgressAlert";
import { ShipmentTrackingActions } from "./ShipmentTrackingActions";
import { ProductPurchaseTerms } from "@/features/product/ProductPurchaseTerms";

interface Props {
  order: OrderDetailResponse;
}

export function OrderDetailCard({ order }: Props) {
  return (
    <Card>
      <Card.Header className="d-flex justify-content-between align-items-center">
        <span>{order.orderNumber}</span>
        <StatusBadge status={order.status} />
      </Card.Header>
      <Card.Body>
        <Row className="g-3 mb-3">
          <Col xs={6}>
            <small className="text-muted-soft d-block">카드·간편결제 금액</small>
            <span>{formatKRW(order.pgPaidAmount)}</span>
          </Col>
          <Col xs={6}>
            <small className="text-muted-soft d-block">결제일</small>
            <span>{order.paidAt ? formatDateTime(order.paidAt) : "-"}</span>
          </Col>
          <Col xs={6}>
            <small className="text-muted-soft d-block">공방 주문 승인 기한</small>
            <span>{order.approvalDeadlineAt ? formatDateTime(order.approvalDeadlineAt) : "-"}</span>
          </Col>
        </Row>

        <h6>주문 상품</h6>
        <Table responsive size="sm" className="mb-3">
          <thead>
            <tr>
              <th>상품</th>
              <th className="text-end">수량</th>
              <th className="text-end">단가</th>
              <th className="text-end">소계</th>
            </tr>
          </thead>
          <tbody>
            {order.items.map((item, i) => (
              <tr key={i}>
                <td>
                  <div>{item.productName}</div>
                  {item.options.length > 0 && (
                    <div className="small text-muted mt-1">
                      {item.options.map((option) => (
                        <div key={`${option.type}-${option.groupName}`}>
                          {option.groupName}: {option.value}
                          {option.priceAdjustment > 0
                            ? ` (+${formatKRW(option.priceAdjustment)})`
                            : ""}
                        </div>
                      ))}
                    </div>
                  )}
                  <div className="mt-2">
                    <ProductPurchaseTerms
                      productName={item.productName}
                      type={item.productType}
                      specification={item.specification}
                      careInstructions={item.careInstructions}
                      productionLeadDays={item.productionLeadDays}
                      compact
                      showCustomizationInquiry={false}
                      showLegacySnapshotNotice
                    />
                  </div>
                </td>
                <td className="text-end">{item.qty}</td>
                <td className="text-end">{formatKRW(item.unitPrice)}</td>
                <td className="text-end">{formatKRW(item.grossAmount)}</td>
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
              <td className="text-end text-success">
                {order.couponDiscountAmount > 0
                  ? `-${formatKRW(order.couponDiscountAmount)}`
                  : formatKRW(0)}
              </td>
            </tr>
            <tr>
              <th colSpan={3} className="text-end">적립금 사용</th>
              <td className="text-end text-success">
                {order.rewardUsedAmount > 0
                  ? `-${formatKRW(order.rewardUsedAmount)}`
                  : formatKRW(0)}
              </td>
            </tr>
            <tr>
              <th colSpan={3} className="text-end">카드·간편결제 금액</th>
              <td className="text-end fw-semibold">{formatKRW(order.pgPaidAmount)}</td>
            </tr>
          </tfoot>
        </Table>

        {order.fulfillment && (
          <>
            <h6>배송·수령 정보</h6>
            <Row className="g-3">
              <Col xs={6}>
                <small className="text-muted-soft d-block">수령 방법</small>
                <Badge bg="info" className="badge-status">
                  {FULFILLMENT_TYPE_LABEL[order.fulfillment.type] ?? "수령 방법 확인 필요"}
                </Badge>
              </Col>
              {order.fulfillment.expectedShipDate && (
                <Col xs={6}>
                  <small className="text-muted-soft d-block">예상 출고일</small>
                  <span>{formatDate(order.fulfillment.expectedShipDate)}</span>
                </Col>
              )}
              {order.fulfillment.shippingAddress && (
                <Col xs={12}>
                  <small className="text-muted-soft d-block">배송지</small>
                  <span>
                    {order.fulfillment.shippingAddress.recipientName} ·{" "}
                    {order.fulfillment.shippingAddress.phone}
                  </span>
                  <span className="d-block">
                    ({order.fulfillment.shippingAddress.postalCode}){" "}
                    {order.fulfillment.shippingAddress.addressLine1}
                    {order.fulfillment.shippingAddress.addressLine2
                      ? ` ${order.fulfillment.shippingAddress.addressLine2}`
                      : ""}
                  </span>
                </Col>
              )}
              {order.fulfillment.pickupDeadlineAt && (
                <Col xs={6}>
                  <small className="text-muted-soft d-block">매장 수령 기한</small>
                  <span>{formatDateTime(order.fulfillment.pickupDeadlineAt)}</span>
                </Col>
              )}
              {order.fulfillment.carrier && (
                <Col xs={6}>
                  <small className="text-muted-soft d-block">택배사</small>
                  <span>{order.fulfillment.carrier}</span>
                </Col>
              )}
              {order.fulfillment.trackingNumber && (
                <Col xs={6}>
                  <small className="text-muted-soft d-block">운송장 번호</small>
                  <span>{order.fulfillment.trackingNumber}</span>
                  {order.fulfillment.carrier && (
                    <ShipmentTrackingActions
                      carrier={order.fulfillment.carrier}
                      trackingNumber={order.fulfillment.trackingNumber}
                    />
                  )}
                </Col>
              )}
              {order.fulfillment.trackingStatusText && (
                <Col xs={12}>
                  <small className="text-muted-soft d-block">택배사 배송 상태</small>
                  <strong>{order.fulfillment.trackingStatusText}</strong>
                  {order.fulfillment.trackingUpdatedAt && (
                    <small className="text-muted-soft ms-2">
                      {formatDateTime(order.fulfillment.trackingUpdatedAt)} 기준
                    </small>
                  )}
                </Col>
              )}
              {order.fulfillment.trackingEvents.length > 0 && (
                <Col xs={12}>
                  <small className="text-muted-soft d-block mb-1">배송 진행 내역</small>
                  <ol className="mb-0 ps-3">
                    {[...order.fulfillment.trackingEvents].reverse().map((event) => (
                      <li key={`${event.occurredAt}-${event.status}-${event.location ?? ""}`} className="mb-1">
                        <span>{event.statusText}</span>
                        {event.location && <span> · {event.location}</span>}
                        <small className="text-muted-soft ms-2">{formatDateTime(event.occurredAt)}</small>
                      </li>
                    ))}
                  </ol>
                </Col>
              )}
            </Row>
          </>
        )}
        <RefundProgressAlert refund={order.refund} />
      </Card.Body>
    </Card>
  );
}
