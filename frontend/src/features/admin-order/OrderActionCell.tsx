import { useState } from "react";
import { Button, Form, InputGroup } from "react-bootstrap";
import type { OrderStatus } from "@/shared/types";
import type { OrderMutations } from "./useOrderMutations";

interface Props {
  orderId: number;
  status: OrderStatus;
  mutations: OrderMutations;
}

export function OrderActionCell({ orderId, status, mutations }: Props) {
  const [pickupDeadline, setPickupDeadline] = useState("");
  const [shipDateValue, setShipDateValue] = useState("");

  const disabled = mutations.pendingId === orderId;
  const pending = disabled;

  switch (status) {
    case "PAID_APPROVAL_PENDING":
      return (
        <div className="d-flex gap-1">
          <Button size="sm" variant="success" disabled={disabled}
            onClick={() => mutations.approve.mutate(orderId)}>
            {pending ? "..." : "승인"}
          </Button>
          <Button size="sm" variant="outline-danger" disabled={disabled}
            onClick={() => mutations.reject.mutate(orderId)}>
            {pending ? "..." : "거절"}
          </Button>
        </div>
      );
    case "IN_PRODUCTION":
    case "DELAY_REQUESTED":
      return (
        <div className="d-flex gap-1 flex-wrap">
          <Button size="sm" variant="info" disabled={disabled}
            onClick={() => mutations.completeProduction.mutate(orderId)}>
            {pending ? "..." : "제작 완료"}
          </Button>
          {status === "IN_PRODUCTION" && (
            <Button size="sm" variant="outline-warning" disabled={disabled}
              onClick={() => mutations.delay.mutate(orderId)}>
              {pending ? "..." : "지연"}
            </Button>
          )}
          {status === "DELAY_REQUESTED" && (
            <Button size="sm" variant="outline-success" disabled={disabled}
              onClick={() => mutations.resumeProduction.mutate(orderId)}>
              {pending ? "..." : "재개"}
            </Button>
          )}
          <InputGroup size="sm" style={{ width: "auto" }}>
            <Form.Control type="date" value={shipDateValue}
              onChange={(e) => setShipDateValue(e.target.value)}
              style={{ maxWidth: 150 }} />
            <Button variant="outline-primary" disabled={disabled}
              onClick={() => mutations.shipDate.mutate({ id: orderId, body: { expectedShipDate: shipDateValue || undefined } })}>출고일</Button>
          </InputGroup>
        </div>
      );
    case "APPROVED_FULFILLMENT_PENDING":
      return (
        <div className="d-flex gap-1 flex-wrap">
          <InputGroup size="sm" style={{ width: "auto" }}>
            <Form.Control type="datetime-local" value={pickupDeadline}
              onChange={(e) => setPickupDeadline(e.target.value)}
              style={{ maxWidth: 200 }} />
            <Button variant="outline-primary" disabled={disabled}
              onClick={() => mutations.pickup.mutate({ id: orderId, body: { pickupDeadlineAt: pickupDeadline || undefined } })}>
              {pending ? "..." : "픽업 준비"}
            </Button>
          </InputGroup>
          <Button size="sm" variant="outline-info" disabled={disabled}
            onClick={() => mutations.prepareShipping.mutate(orderId)}>
            {pending ? "..." : "배송 준비"}
          </Button>
        </div>
      );
    case "SHIPPING_PREPARING":
      return (
        <Button size="sm" variant="primary" disabled={disabled}
          onClick={() => mutations.shipped.mutate(orderId)}>
          {pending ? "..." : "배송 출발"}
        </Button>
      );
    case "SHIPPED":
      return (
        <Button size="sm" variant="success" disabled={disabled}
          onClick={() => mutations.delivered.mutate(orderId)}>
          {pending ? "..." : "배송 완료"}
        </Button>
      );
    case "PICKUP_READY":
      return (
        <Button size="sm" variant="outline-success" disabled={disabled}
          onClick={() => mutations.pickupDone.mutate(orderId)}>
          {pending ? "..." : "픽업 완료"}
        </Button>
      );
    default:
      return null;
  }
}
