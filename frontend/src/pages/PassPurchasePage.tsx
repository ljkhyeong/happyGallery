import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Container, Card, Form, Row, Col, Button } from "react-bootstrap";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { api } from "@/shared/api";
import { ErrorAlert, useToast } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";

interface MemberPassResponse {
  passId: number;
  purchasedAt: string;
  expiresAt: string;
  totalCredits: number;
  remainingCredits: number;
  totalPrice: number;
}

export function PassPurchasePage() {
  const toast = useToast();
  const { isAuthenticated } = useCustomerAuth();

  const [totalPrice, setTotalPrice] = useState("");
  const [memberDone, setMemberDone] = useState(false);

  const memberMutation = useMutation({
    mutationFn: () =>
      api<MemberPassResponse>("/me/passes", {
        method: "POST",
        body: { totalPrice: Number(totalPrice) || 0 },
      }),
    onSuccess: () => {
      toast.show("8회권이 구매되었습니다!");
      setMemberDone(true);
    },
  });

  const priceValid = totalPrice === "" || Number(totalPrice) >= 0;

  const loginHref = buildAuthPageHref("/login", { redirectTo: "/passes/purchase" });

  if (memberDone) {
    return (
      <Container className="page-container" style={{ maxWidth: 540 }}>
        <h4 className="mb-4">구매 완료</h4>
        <div className="text-center">
          <p className="mb-3">8회권이 구매되었습니다.</p>
          <Button as={"a" as any} href="/my/passes" variant="primary">
            내 8회권 확인하기
          </Button>
        </div>
      </Container>
    );
  }

  return (
    <Container className="page-container" style={{ maxWidth: 540 }}>
      <h4 className="mb-4">8회권 구매</h4>

      <Card className="mb-3">
        <Card.Body>
          <p className="text-muted-soft small mb-0">
            8회권을 구매하면 90일간 8회 수업을 이용할 수 있습니다.
            예약 시 8회권을 선택하면 예약금 없이 횟수가 차감됩니다.
          </p>
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Body>
          <h6 className="mb-3">구매 정보</h6>
          <Row className="g-2 align-items-end">
            <Col xs={8}>
              <Form.Group controlId="pass-total-price">
                <Form.Label>결제 금액 (원)</Form.Label>
                <Form.Control
                  type="number" min={0} value={totalPrice}
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
        </Card.Body>
      </Card>

      <ErrorAlert error={memberMutation.error} />

      {isAuthenticated ? (
        <Button
          variant="primary" size="lg" className="w-100"
          disabled={!priceValid || memberMutation.isPending}
          onClick={() => memberMutation.mutate()}
        >
          {memberMutation.isPending
            ? "구매 처리 중..."
            : `8회권 구매${totalPrice ? ` (${formatKRW(Number(totalPrice))})` : ""}`}
        </Button>
      ) : (
        <Button
          as={"a" as any} href={loginHref}
          variant="primary" size="lg" className="w-100"
        >
          로그인 후 구매하기
        </Button>
      )}
    </Container>
  );
}
