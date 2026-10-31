import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Button, Card, Form } from "react-bootstrap";
import { updateMyOrderShippingAddress } from "@/generated/api/customerStore";
import { updateGuestOrderShippingAddress, type OrderDetailResponse } from "@/generated/api/order";
import { runForCurrentCustomer } from "@/shared/api";
import { ErrorAlert } from "@/shared/ui";
import { ShippingAddressFields } from "./ShippingAddressFields";
import { isFulfillmentComplete } from "./fulfillmentSelection";

const ADDRESS_EDITABLE_STATUSES: readonly string[] = [
  "PAID_APPROVAL_PENDING", "APPROVED_FULFILLMENT_PENDING", "IN_PRODUCTION", "DELAY_CONSENT_PENDING", "DELAY_ACCEPTED",
];

/** 택배 주문은 배송 준비를 시작하기 전까지 배송지를 고칠 수 있다. */
export function canEditShippingAddress(order: OrderDetailResponse): boolean {
  return order.fulfillment?.type === "SHIPPING" && Boolean(order.fulfillment.shippingAddress)
    && ADDRESS_EDITABLE_STATUSES.includes(order.status);
}

interface Props {
  order: OrderDetailResponse;
  accessToken?: string;
  onSaved: () => Promise<unknown>;
  /** 저장을 마치거나 수정을 그만두면 주문 상세의 행동 버튼 줄로 돌아간다. */
  onClose: () => void;
}

/** 주문 상세의 배송지 수정 칸. 열고 닫는 버튼은 OrderCustomerActionPanel의 행동 줄에 있다. */
export function ShippingAddressEditPanel({ order, accessToken, onSaved, onClose }: Props) {
  const [address, setAddress] = useState(order.fulfillment!.shippingAddress!);
  const mutation = useMutation({
    mutationFn: () => runForCurrentCustomer(
      () => {
        const request = { version: order.fulfillment!.version, shippingAddress: address };
        return accessToken
          ? updateGuestOrderShippingAddress(order.orderId, request, {
              headers: { "X-Access-Token": accessToken },
            })
          : updateMyOrderShippingAddress(order.orderId, request);
      },
      async (_, requireCurrent) => {
        await onSaved();
        requireCurrent();
        onClose();
      },
    ),
  });
  return (
    <Card className="mb-3">
      <Card.Body>
        <h2 className="h5 display-heading">배송지 수정</h2>
        <p className="small text-muted">배송 준비를 시작하기 전까지 수정할 수 있습니다.</p>
        <ErrorAlert error={mutation.error} />
        <Form onSubmit={(event) => { event.preventDefault(); mutation.mutate(); }}>
          <fieldset disabled={mutation.isPending}>
            <ShippingAddressFields value={address} onChange={setAddress} />
            <div className="d-flex gap-2 mt-3">
              <Button type="submit" disabled={!isFulfillmentComplete({ fulfillmentType: "SHIPPING", shippingAddress: address })}>
                {mutation.isPending ? "저장 중..." : "배송지 저장"}
              </Button>
              {/* 행동 줄의 "주문 취소"와 헷갈리지 않게 무엇을 그만두는지 이름에 적는다. */}
              <Button variant="outline-secondary" onClick={onClose}>수정 취소</Button>
            </div>
          </fieldset>
        </Form>
        {mutation.isError && <Button variant="link" onClick={() => { void runForCurrentCustomer(onSaved); }}>최신 배송지 다시 불러오기</Button>}
      </Card.Body>
    </Card>
  );
}
