import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Form, Button, Row, Col } from "react-bootstrap";
import { createSlot } from "./api";
import { ErrorAlert, useToast } from "@/shared/ui";
import type { SlotResponse } from "@/shared/types";

interface Props {
  adminKey: string;
  onCreated?: (slot: SlotResponse) => void;
}

export function CreateSlotForm({ adminKey, onCreated }: Props) {
  const toast = useToast();
  const [classId, setClassId] = useState("");
  const [startAt, setStartAt] = useState("");
  const [endAt, setEndAt] = useState("");

  const mutation = useMutation({
    mutationFn: () =>
      createSlot(adminKey, {
        classId: Number(classId),
        startAt,
        endAt,
      }),
    onSuccess: (slot) => {
      toast.show(`슬롯 #${slot.id} 생성 완료`);
      onCreated?.(slot);
      setStartAt("");
      setEndAt("");
    },
  });

  const valid = Number(classId) > 0 && startAt && endAt;

  return (
    <Form
      onSubmit={(e) => {
        e.preventDefault();
        if (valid) mutation.mutate();
      }}
    >
      <ErrorAlert error={mutation.error} />
      <Row className="g-2 align-items-end">
        <Col xs={6} md={2}>
          <Form.Group>
            <Form.Label>클래스 ID</Form.Label>
            <Form.Control
              type="number"
              min={1}
              value={classId}
              onChange={(e) => setClassId(e.target.value)}
              placeholder="1"
            />
          </Form.Group>
        </Col>
        <Col xs={12} md={3}>
          <Form.Group>
            <Form.Label>시작 시각</Form.Label>
            <Form.Control
              type="datetime-local"
              value={startAt}
              onChange={(e) => setStartAt(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col xs={12} md={3}>
          <Form.Group>
            <Form.Label>종료 시각</Form.Label>
            <Form.Control
              type="datetime-local"
              value={endAt}
              onChange={(e) => setEndAt(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col xs={6} md={2}>
          <Button type="submit" variant="primary" disabled={!valid || mutation.isPending}>
            {mutation.isPending ? "생성 중..." : "슬롯 생성"}
          </Button>
        </Col>
      </Row>
    </Form>
  );
}
