import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Badge, Button, Form, ListGroup } from "react-bootstrap";
import { fetchRescheduleSlots } from "./api";
import { invalidateSlotAvailability, queryKeys } from "@/shared/api";
import { formatDateTime } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";

interface Props {
  classId: number;
  currentSlotId: number;
  currentStartAt: string;
  onReschedule: (newSlotId: number) => Promise<unknown>;
  onSuccess: () => void;
  successMessage?: string;
}

export function RescheduleForm({
  classId,
  currentSlotId,
  currentStartAt,
  onReschedule,
  onSuccess,
  successMessage = "예약이 변경되었습니다.",
}: Props) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [date, setDate] = useState(currentStartAt.slice(0, 10));
  const [selectedSlotId, setSelectedSlotId] = useState<number | null>(null);

  useEffect(() => {
    setDate(currentStartAt.slice(0, 10));
    setSelectedSlotId(null);
  }, [classId, currentSlotId, currentStartAt]);

  const {
    data: slots,
    isLoading: slotsLoading,
    error: slotsError,
  } = useQuery({
    queryKey: queryKeys.slotAvailability.reschedule.byClassAndDate(classId, date),
    queryFn: () => fetchRescheduleSlots(classId, date),
    enabled: date.length > 0,
  });

  const mutation = useMutation({
    mutationFn: (newSlotId: number) => onReschedule(newSlotId),
    onSuccess: () => {
      toast.show(successMessage);
      setSelectedSlotId(null);
      void invalidateSlotAvailability(queryClient);
      onSuccess();
    },
  });

  const availableSlots = slots?.filter((slot) => slot.id !== currentSlotId) ?? [];

  return (
    <Form
      onSubmit={(event) => {
        event.preventDefault();
        if (selectedSlotId !== null) mutation.mutate(selectedSlotId);
      }}
    >
      <Form.Group controlId={`booking-reschedule-date-${currentSlotId}`} className="mb-3">
        <Form.Label>변경할 날짜</Form.Label>
        <Form.Control
          type="date"
          value={date}
          min={new Date().toLocaleDateString("sv-SE", { timeZone: "Asia/Seoul" })}
          onChange={(event) => {
            setDate(event.target.value);
            setSelectedSlotId(null);
          }}
        />
        <Form.Text className="text-muted">
          현재 예약과 같은 클래스의 예약 가능한 시간만 표시됩니다.
        </Form.Text>
      </Form.Group>

      <ErrorAlert error={slotsError} />
      <ErrorAlert error={mutation.error} />
      {slotsLoading && <LoadingSpinner text="예약 가능한 시간 조회 중..." />}
      {!slotsLoading && slots && availableSlots.length === 0 && (
        <EmptyState message="선택한 날짜에 변경 가능한 시간이 없습니다." />
      )}

      {availableSlots.length > 0 && (
        <ListGroup className="mb-3">
          {availableSlots.map((slot) => (
            <ListGroup.Item
              key={slot.id}
              data-slot-id={slot.id}
              action
              type="button"
              active={selectedSlotId === slot.id}
              onClick={() => setSelectedSlotId(slot.id)}
              className="d-flex justify-content-between align-items-center gap-3"
            >
              <span>{formatDateTime(slot.startAt)} ~ {formatDateTime(slot.endAt)}</span>
              <Badge bg={slot.remainingCapacity <= 2 ? "warning" : "info"} className="badge-status">
                잔여 {slot.remainingCapacity}명
              </Badge>
            </ListGroup.Item>
          ))}
        </ListGroup>
      )}

      <Button
        type="submit"
        variant="warning"
        disabled={selectedSlotId === null || mutation.isPending}
      >
        {mutation.isPending ? "변경 중..." : "선택한 시간으로 변경"}
      </Button>
    </Form>
  );
}
