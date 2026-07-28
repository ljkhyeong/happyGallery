import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Badge, Button, Form, ListGroup } from "react-bootstrap";
import { fetchRescheduleSlots } from "./api";
import {
  invalidateSlotAvailability,
  queryKeys,
  runForCurrentCustomer,
} from "@/shared/api";
import { formatDateTime } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import { WorkshopInquiryLink } from "@/features/workshop/WorkshopInquiryLink";

interface Props {
  classId: number;
  className?: string;
  currentSlotId: number;
  currentStartAt: string;
  participantCount: number;
  onReschedule: (newSlotId: number) => Promise<unknown>;
  onSuccess: () => void | Promise<void>;
  successMessage?: string;
}

export function RescheduleForm({
  classId,
  className,
  currentSlotId,
  currentStartAt,
  participantCount,
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

  const applySuccess = async (requireCurrent: () => void) => {
    requireCurrent();
    await invalidateSlotAvailability(queryClient);
    requireCurrent();
    await onSuccess();
    requireCurrent();
    toast.show(successMessage);
    setSelectedSlotId(null);
  };

  const mutation = useMutation({
    mutationFn: (newSlotId: number) => runForCurrentCustomer(
      () => onReschedule(newSlotId),
      (_, requireCurrent) => applySuccess(requireCurrent),
    ),
  });

  const availableSlots = slots?.filter(
    (slot) => slot.id !== currentSlotId && slot.remainingCapacity >= participantCount,
  ) ?? [];
  const selectedSlot = availableSlots.find((slot) => slot.id === selectedSlotId);

  return (
    <Form
      onSubmit={(event) => {
        event.preventDefault();
        if (selectedSlot) mutation.mutate(selectedSlot.id);
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
          현재 예약 {participantCount}명이 모두 이동할 수 있는 시간만 표시됩니다.
        </Form.Text>
      </Form.Group>

      <ErrorAlert error={slotsError} />
      <ErrorAlert error={mutation.error} />
      {slotsLoading && <LoadingSpinner text="예약 가능한 시간 조회 중..." />}
      {!slotsLoading && slots && availableSlots.length === 0 && (
        <div className="mb-3">
          <EmptyState message="선택한 날짜에 변경 가능한 시간이 없습니다." />
          {className && <WorkshopInquiryLink className={className} desiredDate={date} />}
        </div>
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
        disabled={!selectedSlot || mutation.isPending}
      >
        {mutation.isPending ? "변경 중..." : "선택한 시간으로 변경"}
      </Button>
    </Form>
  );
}
