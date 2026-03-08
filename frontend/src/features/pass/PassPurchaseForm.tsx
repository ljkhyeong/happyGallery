import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Form, Button, Row, Col } from "react-bootstrap";
import { purchasePassByPhone } from "./api";
import { ErrorAlert, useToast } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";
import type { PurchasePassResponse } from "@/shared/types";

interface Props {
  phone: string;
  verificationCode: string;
  onSuccess: (result: PurchasePassResponse) => void;
}

export function PassPurchaseForm({ phone, verificationCode, onSuccess }: Props) {
  const toast = useToast();
  const [name, setName] = useState("");
  const [totalPrice, setTotalPrice] = useState("");

  const mutation = useMutation({
    mutationFn: () =>
      purchasePassByPhone({
        phone,
        verificationCode,
        name,
        totalPrice: Number(totalPrice) || 0,
      }),
    onSuccess: (result) => {
      toast.show("8회권이 구매되었습니다!");
      onSuccess(result);
    },
  });

  const valid = name.trim().length > 0;

  return (
    <div>
      <h6 className="mb-3">2. 구매 정보</h6>
      <ErrorAlert error={mutation.error} />

      <Form.Group className="mb-3">
        <Form.Label>이름</Form.Label>
        <Form.Control
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="구매자 이름"
        />
      </Form.Group>

      <Row className="g-2 align-items-end mb-3">
        <Col xs={8}>
          <Form.Group>
            <Form.Label>결제 금액 (원)</Form.Label>
            <Form.Control
              type="number"
              min={0}
              value={totalPrice}
              onChange={(e) => setTotalPrice(e.target.value)}
              placeholder="0"
            />
            <Form.Text className="text-muted">
              환불 시 잔여 횟수 기준으로 정산됩니다.
            </Form.Text>
          </Form.Group>
        </Col>
      </Row>

      <Button
        variant="primary"
        size="lg"
        className="w-100"
        disabled={!valid || mutation.isPending}
        onClick={() => mutation.mutate()}
      >
        {mutation.isPending
          ? "구매 처리 중..."
          : `8회권 구매${totalPrice ? ` (${formatKRW(Number(totalPrice))})` : ""}`}
      </Button>
    </div>
  );
}
