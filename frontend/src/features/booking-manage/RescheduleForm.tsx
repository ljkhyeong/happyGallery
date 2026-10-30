import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Form, ListGroup } from "react-bootstrap";
import { fetchRescheduleSlots } from "./api";
import { BookingDateChips, BookingSlotTime } from "@/features/booking-create/BookingDateChips";
import { isFewSeats, slotDates, UPCOMING_SLOT_DAYS, upcomingSlotsQuery } from "@/features/booking-create/upcomingSlots";
import {
  invalidateSlotAvailability,
  queryKeys,
  runForCurrentCustomer,
} from "@/shared/api";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import { WorkshopInquiryLink } from "@/features/workshop/WorkshopInquiryLink";
import { formatDateInput } from "@/shared/lib";

interface Props {
  classId: number;
  className: string;
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
  // 고객이 칩이나 입력 칸으로 고른 날짜. 고르기 전에는 아래 기본 날짜를 보여 준다.
  const [pickedDate, setPickedDate] = useState<string | null>(null);
  const [selectedSlotId, setSelectedSlotId] = useState<number | null>(null);

  useEffect(() => {
    setPickedDate(null);
    setSelectedSlotId(null);
  }, [classId, currentSlotId, currentStartAt]);

  const upcomingQuery = useQuery(upcomingSlotsQuery(classId));
  const availableDates = slotDates(
    upcomingQuery.data?.filter((slot) => slot.id !== currentSlotId && slot.remainingCapacity >= participantCount),
  );
  // 처음에는 현재 예약일을 보이되, 그날 옮길 시간이 없으면 날짜 칩의 첫 날짜를 고른다. 고르지 않았는데
  // "변경 가능한 시간이 없습니다"부터 보이지 않게 한다. 14일 안에 옮길 날짜가 없거나 조회하지 못하면 현재 예약일이다.
  const currentDate = currentStartAt.slice(0, 10);
  const defaultDate = upcomingQuery.isPending
    ? ""
    : availableDates.includes(currentDate) ? currentDate : availableDates[0] ?? currentDate;
  const date = pickedDate ?? defaultDate;
  const pickDate = (value: string) => {
    setPickedDate(value);
    setSelectedSlotId(null);
  };

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
          onSelect={pickDate}
        />
      ) : upcomingQuery.isSuccess && (
        <p className="booking-step-label">{UPCOMING_SLOT_DAYS}일 내 변경 가능한 날짜가 없습니다. 아래에서 날짜를 직접 골라 주세요.</p>
      )}

      <Form.Group controlId={`booking-reschedule-date-${currentSlotId}`} className="reschedule-date-field">
        <Form.Label>변경할 날짜</Form.Label>
        <Form.Control
          type="date"
          value={date}
          min={formatDateInput(Date.now())}
          onChange={(event) => pickDate(event.target.value)}
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
          <WorkshopInquiryLink className={className} desiredDate={date} />
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
              aria-pressed={selectedSlotId === slot.id}
              onClick={() => setSelectedSlotId(slot.id)}
              className={isFewSeats(slot.remainingCapacity) ? "is-few" : undefined}
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
