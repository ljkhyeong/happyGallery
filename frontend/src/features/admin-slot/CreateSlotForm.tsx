import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Form, Button, Row, Col } from "react-bootstrap";
import { createSlot, fetchClasses } from "./api";
import { REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";
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

  const { data: classes, isLoading: classesLoading } = useQuery({
    queryKey: ["classes"],
    queryFn: fetchClasses,
    staleTime: REFERENCE_DATA_STALE_TIME,
  });

  const mutation = useMutation({
    mutationFn: () =>
      createSlot(adminKey, {
        classId: Number(classId),
        startAt,
      }),
    onSuccess: (slot) => {
      toast.show(`슬롯 #${slot.id} 생성 완료`);
      queryClient.invalidateQueries({ queryKey: ["admin", "slots"] });
      setStartAt("");
    },
    onError: (error) => {
      if (error instanceof ApiError && error.status === 401) {
        onAuthError();
      }
    },
  });

  const hasClasses = (classes?.length ?? 0) > 0;
  const valid = hasClasses && Number(classId) > 0 && Boolean(startAt);

  if (classesLoading) return <LoadingSpinner text="클래스 목록 로딩 중..." />;

  return (
    <Form
      onSubmit={(e) => {
        e.preventDefault();
        if (valid) mutation.mutate();
      }}
    >
      <ErrorAlert error={mutation.error} />
      <Row className="g-2 align-items-end">
        <Col xs={12} md={4}>
          <Form.Group controlId="admin-slot-class">
            <Form.Label>클래스</Form.Label>
            <Form.Select
              value={classId}
              onChange={(e) => setClassId(e.target.value)}
              disabled={!hasClasses}
            >
              <option value="">{hasClasses ? "선택..." : "먼저 클래스를 생성하세요"}</option>
              {classes?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.durationMin}분)
                </option>
              ))}
            </Form.Select>
            {!hasClasses && (
              <Form.Text className="text-muted">
                등록된 클래스가 없습니다. 위에서 클래스를 먼저 생성해 주세요.
              </Form.Text>
            )}
          </Form.Group>
        </Col>
        <Col xs={12} md={4}>
          <Form.Group controlId="admin-slot-start-at">
            <Form.Label>시작 시각</Form.Label>
            <Form.Control
              type="datetime-local"
              value={startAt}
              onChange={(e) => setStartAt(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col xs={12} sm={6} md={4}>
          <Button type="submit" variant="primary" className="w-100" disabled={!valid || mutation.isPending}>
            {mutation.isPending ? "생성 중..." : "슬롯 생성"}
          </Button>
        </Col>
      </Row>
    </Form>
  );
}
