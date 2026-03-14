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
  const [nameTouched, setNameTouched] = useState(false);
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

  const nameValid = name.trim().length > 0;
  const priceValid = totalPrice === "" || Number(totalPrice) >= 0;
  const formValid = nameValid && priceValid;

  return (
    <Form
      onSubmit={(e) => {
        e.preventDefault();
        if (formValid && !mutation.isPending) mutation.mutate();
      }}
    >
      <h6 className="mb-3">2. 구매 정보</h6>
      <ErrorAlert error={mutation.error} />

      <Form.Group controlId="pass-purchase-name" className="mb-3">
        <Form.Label>이름</Form.Label>
        <Form.Control
          value={name}
          onChange={(e) => setName(e.target.value)}
          onBlur={() => setNameTouched(true)}
          placeholder="구매자 이름"
          isInvalid={nameTouched && !nameValid}
        />
        <Form.Control.Feedback type="invalid">
          이름을 입력해 주세요.
        </Form.Control.Feedback>
      </Form.Group>

      <Row className="g-2 align-items-end mb-3">
        <Col xs={8}>
          <Form.Group controlId="pass-purchase-total-price">
            <Form.Label>결제 금액 (원)</Form.Label>
            <Form.Control
              type="number"
              min={0}
              value={totalPrice}
              onChange={(e) => setTotalPrice(e.target.value)}
              placeholder="0"
              isInvalid={totalPrice !== "" && Number(totalPrice) < 0}
            />
            <Form.Control.Feedback type="invalid">
              금액은 0원 이상이어야 합니다.
            </Form.Control.Feedback>
            <Form.Text className="text-muted">
              환불 시 잔여 횟수 기준으로 정산됩니다.
            </Form.Text>
          </Form.Group>
        </Col>
      </Row>

      <Button
        type="submit"
        variant="primary"
        size="lg"
        className="w-100"
        disabled={!formValid || mutation.isPending}
      >
        {mutation.isPending
          ? "구매 처리 중..."
          : `8회권 구매${totalPrice ? ` (${formatKRW(Number(totalPrice))})` : ""}`}
      </Button>
    </Form>
  );
}
