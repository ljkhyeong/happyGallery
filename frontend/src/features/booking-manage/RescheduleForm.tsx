import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Form, Button, Row, Col } from "react-bootstrap";
import { ErrorAlert, useToast } from "@/shared/ui";

interface Props {
  currentSlotId: number;
  onReschedule: (newSlotId: number) => Promise<unknown>;
  onSuccess: () => void;
  successMessage?: string;
}

export function RescheduleForm({
  currentSlotId,
  onReschedule,
  onSuccess,
  successMessage = "예약이 변경되었습니다.",
}: Props) {
  const toast = useToast();
  const [newSlotId, setNewSlotId] = useState("");
  const [touched, setTouched] = useState(false);

  const mutation = useMutation({
    mutationFn: () => onReschedule(Number(newSlotId)),
    onSuccess: () => {
      toast.show(successMessage);
      setNewSlotId("");
      setTouched(false);
      onSuccess();
    },
  });

  const slotNum = Number(newSlotId);
  const isEmpty = !(slotNum > 0);
  const isSameSlot = slotNum === currentSlotId;
  const valid = !isEmpty && !isSameSlot;

  const showError = touched && newSlotId !== "";
  let feedback = "";
  if (showError && isEmpty) feedback = "유효한 슬롯 ID를 입력해 주세요.";
  else if (showError && isSameSlot) feedback = "현재와 다른 슬롯을 선택해 주세요.";

  return (
    <Form
      onSubmit={(e) => {
        e.preventDefault();
        setTouched(true);
        if (valid) mutation.mutate();
      }}
    >
      <ErrorAlert error={mutation.error} />
      <Row className="g-2 align-items-end">
        <Col xs={12} sm={8}>
          <Form.Group controlId="booking-reschedule-slot-id">
            <Form.Label>새 슬롯 ID</Form.Label>
            <Form.Control
              type="number"
              min={1}
              value={newSlotId}
              onChange={(e) => setNewSlotId(e.target.value)}
              onBlur={() => setTouched(true)}
              placeholder="변경할 슬롯 ID"
              isInvalid={!!feedback}
            />
            <Form.Control.Feedback type="invalid">
              {feedback}
            </Form.Control.Feedback>
            <Form.Text className="text-muted">
              현재 슬롯: #{currentSlotId}
            </Form.Text>
          </Form.Group>
        </Col>
        <Col xs={12} sm={4}>
          <Button type="submit" variant="warning" className="w-100" disabled={!valid || mutation.isPending}>
            {mutation.isPending ? "변경 중..." : "예약 변경"}
          </Button>
        </Col>
      </Row>
    </Form>
  );
}
