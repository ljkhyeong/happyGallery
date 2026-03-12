import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Form, Button, Row, Col } from "react-bootstrap";
import { createSlot, fetchClasses } from "./api";
import { ErrorAlert, useToast, LoadingSpinner } from "@/shared/ui";
import { ApiError } from "@/shared/api";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function CreateSlotForm({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [classId, setClassId] = useState("");
  const [startAt, setStartAt] = useState("");
  const [endAt, setEndAt] = useState("");
  const [touched, setTouched] = useState({ startAt: false, endAt: false });

  const { data: classes, isLoading: classesLoading } = useQuery({
    queryKey: ["classes"],
    queryFn: fetchClasses,
  });

  const mutation = useMutation({
    mutationFn: () =>
      createSlot(adminKey, {
        classId: Number(classId),
        startAt,
        endAt,
      }),
    onSuccess: (slot) => {
      toast.show(`슬롯 #${slot.id} 생성 완료`);
      queryClient.invalidateQueries({ queryKey: ["admin", "slots"] });
      setStartAt("");
      setEndAt("");
      setTouched({ startAt: false, endAt: false });
    },
    onError: (error) => {
      if (error instanceof ApiError && error.status === 401) {
        onAuthError();
      }
    },
  });

  const timeValid = startAt && endAt && startAt < endAt;
  const valid = Number(classId) > 0 && timeValid;

  const showEndError = touched.endAt && endAt && startAt && endAt <= startAt;

  if (classesLoading) return <LoadingSpinner text="클래스 목록 로딩 중..." />;

  return (
    <Form
      onSubmit={(e) => {
        e.preventDefault();
        setTouched({ startAt: true, endAt: true });
        if (valid) mutation.mutate();
      }}
    >
      <ErrorAlert error={mutation.error} />
      <Row className="g-2 align-items-end">
        <Col xs={12} md={3}>
          <Form.Group>
            <Form.Label>클래스</Form.Label>
            {classes?.length ? (
              <Form.Select value={classId} onChange={(e) => setClassId(e.target.value)}>
                <option value="">선택...</option>
                {classes.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} ({c.durationMin}분)
                  </option>
                ))}
              </Form.Select>
            ) : (
              <Form.Control
                type="number"
                min={1}
                value={classId}
                onChange={(e) => setClassId(e.target.value)}
                placeholder="클래스 ID"
              />
            )}
          </Form.Group>
        </Col>
        <Col xs={12} md={3}>
          <Form.Group>
            <Form.Label>시작 시각</Form.Label>
            <Form.Control
              type="datetime-local"
              value={startAt}
              onChange={(e) => setStartAt(e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, startAt: true }))}
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
              onBlur={() => setTouched((t) => ({ ...t, endAt: true }))}
              isInvalid={!!showEndError}
            />
            <Form.Control.Feedback type="invalid">
              종료 시각은 시작 시각 이후여야 합니다.
            </Form.Control.Feedback>
          </Form.Group>
        </Col>
        <Col xs={6} md={3}>
          <Button type="submit" variant="primary" disabled={!valid || mutation.isPending}>
            {mutation.isPending ? "생성 중..." : "슬롯 생성"}
          </Button>
        </Col>
      </Row>
    </Form>
  );
}
