import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Container, Card, Button, Form } from "react-bootstrap";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import { OrderItemsForm } from "@/features/order/OrderItemsForm";
import { OrderSuccessCard } from "@/features/order/OrderSuccessCard";
import { createOrder } from "@/features/order/api";
import { ErrorAlert, useToast } from "@/shared/ui";
import type { OrderItemInput, OrderResponse } from "@/shared/types";

type Step = "verify" | "items" | "done";

export function OrderCreatePage() {
  const toast = useToast();
  const [step, setStep] = useState<Step>("verify");
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [name, setName] = useState("");
  const [nameTouched, setNameTouched] = useState(false);
  const [items, setItems] = useState<OrderItemInput[]>([]);
  const [result, setResult] = useState<OrderResponse | null>(null);

  const mutation = useMutation({
    mutationFn: () => createOrder({ phone, verificationCode: code, name, items }),
    onSuccess: (order) => {
      toast.show("주문이 완료되었습니다!");
      setResult(order);
      setStep("done");
    },
  });

  if (step === "done" && result) {
    return (
      <Container className="page-container" style={{ maxWidth: 640 }}>
        <h4 className="mb-4">주문 완료</h4>
        <OrderSuccessCard order={result} />
      </Container>
    );
  }

  return (
    <Container className="page-container" style={{ maxWidth: 640 }}>
      <h4 className="mb-4">상품 주문</h4>

      <Card className="mb-4">
        <Card.Body>
          <PhoneVerificationStep
            onVerified={(p, c) => {
              setPhone(p);
              setCode(c);
              setStep("items");
            }}
          />
        </Card.Body>
      </Card>

      {step === "items" && (
        <>
          <Card className="mb-4">
            <Card.Body>
              <Form.Group>
                <Form.Label>주문자 이름</Form.Label>
                <Form.Control
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  onBlur={() => setNameTouched(true)}
                  placeholder="이름을 입력하세요"
                  isInvalid={nameTouched && !name.trim()}
                />
                <Form.Control.Feedback type="invalid">
                  이름을 입력해 주세요.
                </Form.Control.Feedback>
              </Form.Group>
            </Card.Body>
          </Card>

          <Card className="mb-4">
            <Card.Header>상품 선택</Card.Header>
            <Card.Body>
              <OrderItemsForm items={items} onChange={setItems} />
            </Card.Body>
          </Card>

          <ErrorAlert error={mutation.error} />

          <Button
            variant="primary" size="lg" className="w-100"
            disabled={!name.trim() || items.length === 0 || mutation.isPending}
            onClick={() => { if (!mutation.isPending) mutation.mutate(); }}>
            {mutation.isPending ? "주문 처리 중..." : "주문하기"}
          </Button>
        </>
      )}
    </Container>
  );
}
