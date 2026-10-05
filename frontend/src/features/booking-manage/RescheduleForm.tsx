import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Form, ListGroup } from "react-bootstrap";
import { fetchRescheduleSlots } from "./api";
import { BookingDateChips, BookingSlotTime } from "@/features/booking-create/BookingDateChips";
import { UPCOMING_SLOT_DAYS, upcomingSlotsQuery } from "@/features/booking-create/upcomingSlots";
import {
  invalidateSlotAvailability,
  queryKeys,
  runForCurrentCustomer,
} from "@/shared/api";
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
    isFetching: slotsFetching,
    error: slotsError,
    refetch: refetchSlots,
  } = useQuery({
    queryKey: queryKeys.slotAvailability.reschedule.byClassAndDate(classId, date),
    queryFn: () => fetchRescheduleSlots(classId, date),
    enabled: date.length > 0,
  });
  const upcomingQuery = useQuery({
    ...upcomingSlotsQuery(classId),
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
  const availableDates = Array.from(new Set(
    upcomingQuery.data
      ?.filter((slot) => slot.id !== currentSlotId && slot.remainingCapacity >= participantCount)
      .map((slot) => slot.startAt.slice(0, 10)) ?? [],
  )).sort();
  const selectedSlot = availableSlots.find((slot) => slot.id === selectedSlotId);

  return (
    <Form
      className="reschedule-form"
      onSubmit={(event) => {
        event.preventDefault();
        if (selectedSlot) mutation.mutate(selectedSlot.id);
      }}
    >
      {upcomingQuery.isLoading && <p className="booking-step-label">예약 가능한 날짜를 확인하고 있습니다.</p>}
      <ErrorAlert
        error={upcomingQuery.error}
        onRetry={() => { void upcomingQuery.refetch(); }}
        retrying={upcomingQuery.isFetching}
      />
      {availableDates.length > 0 ? (
        <BookingDateChips
          label={`빠른 날짜 선택 (${UPCOMING_SLOT_DAYS}일 이내)`}
          dates={availableDates}
          activeDate={date}
          disabled={mutation.isPending}
          onSelect={(value) => {
            setDate(value);
            setSelectedSlotId(null);
          }}
        />
      ) : upcomingQuery.isSuccess && (
        <p className="booking-step-label">{UPCOMING_SLOT_DAYS}일 내 변경 가능한 날짜가 없습니다. 아래에서 날짜를 직접 골라 주세요.</p>
      )}

      <Form.Group controlId={`booking-reschedule-date-${currentSlotId}`} className="reschedule-date-field">
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
        <Form.Text>
          현재 예약 {participantCount}명이 모두 이동할 수 있는 날짜와 시간만 보여 줍니다. {UPCOMING_SLOT_DAYS}일 뒤는 직접 고르세요.
        </Form.Text>
      </Form.Group>

      <ErrorAlert error={slotsError} onRetry={() => { void refetchSlots(); }} retrying={slotsFetching} />
      <ErrorAlert error={mutation.error} />
      {slotsLoading && <LoadingSpinner text="예약 가능한 시간 조회 중..." />}
      {!slotsLoading && slots && availableSlots.length === 0 && (
        <div className="mb-3">
          <EmptyState message="선택한 날짜에 변경 가능한 시간이 없습니다." />
          {className && <WorkshopInquiryLink className={className} desiredDate={date} />}
        </div>
      )}

      {availableSlots.length > 0 && (
        <ListGroup className="booking-slot-list mb-3">
          {availableSlots.map((slot) => (
            <ListGroup.Item
              key={slot.id}
              data-slot-id={slot.id}
              action
              type="button"
              active={selectedSlotId === slot.id}
              onClick={() => setSelectedSlotId(slot.id)}
              className={slot.remainingCapacity <= 2 ? "is-few" : undefined}
            >
              <BookingSlotTime slot={slot} />
              <span className="booking-slot-seats">{slot.remainingCapacity}명 예약 가능</span>
            </ListGroup.Item>
          ))}
        </ListGroup>
      )}

      <Button
        type="submit"
        variant="primary"
        disabled={!selectedSlot || mutation.isPending}
      >
        {mutation.isPending ? "변경 중..." : "선택한 시간으로 변경"}
      </Button>
    </Form>
  );
}
