import { Card, Table, Row, Col, Badge } from "react-bootstrap";
import { StatusBadge } from "@/shared/ui";
import { formatKRW, formatDateTime, formatDate, FULFILLMENT_TYPE_LABEL } from "@/shared/lib";
import type { OrderDetailResponse } from "@/shared/types";
import { RefundProgressAlert } from "@/features/refund/RefundProgressAlert";

interface Props {
  order: OrderDetailResponse;
}

export function OrderDetailCard({ order }: Props) {
  const itemTotal = order.items.reduce((total, item) => total + item.unitPrice * item.qty, 0);

  return (
    <Card>
      <Card.Header className="d-flex justify-content-between align-items-center">
        <span>주문 #{order.orderId}</span>
        <StatusBadge status={order.status} />
      </Card.Header>
      <Card.Body>
        <Row className="g-3 mb-3">
          <Col xs={6}>
            <small className="text-muted-soft d-block">결제 금액</small>
            <span>{formatKRW(order.totalAmount)}</span>
          </Col>
          <Col xs={6}>
            <small className="text-muted-soft d-block">결제일</small>
            <span>{formatDateTime(order.paidAt)}</span>
          </Col>
          <Col xs={6}>
            <small className="text-muted-soft d-block">승인 마감</small>
            <span>{formatDateTime(order.approvalDeadlineAt)}</span>
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
                <td>{item.productName}</td>
                <td className="text-end">{item.qty}</td>
                <td className="text-end">{formatKRW(item.unitPrice)}</td>
                <td className="text-end">{formatKRW(item.unitPrice * item.qty)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <th colSpan={3} className="text-end">상품 합계</th>
              <td className="text-end">{formatKRW(itemTotal)}</td>
            </tr>
            <tr>
              <th colSpan={3} className="text-end">배송비</th>
              <td className="text-end">{order.shippingFee === 0 ? "무료" : formatKRW(order.shippingFee)}</td>
            </tr>
            <tr>
              <th colSpan={3} className="text-end">결제 금액</th>
              <td className="text-end fw-semibold">{formatKRW(order.totalAmount)}</td>
            </tr>
          </tfoot>
        </Table>

        {order.fulfillment && (
          <>
            <h6>이행 정보</h6>
            <Row className="g-3">
              <Col xs={6}>
                <small className="text-muted-soft d-block">유형</small>
                <Badge bg="info" className="badge-status">
                  {FULFILLMENT_TYPE_LABEL[order.fulfillment.type] ?? order.fulfillment.type}
                </Badge>
              </Col>
              {order.fulfillment.expectedShipDate && (
                <Col xs={6}>
                  <small className="text-muted-soft d-block">예상 출고일</small>
                  <span>{formatDate(order.fulfillment.expectedShipDate)}</span>
                </Col>
              )}
              {order.fulfillment.pickupDeadlineAt && (
                <Col xs={6}>
                  <small className="text-muted-soft d-block">픽업 마감</small>
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
