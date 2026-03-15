import { useEffect, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Alert, Badge, Button, Form, Modal, Stack } from "react-bootstrap";
import { claimGuestRecords, getGuestClaimPreview, verifyGuestClaimPhone } from "./api";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import { queryClient } from "@/shared/api";
import { ErrorAlert, useToast } from "@/shared/ui";
import { formatDateTime, formatKRW } from "@/shared/lib";

interface Props {
  show: boolean;
  onClose: () => void;
  phone: string;
  phoneVerified: boolean;
  onPhoneVerified: () => Promise<void>;
}

function toDigits(phone: string) {
  return phone.replace(/\D/g, "");
}

export function GuestClaimModal({ show, onClose, phone, phoneVerified, onPhoneVerified }: Props) {
  const toast = useToast();
  const [previewOverride, setPreviewOverride] = useState<Awaited<ReturnType<typeof getGuestClaimPreview>> | null>(null);
  const [selectedOrderIds, setSelectedOrderIds] = useState<number[]>([]);
  const [selectedBookingIds, setSelectedBookingIds] = useState<number[]>([]);
  const [selectedPassIds, setSelectedPassIds] = useState<number[]>([]);

  const previewQuery = useQuery({
    queryKey: ["my", "guest-claims", "preview"],
    queryFn: getGuestClaimPreview,
    enabled: show && phoneVerified,
  });

  useEffect(() => {
    if (!show) {
      setPreviewOverride(null);
      setSelectedOrderIds([]);
      setSelectedBookingIds([]);
      setSelectedPassIds([]);
    }
  }, [show]);

  const preview = previewOverride ?? previewQuery.data;

  useEffect(() => {
    if (!preview) return;
    setSelectedOrderIds(preview.orders.map((item) => item.orderId));
    setSelectedBookingIds(preview.bookings.map((item) => item.bookingId));
    setSelectedPassIds(preview.passes.map((item) => item.passId));
  }, [preview]);

  const verifyMutation = useMutation({
    mutationFn: (verificationCode: string) => verifyGuestClaimPhone(verificationCode),
    onSuccess: async (data) => {
      setPreviewOverride(data);
      await onPhoneVerified();
    },
  });

  const claimMutation = useMutation({
    mutationFn: () => claimGuestRecords(selectedOrderIds, selectedBookingIds, selectedPassIds),
    onSuccess: async (data) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["my", "orders"] }),
        queryClient.invalidateQueries({ queryKey: ["my", "bookings"] }),
        queryClient.invalidateQueries({ queryKey: ["my", "passes"] }),
        queryClient.invalidateQueries({ queryKey: ["my", "guest-claims", "preview"] }),
        onPhoneVerified(),
      ]);
      toast.show(
        `비회원 이력을 가져왔습니다. 주문 ${data.claimedOrderCount}건, 예약 ${data.claimedBookingCount}건, 8회권 ${data.claimedPassCount}건`,
      );
      onClose();
    },
  });

  function toggle(selected: number[], id: number, setter: (value: number[]) => void) {
    setter(
      selected.includes(id)
        ? selected.filter((value) => value !== id)
        : [...selected, id],
    );
  }

  const totalSelectedCount =
    selectedOrderIds.length + selectedBookingIds.length + selectedPassIds.length;

  const combinedError =
    previewQuery.error ?? verifyMutation.error ?? claimMutation.error ?? null;

  const needsPhoneVerification =
    !phoneVerified && !(previewOverride?.phoneVerified ?? false);

  return (
    <Modal show={show} onHide={onClose} centered>
      <Modal.Header closeButton>
        <Modal.Title className="fs-6">비회원 이력 가져오기</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <p className="text-muted-soft small mb-3">
          같은 휴대폰 번호로 남긴 비회원 주문, 예약, 8회권을 현재 회원 계정으로 이전합니다.
        </p>
        <ErrorAlert error={combinedError as Error | null} />

        {needsPhoneVerification ? (
          <PhoneVerificationStep
            title="휴대폰 재인증"
            description="회원 조회는 로그인으로 가능하지만, 기존 비회원 이력을 가져오려면 같은 번호인지 한 번 더 확인합니다."
            initialPhone={toDigits(phone)}
            lockPhone
            confirmLabel="인증하고 불러오기"
            onVerified={(_, verificationCode) => verifyMutation.mutate(verificationCode)}
          />
        ) : (
          <>
            {previewQuery.isLoading && !preview && <p className="mb-0">비회원 이력을 확인하는 중입니다...</p>}

            {preview && (
              <Stack gap={3}>
                <div className="d-flex justify-content-between align-items-center">
                  <span className="small text-muted-soft">인증 상태</span>
                  <Badge bg="success">확인 완료</Badge>
                </div>

                {preview.orders.length === 0 &&
                  preview.bookings.length === 0 &&
                  preview.passes.length === 0 && (
                    <Alert variant="light" className="mb-0">
                      현재 가져올 비회원 이력이 없습니다.
                    </Alert>
                  )}

                {preview.orders.length > 0 && (
                  <div>
                    <div className="d-flex justify-content-between align-items-center mb-2">
                      <strong className="small">주문</strong>
                      <span className="text-muted-soft small">{preview.orders.length}건</span>
                    </div>
                    <Stack gap={2}>
                      {preview.orders.map((order) => (
                        <Form.Check
                          key={order.orderId}
                          id={`claim-order-${order.orderId}`}
                          type="checkbox"
                          checked={selectedOrderIds.includes(order.orderId)}
                          onChange={() =>
                            toggle(selectedOrderIds, order.orderId, setSelectedOrderIds)
                          }
                          label={
                            <div className="small">
                              <div className="fw-semibold">주문 #{order.orderId}</div>
                              <div className="text-muted-soft">
                                {order.status} · {formatKRW(order.totalAmount)} · {formatDateTime(order.createdAt)}
                              </div>
                            </div>
                          }
                        />
                      ))}
                    </Stack>
                  </div>
                )}

                {preview.bookings.length > 0 && (
                  <div>
                    <div className="d-flex justify-content-between align-items-center mb-2">
                      <strong className="small">예약</strong>
                      <span className="text-muted-soft small">{preview.bookings.length}건</span>
                    </div>
                    <Stack gap={2}>
                      {preview.bookings.map((booking) => (
                        <Form.Check
                          key={booking.bookingId}
                          id={`claim-booking-${booking.bookingId}`}
                          type="checkbox"
                          checked={selectedBookingIds.includes(booking.bookingId)}
                          onChange={() =>
                            toggle(selectedBookingIds, booking.bookingId, setSelectedBookingIds)
                          }
                          label={
                            <div className="small">
                              <div className="fw-semibold">
                                {booking.className} #{booking.bookingId}
                              </div>
                              <div className="text-muted-soft">
                                {booking.status} · {formatDateTime(booking.startAt)}
                              </div>
                            </div>
                          }
                        />
                      ))}
                    </Stack>
                  </div>
                )}

                {preview.passes.length > 0 && (
                  <div>
                    <div className="d-flex justify-content-between align-items-center mb-2">
                      <strong className="small">8회권</strong>
                      <span className="text-muted-soft small">{preview.passes.length}건</span>
                    </div>
                    <Stack gap={2}>
                      {preview.passes.map((pass) => (
                        <Form.Check
                          key={pass.passId}
                          id={`claim-pass-${pass.passId}`}
                          type="checkbox"
                          checked={selectedPassIds.includes(pass.passId)}
                          onChange={() =>
                            toggle(selectedPassIds, pass.passId, setSelectedPassIds)
                          }
                          label={
                            <div className="small">
                              <div className="fw-semibold">8회권 #{pass.passId}</div>
                              <div className="text-muted-soft">
                                잔여 {pass.remainingCredits}/{pass.totalCredits}회 · {formatKRW(pass.totalPrice)} · 만료 {formatDateTime(pass.expiresAt)}
                              </div>
                            </div>
                          }
                        />
                      ))}
                    </Stack>
                  </div>
                )}

                {(preview.bookings.length > 0 || preview.passes.length > 0) && (
                  <Alert variant="light" className="mb-0 small">
                    8회권 예약을 함께 선택하면 연결된 8회권도 자동으로 회원 계정으로 이전됩니다.
                  </Alert>
                )}
              </Stack>
            )}
          </>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onClose}>
          닫기
        </Button>
        <Button
          variant="primary"
          disabled={
            needsPhoneVerification ||
            !preview ||
            totalSelectedCount === 0 ||
            claimMutation.isPending
          }
          onClick={() => claimMutation.mutate()}
        >
          {claimMutation.isPending ? "가져오는 중..." : "선택한 이력 가져오기"}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
