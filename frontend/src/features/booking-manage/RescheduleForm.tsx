import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Form, Button, Row, Col } from "react-bootstrap";
import { rescheduleBooking } from "./api";
import { ErrorAlert, useToast } from "@/shared/ui";
import type { BookingDetailResponse } from "@/shared/types";

interface Props {
  booking: BookingDetailResponse;
  token: string;
  onSuccess: () => void;
}

export function RescheduleForm({ booking, token, onSuccess }: Props) {
  const toast = useToast();
  const [newSlotId, setNewSlotId] = useState("");

  const mutation = useMutation({
    mutationFn: () => rescheduleBooking(booking.bookingId, Number(newSlotId), token),
    onSuccess: (res) => {
      toast.show(`슬롯 #${res.slotId}로 변경 완료`);
      setNewSlotId("");
      onSuccess();
    },
  });

  const valid = Number(newSlotId) > 0 && Number(newSlotId) !== booking.slotId;

  return (
    <Form
      onSubmit={(e) => {
        e.preventDefault();
        if (valid) mutation.mutate();
      }}
    >
      <ErrorAlert error={mutation.error} />
      <Row className="g-2 align-items-end">
        <Col xs={8}>
          <Form.Group>
            <Form.Label>새 슬롯 ID</Form.Label>
            <Form.Control
              type="number"
              min={1}
              value={newSlotId}
              onChange={(e) => setNewSlotId(e.target.value)}
              placeholder="변경할 슬롯 ID"
            />
            <Form.Text className="text-muted">
              현재 슬롯: #{booking.slotId}
            </Form.Text>
          </Form.Group>
        </Col>
        <Col xs={4}>
          <Button type="submit" variant="warning" className="w-100" disabled={!valid || mutation.isPending}>
            {mutation.isPending ? "변경 중..." : "예약 변경"}
          </Button>
        </Col>
      </Row>
    </Form>
  );
}
