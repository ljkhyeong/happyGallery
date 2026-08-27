import { useEffect, useMemo, useState } from "react";
import { Button, Col, Form, Modal, Row } from "react-bootstrap";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { formatDateTime, parseApiDateTime } from "@/shared/lib";
import { fetchClasses, fetchSlotsByClass } from "@/features/admin-slot/api";
import { createBookingByAdmin } from "./api";
import type {
  AdminBookingResponse,
  CreateAdminBookingRequestSource,
} from "@/generated/api/adminBooking";

interface Props {
  adminKey: string;
  onAuthError: () => void;
  show: boolean;
  onHide: () => void;
  onCreated: (booking: AdminBookingResponse) => void;
}

const SOURCE_OPTIONS: Array<{ value: CreateAdminBookingRequestSource; label: string }> = [
  { value: "PHONE", label: "전화" },
  { value: "NAVER_TALK", label: "네이버톡톡" },
  { value: "KAKAO", label: "카카오톡" },
  { value: "VISIT", label: "방문" },
];

export function AdminBookingCreateModal({
  adminKey,
  onAuthError,
  show,
  onHide,
  onCreated,
}: Props) {
  const [classId, setClassId] = useState("");
  const [slotId, setSlotId] = useState("");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [participantCount, setParticipantCount] = useState(1);
  const [source, setSource] = useState<CreateAdminBookingRequestSource>("PHONE");
  const [depositPaid, setDepositPaid] = useState(false);

  const classesQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "classes"],
    queryFn: () => fetchClasses(adminKey),
    enabled: show,
  });
  const selectedClassId = Number(classId);
  const slotsQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "slots", selectedClassId],
    queryFn: () => fetchSlotsByClass(adminKey, selectedClassId),
    enabled: show && Number.isSafeInteger(selectedClassId) && selectedClassId > 0,
  });
  const selectableSlots = useMemo(
    () => slotsQuery.data
      ?.filter((slot) => slot.isActive
        && parseApiDateTime(slot.startAt) > Date.now()
        && slot.bookedCount < slot.capacity)
      .sort((left, right) => left.startAt.localeCompare(right.startAt)),
    [slotsQuery.data],
  );
  const selectedSlot = selectableSlots?.find((slot) => String(slot.id) === slotId);
  const maximumParticipants = selectedSlot
    ? selectedSlot.capacity - selectedSlot.bookedCount
    : 1;

  useEffect(() => {
    if (participantCount > maximumParticipants) {
      setParticipantCount(1);
    }
  }, [maximumParticipants, participantCount]);

  const createMutation = useAdminMutation(onAuthError, {
    mutationFn: () => createBookingByAdmin(adminKey, {
      slotId: Number(slotId),
      name: name.trim(),
      phone: phone.trim(),
      participantCount,
      source,
      depositPaid,
    }),
    onSuccess: (booking) => {
      resetForm();
      onCreated(booking);
    },
  });

  const resetForm = () => {
    setClassId("");
    setSlotId("");
    setName("");
    setPhone("");
    setParticipantCount(1);
    setSource("PHONE");
    setDepositPaid(false);
    createMutation.reset();
  };
  const close = () => {
    if (createMutation.isPending) return;
    resetForm();
    onHide();
  };
  const formValid = slotsQuery.error === null
    && selectedSlot !== undefined
    && name.trim().length > 0
    && phone.trim().length > 0
    && participantCount >= 1
    && participantCount <= maximumParticipants;

  return (
    <Modal
      show={show}
      aria-labelledby="admin-booking-create-title"
      onHide={close}
      size="lg"
      centered
    >
      <Modal.Header closeButton={!createMutation.isPending}>
        <Modal.Title id="admin-booking-create-title" className="fs-6">
          전화·메신저·방문 예약 등록
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <ErrorAlert error={createMutation.error} />
        <ErrorAlert
          error={classesQuery.error}
          onRetry={() => { void classesQuery.refetch(); }}
          retrying={classesQuery.isFetching}
        />
        <ErrorAlert
          error={slotsQuery.error}
          onRetry={() => { void slotsQuery.refetch(); }}
          retrying={slotsQuery.isFetching}
        />
        {classesQuery.isLoading ? (
          <LoadingSpinner text="클래스를 불러오는 중..." />
        ) : classesQuery.data !== undefined ? (
          <Form>
            <Row className="g-3">
              <Col xs={12} md={6}>
                <Form.Group controlId="admin-manual-booking-class">
                  <Form.Label>클래스</Form.Label>
                  <Form.Select
                    value={classId}
                    disabled={createMutation.isPending}
                    onChange={(event) => {
                      setClassId(event.target.value);
                      setSlotId("");
                      setParticipantCount(1);
                    }}
                  >
                    <option value="">선택하세요</option>
                    {classesQuery.data?.filter((item) => item.status === "ACTIVE").map((item) => (
                      <option key={item.id} value={item.id}>{item.name}</option>
                    ))}
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col xs={12} md={6}>
                <Form.Group controlId="admin-manual-booking-slot">
                  <Form.Label>예약 시간</Form.Label>
                  <Form.Select
                    value={slotId}
                    disabled={
                      !classId
                      || slotsQuery.isLoading
                      || slotsQuery.data === undefined
                      || Boolean(slotsQuery.error)
                      || createMutation.isPending
                    }
                    onChange={(event) => {
                      setSlotId(event.target.value);
                      setParticipantCount(1);
                    }}
                  >
                    <option value="">
                      {slotsQuery.isLoading
                        ? "예약 시간 조회 중..."
                        : slotsQuery.error
                          ? "예약 시간을 다시 조회해 주세요"
                          : slotsQuery.data === undefined
                          ? "예약 시간을 다시 조회해 주세요"
                          : selectableSlots?.length === 0
                          ? "예약 가능한 시간이 없습니다"
                          : "선택하세요"}
                    </option>
                    {selectableSlots?.map((slot) => (
                      <option key={slot.id} value={slot.id}>
                        {formatDateTime(slot.startAt)} · 잔여 {slot.capacity - slot.bookedCount}명
                      </option>
                    ))}
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col xs={12} md={6}>
                <Form.Group controlId="admin-manual-booking-name">
                  <Form.Label>예약자 이름</Form.Label>
                  <Form.Control
                    value={name}
                    maxLength={100}
                    disabled={createMutation.isPending}
                    onChange={(event) => setName(event.target.value)}
                  />
                </Form.Group>
              </Col>
              <Col xs={12} md={6}>
                <Form.Group controlId="admin-manual-booking-phone">
                  <Form.Label>휴대폰 번호</Form.Label>
                  <Form.Control
                    type="tel"
                    value={phone}
                    maxLength={20}
                    placeholder="010-1234-5678"
                    disabled={createMutation.isPending}
                    onChange={(event) => setPhone(event.target.value)}
                  />
                </Form.Group>
              </Col>
              <Col xs={12} md={4}>
                <Form.Group controlId="admin-manual-booking-count">
                  <Form.Label>인원</Form.Label>
                  <Form.Control
                    type="number"
                    min={1}
                    max={maximumParticipants}
                    step={1}
                    value={participantCount}
                    disabled={!selectedSlot || createMutation.isPending}
                    onChange={(event) => setParticipantCount(Number(event.target.value))}
                  />
                </Form.Group>
              </Col>
              <Col xs={12} md={4}>
                <Form.Group controlId="admin-manual-booking-source">
                  <Form.Label>접수 경로</Form.Label>
                  <Form.Select
                    value={source}
                    disabled={createMutation.isPending}
                    onChange={(event) => setSource(
                      event.target.value as CreateAdminBookingRequestSource,
                    )}
                  >
                    {SOURCE_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>{option.label}</option>
                    ))}
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col xs={12} md={4} className="d-flex align-items-end">
                <Form.Check
                  type="switch"
                  id="admin-manual-booking-deposit-paid"
                  label="예약금 입금 완료"
                  checked={depositPaid}
                  disabled={createMutation.isPending}
                  onChange={(event) => setDepositPaid(event.target.checked)}
                />
              </Col>
            </Row>
            <p className="small text-muted-soft mt-3 mb-0">
              금액은 현재 클래스 가격과 인원에 따라 자동 계산됩니다. 직접 받은 예약금이 있는
              예약을 취소하면 고객에게 반환할 금액이 관리자 할 일에 남습니다.
            </p>
          </Form>
        ) : null}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="outline-secondary" disabled={createMutation.isPending} onClick={close}>
          닫기
        </Button>
        <Button
          variant="primary"
          disabled={!formValid || createMutation.isPending}
          onClick={() => createMutation.mutate()}
        >
          {createMutation.isPending ? "등록 중..." : "예약 등록"}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
