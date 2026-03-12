import { useState } from "react";
import { Container, Card } from "react-bootstrap";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import { SlotSelectionStep } from "@/features/booking-create/SlotSelectionStep";
import { BookingFormStep } from "@/features/booking-create/BookingFormStep";
import { BookingSuccessCard } from "@/features/booking-create/BookingSuccessCard";
import type { BookingResponse, PublicSlotResponse } from "@/shared/types";

type Step = "verify" | "select" | "form" | "done";

export function BookingCreatePage() {
  const [step, setStep] = useState<Step>("verify");
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [selectedSlot, setSelectedSlot] = useState<PublicSlotResponse | null>(null);
  const [result, setResult] = useState<BookingResponse | null>(null);

  return (
    <Container className="page-container" style={{ maxWidth: 640 }}>
      <h4 className="mb-4">예약하기</h4>

      {step === "done" && result ? (
        <BookingSuccessCard booking={result} />
      ) : (
        <>
          <Card className="mb-4">
            <Card.Body>
              <PhoneVerificationStep
                onVerified={(p, c) => {
                  setPhone(p);
                  setCode(c);
                  setStep("select");
                }}
              />
            </Card.Body>
          </Card>

          {(step === "select" || step === "form") && (
            <Card className="mb-4">
              <Card.Body>
                <SlotSelectionStep
                  selectedSlotId={selectedSlot?.id ?? null}
                  onSelect={(slot) => {
                    setSelectedSlot(slot);
                    setStep("form");
                  }}
                />
              </Card.Body>
            </Card>
          )}

          {step === "form" && selectedSlot && (
            <Card className="mb-4">
              <Card.Body>
                <BookingFormStep
                  phone={phone}
                  verificationCode={code}
                  slotId={selectedSlot.id}
                  onSuccess={(booking) => {
                    setResult(booking);
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
