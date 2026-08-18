import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Form, Button, Row, Col } from "react-bootstrap";
import { createSlot, fetchClasses } from "./api";
import { ErrorAlert, useToast, LoadingSpinner } from "@/shared/ui";
import { invalidateSlotAvailability, queryKeys } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function CreateSlotForm({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [classId, setClassId] = useState("");
  const [startAt, setStartAt] = useState("");

  const {
    data: classes,
    isLoading: classesLoading,
    isFetching: classesFetching,
    error: classesError,
    refetch: refetchClasses,
  } = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.classes,
    queryFn: () => fetchClasses(adminKey),
  });
  const activeClasses = classes?.filter((bookingClass) => bookingClass.status === "ACTIVE");

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () =>
      createSlot(adminKey, {
        classId: Number(classId),
        startAt,
      }),
    onSuccess: (slot) => {
      toast.show(`수업 일정 #${slot.id}을 생성했습니다.`);
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.slots.all });
      void invalidateSlotAvailability(queryClient);
      setStartAt("");
    },
  });

  const hasClasses = (activeClasses?.length ?? 0) > 0;
  const valid = hasClasses && Number(classId) > 0 && Boolean(startAt);

  if (classesLoading) return <LoadingSpinner text="클래스 목록을 불러오는 중..." />;
  if (classes === undefined) {
    return (
      <ErrorAlert
        error={classesError}
        onRetry={() => { void refetchClasses(); }}
        retrying={classesFetching}
      />
    );
  }

  return (
    <Form
      onSubmit={(e) => {
        e.preventDefault();
        if (valid) mutation.mutate();
      }}
    >
      <ErrorAlert
        error={classesError}
        onRetry={() => { void refetchClasses(); }}
        retrying={classesFetching}
      />
      <ErrorAlert error={mutation.error} />
      <Row className="g-2 align-items-end">
        <Col xs={12} md={4}>
          <Form.Group controlId="admin-slot-class">
            <Form.Label>클래스</Form.Label>
            <Form.Select
              value={classId}
              onChange={(e) => setClassId(e.target.value)}
              disabled={!hasClasses || Boolean(classesError)}
            >
              <option value="">
                {classesError
                  ? "클래스를 다시 조회해 주세요"
                  : hasClasses ? "선택..." : "먼저 클래스를 생성하세요"}
              </option>
              {activeClasses?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.durationMin}분)
                </option>
              ))}
            </Form.Select>
            {!classesError && !hasClasses && (
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
            {mutation.isPending ? "생성 중..." : "수업 일정 생성"}
          </Button>
        </Col>
      </Row>
    </Form>
  );
}
