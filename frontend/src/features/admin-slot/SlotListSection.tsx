import { useState, useCallback, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Table, Button, Badge, Form, Row, Col, ProgressBar } from "react-bootstrap";
import { fetchClasses, fetchSlotsByClass, deactivateSlot } from "./api";
import { LoadingSpinner, ErrorAlert, EmptyState, useToast } from "@/shared/ui";
import { ApiError } from "@/shared/api";
import { formatDateTime } from "@/shared/lib";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function SlotListSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [classId, setClassId] = useState("");
  const [pendingId, setPendingId] = useState<number | null>(null);

  const { data: classes } = useQuery({
    queryKey: ["classes"],
    queryFn: fetchClasses,
  });

  const classIdNum = Number(classId);
  const { data: slots, isLoading, error } = useQuery({
    queryKey: ["admin", "slots", classIdNum],
    queryFn: () => fetchSlotsByClass(adminKey, classIdNum),
    enabled: classIdNum > 0,
  });

  useEffect(() => {
    if (error instanceof ApiError && error.status === 401) {
      onAuthError();
    }
  }, [error, onAuthError]);

  const mutation = useMutation({
    mutationFn: (slotId: number) => deactivateSlot(adminKey, slotId),
    onMutate: (slotId) => setPendingId(slotId),
    onSuccess: (slot) => {
      toast.show(`슬롯 #${slot.id} 비활성화 완료`);
      queryClient.invalidateQueries({ queryKey: ["admin", "slots", classIdNum] });
    },
    onError: (err) => {
      if (err instanceof ApiError && err.status === 401) onAuthError();
    },
    onSettled: () => setPendingId(null),
  });

  const handleDeactivate = useCallback(
    (id: number) => mutation.mutate(id),
    [mutation],
  );

  return (
    <div>
      <Row className="g-2 mb-3">
        <Col xs={12} sm={6}>
          <Form.Group>
            <Form.Label>클래스 선택</Form.Label>
            <Form.Select value={classId} onChange={(e) => setClassId(e.target.value)}>
              <option value="">클래스를 선택하세요</option>
              {classes?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.durationMin}분)
                </option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
      </Row>

      {!classIdNum && <EmptyState message="클래스를 선택하면 슬롯 목록이 표시됩니다." />}
      {isLoading && <LoadingSpinner />}
      {error && !(error instanceof ApiError && error.status === 401) && <ErrorAlert error={error} />}
      {slots && slots.length === 0 && <EmptyState message="해당 클래스에 슬롯이 없습니다." />}

      {slots && slots.length > 0 && (
        <Table responsive hover size="sm">
          <thead>
            <tr>
              <th>ID</th>
              <th>시작</th>
              <th>종료</th>
              <th>예약 현황</th>
              <th>상태</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {slots.map((s) => {
              const pct = s.capacity > 0 ? Math.round((s.bookedCount / s.capacity) * 100) : 0;
              const variant = pct >= 80 ? "danger" : pct >= 50 ? "warning" : "success";
              return (
                <tr key={s.id}>
                  <td>{s.id}</td>
                  <td>{formatDateTime(s.startAt)}</td>
                  <td>{formatDateTime(s.endAt)}</td>
                  <td style={{ minWidth: 120 }}>
                    <div className="d-flex align-items-center gap-2">
                      <ProgressBar now={pct} variant={variant} style={{ flex: 1, height: 8 }} />
                      <small className="text-nowrap">{s.bookedCount}/{s.capacity}</small>
                    </div>
                  </td>
                  <td>
                    <Badge bg={s.isActive ? "success" : "secondary"}>
                      {s.isActive ? "활성" : "비활성"}
                    </Badge>
                  </td>
                  <td>
                    {s.isActive && (
                      <Button
                        size="sm"
                        variant="outline-danger"
                        disabled={pendingId === s.id}
                        onClick={() => handleDeactivate(s.id)}
                      >
                        {pendingId === s.id ? "처리 중..." : "비활성화"}
                      </Button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </Table>
      )}
    </div>
  );
}
