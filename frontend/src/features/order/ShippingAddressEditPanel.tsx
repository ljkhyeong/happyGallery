import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Button, Card, Form } from "react-bootstrap";
import { updateMyOrderShippingAddress } from "@/generated/api/customerStore";
import { updateGuestOrderShippingAddress, type OrderDetailResponse } from "@/generated/api/order";
import { runForCurrentCustomer } from "@/shared/api";
import { ErrorAlert } from "@/shared/ui";
import { ShippingAddressFields } from "./ShippingAddressFields";
import { isFulfillmentComplete } from "./FulfillmentForm";

interface Props {
  order: OrderDetailResponse;
  accessToken?: string;
  onSaved: () => Promise<unknown>;
}

export function ShippingAddressEditPanel(props: Props) {
  const { order } = props;
  if (order.fulfillment?.type !== "SHIPPING" || !order.fulfillment.shippingAddress
      || !["PAID_APPROVAL_PENDING", "APPROVED_FULFILLMENT_PENDING", "IN_PRODUCTION",
        "DELAY_CONSENT_PENDING", "DELAY_ACCEPTED"].includes(order.status)) return null;
  return <ShippingAddressEditor key={`${order.orderId}:${order.fulfillment.version}`} {...props} />;
}

function ShippingAddressEditor({ order, accessToken, onSaved }: Props) {
  const [editing, setEditing] = useState(false);
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
        setEditing(false);
      },
    ),
  });
  if (!editing) return <Button variant="outline-secondary" className="mb-3" onClick={() => setEditing(true)}>배송지 수정</Button>;
  return (
    <Card className="mb-3">
      <Card.Body>
        <h5>배송지 수정</h5>
        <p className="small text-muted">배송 준비를 시작하기 전까지 수정할 수 있습니다.</p>
        <ErrorAlert error={mutation.error} />
        <Form onSubmit={(event) => { event.preventDefault(); mutation.mutate(); }}>
          <fieldset disabled={mutation.isPending}>
            <ShippingAddressFields value={address} onChange={setAddress} />
            <div className="d-flex gap-2 mt-3">
              <Button type="submit" disabled={!isFulfillmentComplete({ fulfillmentType: "SHIPPING", shippingAddress: address })}>
                {mutation.isPending ? "저장 중..." : "배송지 저장"}
              </Button>
              <Button variant="outline-secondary" onClick={() => { setEditing(false); setAddress(order.fulfillment!.shippingAddress!); mutation.reset(); }}>취소</Button>
            </div>
          </fieldset>
        </Form>
        {mutation.isError && <Button variant="link" onClick={() => { void runForCurrentCustomer(onSaved); }}>최신 배송지 다시 불러오기</Button>}
      </Card.Body>
    </Card>
  );
}
