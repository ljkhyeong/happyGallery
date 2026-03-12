import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Form, Button, Row, Col } from "react-bootstrap";
import { createGuestBooking } from "./api";
import { ErrorAlert, useToast } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";
import type { BookingResponse, DepositPaymentMethod } from "@/shared/types";

interface Props {
  phone: string;
  verificationCode: string;
  slotId: number;
  onSuccess: (booking: BookingResponse) => void;
}

type PaymentPath = "deposit" | "pass";

export function BookingFormStep({ phone, verificationCode, slotId, onSuccess }: Props) {
  const toast = useToast();
  const [name, setName] = useState("");
  const [nameTouched, setNameTouched] = useState(false);
  const [paymentPath, setPaymentPath] = useState<PaymentPath>("deposit");
  const [depositAmount, setDepositAmount] = useState("");
  const [paymentMethod, setPaymentMethod] = useState<DepositPaymentMethod>("CARD");
  const [passId, setPassId] = useState("");

  const mutation = useMutation({
    mutationFn: () => {
      if (paymentPath === "pass") {
        return createGuestBooking({
          phone,
          verificationCode,
          name,
          slotId,
          passId: Number(passId),
        });
      }
      return createGuestBooking({
        phone,
        verificationCode,
        name,
        slotId,
        depositAmount: Number(depositAmount),
        paymentMethod,
      });
    },
    onSuccess: (booking) => {
      toast.show("예약이 완료되었습니다!");
      onSuccess(booking);
    },
  });

  const nameValid = name.trim().length > 0;
  const depositValid = paymentPath === "deposit" ? Number(depositAmount) > 0 : true;
  const passValid = paymentPath === "pass" ? Number(passId) > 0 : true;
  const valid = nameValid && depositValid && passValid;

  return (
    <Form
      onSubmit={(e) => {
        e.preventDefault();
        if (valid && !mutation.isPending) mutation.mutate();
      }}
    >
      <h6 className="mb-3">3. 예약 정보 입력</h6>
      <ErrorAlert error={mutation.error} />

      <Form.Group className="mb-3">
        <Form.Label>이름</Form.Label>
        <Form.Control
          value={name}
          onChange={(e) => setName(e.target.value)}
          onBlur={() => setNameTouched(true)}
          placeholder="예약자 이름"
          isInvalid={nameTouched && !nameValid}
        />
        <Form.Control.Feedback type="invalid">
          이름을 입력해 주세요.
        </Form.Control.Feedback>
      </Form.Group>

      <Form.Group className="mb-3">
        <Form.Label>결제 방식</Form.Label>
        <div>
          <Form.Check
            inline
            type="radio"
            label="예약금 결제"
            name="paymentPath"
            checked={paymentPath === "deposit"}
            onChange={() => setPaymentPath("deposit")}
          />
          <Form.Check
            inline
            type="radio"
            label="8회권 사용"
            name="paymentPath"
            checked={paymentPath === "pass"}
            onChange={() => setPaymentPath("pass")}
          />
        </div>
      </Form.Group>

      {paymentPath === "deposit" ? (
        <Row className="g-2 mb-3">
          <Col xs={6}>
            <Form.Group>
              <Form.Label>예약금 (원)</Form.Label>
              <Form.Control
                type="number"
                min={1}
                value={depositAmount}
                onChange={(e) => setDepositAmount(e.target.value)}
                placeholder="30000"
                isInvalid={depositAmount !== "" && Number(depositAmount) <= 0}
              />
              <Form.Control.Feedback type="invalid">
                1원 이상 입력해 주세요.
              </Form.Control.Feedback>
            </Form.Group>
          </Col>
          <Col xs={6}>
            <Form.Group>
              <Form.Label>결제 수단</Form.Label>
              <Form.Select
                value={paymentMethod}
                onChange={(e) => setPaymentMethod(e.target.value as DepositPaymentMethod)}
              >
                <option value="CARD">카드</option>
                <option value="EASY_PAY">간편결제</option>
              </Form.Select>
            </Form.Group>
          </Col>
        </Row>
      ) : (
        <Form.Group className="mb-3">
          <Form.Label>8회권 ID</Form.Label>
          <Form.Control
            type="number"
            min={1}
            value={passId}
            onChange={(e) => setPassId(e.target.value)}
            placeholder="8회권 ID"
            isInvalid={passId !== "" && Number(passId) <= 0}
          />
          <Form.Control.Feedback type="invalid">
            유효한 8회권 ID를 입력해 주세요.
          </Form.Control.Feedback>
        </Form.Group>
      )}

      <Button
        type="submit"
        variant="primary"
        size="lg"
        className="w-100"
        disabled={!valid || mutation.isPending}
      >
        {mutation.isPending ? "예약 처리 중..." : `예약하기${paymentPath === "deposit" && depositAmount ? ` (${formatKRW(Number(depositAmount))})` : ""}`}
      </Button>
    </Form>
  );
}
