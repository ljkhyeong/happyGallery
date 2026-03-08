import { useState } from "react";
import { Container, Card } from "react-bootstrap";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import { PassPurchaseForm } from "@/features/pass/PassPurchaseForm";
import { PassSuccessCard } from "@/features/pass/PassSuccessCard";
import type { PurchasePassResponse } from "@/shared/types";

type Step = "verify" | "form" | "done";

export function PassPurchasePage() {
  const [step, setStep] = useState<Step>("verify");
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [result, setResult] = useState<PurchasePassResponse | null>(null);

  return (
    <Container className="page-container" style={{ maxWidth: 540 }}>
      <h4 className="mb-4">8회권 구매</h4>

      <Card className="mb-3">
        <Card.Body>
          <p className="text-muted-soft small mb-0">
            8회권을 구매하면 90일간 8회 수업을 이용할 수 있습니다.
            예약 시 8회권 ID를 입력하면 예약금 없이 횟수가 차감됩니다.
          </p>
        </Card.Body>
      </Card>

      {step === "done" && result ? (
        <PassSuccessCard pass={result} />
      ) : (
        <>
          <Card className="mb-4">
            <Card.Body>
              <PhoneVerificationStep
                onVerified={(p, c) => {
                  setPhone(p);
                  setCode(c);
                  setStep("form");
                }}
              />
            </Card.Body>
          </Card>

          {step === "form" && (
            <Card className="mb-4">
              <Card.Body>
                <PassPurchaseForm
                  phone={phone}
                  verificationCode={code}
                  onSuccess={(res) => {
                    setResult(res);
                    setStep("done");
                  }}
                />
              </Card.Body>
            </Card>
          )}
        </>
      )}
    </Container>
  );
}
