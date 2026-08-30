import { useEffect, useId, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Form, Modal } from "react-bootstrap";
import type { ReduceBookingParticipantsResponse } from "@/generated/api/booking";
import {
  invalidateSlotAvailability,
  runForCurrentCustomer,
} from "@/shared/api";
import { formatDateTime, formatKRW } from "@/shared/lib";
import type { BookingCancelPolicy } from "@/shared/types";
import { ErrorAlert, useToast } from "@/shared/ui";

interface Props {
  participantCount: number;
  depositAmount: number;
  cancelPolicy: BookingCancelPolicy;
  passBooking?: boolean;
  onReduce: (participantCount: number) => Promise<ReduceBookingParticipantsResponse>;
  onSuccess: () => void | Promise<void>;
}

export function ReduceParticipantsForm({
  participantCount,
  depositAmount,
  cancelPolicy,
  passBooking = false,
  onReduce,
  onSuccess,
}: Props) {
  const titleId = `booking-participant-reduction-title-${useId()}`;
  const queryClient = useQueryClient();
  const toast = useToast();
  const [nextParticipantCount, setNextParticipantCount] = useState(
    Math.max(1, participantCount - 1),
  );
  const [showConfirm, setShowConfirm] = useState(false);

  useEffect(() => {
    setNextParticipantCount(Math.max(1, participantCount - 1));
  }, [participantCount]);

  const estimatedDeposit = Math.floor(
    depositAmount * nextParticipantCount / participantCount,
  );
  const estimatedRefund = depositAmount - estimatedDeposit;
  const canReduce = participantCount > 1
    && cancelPolicy.cancellable
    && cancelPolicy.refundable
    && !cancelPolicy.manualCompensationRequired
    && !passBooking;

  const mutation = useMutation({
    mutationFn: () => runForCurrentCustomer(
      () => onReduce(nextParticipantCount),
      async (response, requireCurrent) => {
        requireCurrent();
        await Promise.all([
          invalidateSlotAvailability(queryClient),
          onSuccess(),
        ]);
        requireCurrent();
        setShowConfirm(false);
        const message = response.refund
          ? `${response.canceledParticipantCount}명 부분취소와 ${formatKRW(response.refundAmount)} 환불 요청이 접수되었습니다.`
          : `${response.canceledParticipantCount}명 부분취소가 완료되었습니다.`;
        toast.show(message, response.refund ? "info" : "success");
      },
    ),
  });

  if (!canReduce) {
    const message = cancelPolicy.manualCompensationRequired
      ? "오프라인에서 받은 예약금이 있어 인원 변경은 공방에 문의해 주세요."
      : !cancelPolicy.refundable
        ? "부분취소 마감이 지나 인원을 줄일 수 없습니다."
        : "현재 예약은 고객이 직접 인원을 줄일 수 없습니다. 공방에 문의해 주세요.";
    return <Alert variant="warning" className="mb-0 py-2 small">{message}</Alert>;
  }

  return (
    <>
      <Form
        onSubmit={(event) => {
          event.preventDefault();
          setShowConfirm(true);
        }}
      >
        <Form.Group controlId={`booking-participant-count-${participantCount}`}>
          <Form.Label>남길 예약 인원</Form.Label>
          <Form.Select
            value={nextParticipantCount}
            onChange={(event) => setNextParticipantCount(Number(event.target.value))}
          >
            {Array.from({ length: participantCount - 1 }, (_, index) => index + 1)
              .map((count) => <option key={count} value={count}>{count}명</option>)}
          </Form.Select>
          <Form.Text className="text-muted">
            {participantCount - nextParticipantCount}명 취소 · 예상 예약금 환불 {formatKRW(estimatedRefund)}
          </Form.Text>
        </Form.Group>
        <Button type="submit" variant="outline-warning" className="mt-3">
          인원 부분취소
        </Button>
      </Form>

      <Modal
        show={showConfirm}
        aria-labelledby={titleId}
        onHide={() => setShowConfirm(false)}
        centered
      >
        <Modal.Header closeButton>
          <Modal.Title id={titleId}>예약 인원 부분취소</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ErrorAlert error={mutation.error} />
          <p>
            현재 {participantCount}명 예약을 {nextParticipantCount}명으로 변경합니다.
            줄인 인원만큼 회차 자리가 다시 열립니다.
          </p>
          <dl className="row small mb-3">
            <dt className="col-6">취소 인원</dt>
            <dd className="col-6 text-end">{participantCount - nextParticipantCount}명</dd>
            <dt className="col-6">예상 예약금 환불</dt>
            <dd className="col-6 text-end">{formatKRW(estimatedRefund)}</dd>
            <dt className="col-6">부분취소 마감</dt>
            <dd className="col-6 text-end mb-0">{formatDateTime(cancelPolicy.deadlineAt)}</dd>
          </dl>
          <Alert variant="info" className="mb-0 py-2 small">
            최종 환불액은 서버가 현재 예약 금액을 기준으로 다시 계산하며, 결제사 처리 완료까지 시간이 걸릴 수 있습니다.
          </Alert>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowConfirm(false)}>
            닫기
          </Button>
          <Button
            variant="warning"
            disabled={mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            {mutation.isPending ? "처리 중..." : `${participantCount - nextParticipantCount}명 부분취소`}
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  );
}
