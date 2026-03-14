import { Card, Table, Row, Col, Badge } from "react-bootstrap";
import { StatusBadge } from "@/shared/ui";
import { formatKRW, formatDateTime, formatDate } from "@/shared/lib";
import type { OrderDetailResponse } from "@/shared/types";

const FULFILLMENT_TYPE_LABEL: Record<string, string> = {
  SHIPPING: "배송",
  PICKUP: "픽업",
};

interface Props {
  order: OrderDetailResponse;
}

export function OrderDetailCard({ order }: Props) {
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
              <th>상품 ID</th>
              <th className="text-end">수량</th>
              <th className="text-end">단가</th>
              <th className="text-end">소계</th>
            </tr>
          </thead>
          <tbody>
            {order.items.map((item, i) => (
              <tr key={i}>
                <td>{item.productId}</td>
                <td className="text-end">{item.qty}</td>
                <td className="text-end">{formatKRW(item.unitPrice)}</td>
                <td className="text-end">{formatKRW(item.unitPrice * item.qty)}</td>
              </tr>
            ))}
          </tbody>
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
            </Row>
          </>
        )}
      </Card.Body>
    </Card>
  );
}
