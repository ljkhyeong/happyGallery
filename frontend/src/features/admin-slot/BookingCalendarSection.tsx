import { useEffect, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Badge, Button, Col, Form, Row, Stack } from "react-bootstrap";
import { ChevronLeft, ChevronRight, Clock3 } from "lucide-react";
import {
  BookingCalendarDayResponseOverrideMode,
  type UpdateBookingCalendarSettingsRequest,
} from "@/generated/api/adminCatalog";
import {
  createBookingTimeBlock,
  deleteBookingTimeBlock,
  fetchBookingCalendar,
  saveBookingCalendarDay,
  saveBookingCalendarSettings,
} from "./api";
import { ApiError, invalidateSlotAvailability, queryKeys } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import {
  HalfHourDaySchedule,
  type HalfHourScheduleItem,
} from "@/features/admin-calendar/HalfHourDaySchedule";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

interface SettingsForm {
  openTime: string;
  closeTime: string;
  slotIntervalMin: string;
  blockPublicHolidays: boolean;
}

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

function dateKey(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function firstDayOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

function lastDayOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0);
}

export function BookingCalendarSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const today = useMemo(() => dateKey(new Date()), []);
  const [month, setMonth] = useState(() => firstDayOfMonth(new Date()));
  const [selectedDate, setSelectedDate] = useState(today);
  const [dayReason, setDayReason] = useState("");
  const [blockStart, setBlockStart] = useState("12:00");
  const [blockEnd, setBlockEnd] = useState("13:00");
  const [blockReason, setBlockReason] = useState("");
  const [settings, setSettings] = useState<SettingsForm>({
    openTime: "10:00",
    closeTime: "19:00",
    slotIntervalMin: "30",
    blockPublicHolidays: true,
  });

  const dateFrom = dateKey(firstDayOfMonth(month));
  const dateTo = dateKey(lastDayOfMonth(month));
  const calendarQuery = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.slots.calendar(dateFrom, dateTo),
    queryFn: () => fetchBookingCalendar(adminKey, dateFrom, dateTo),
  });

  useEffect(() => {
    if (!calendarQuery.data) return;
    const value = calendarQuery.data.settings;
    setSettings({
      openTime: value.openTime.slice(0, 5),
      closeTime: value.closeTime.slice(0, 5),
      slotIntervalMin: String(value.slotIntervalMin),
      blockPublicHolidays: value.blockPublicHolidays,
    });
  }, [calendarQuery.data]);

  const selectedDay = calendarQuery.data?.days.find((day) => day.date === selectedDate);
  const savedSettings = calendarQuery.data?.settings;
  const selectedDaySchedule: HalfHourScheduleItem[] = selectedDay && savedSettings
    ? selectedDay.effectiveAvailability === "CLOSED"
      ? [{
        id: `closed-${selectedDay.date}`,
        start: savedSettings.openTime,
        end: savedSettings.closeTime,
        title: "종일 닫힘",
        detail: selectedDay.reason ?? undefined,
        tone: "muted",
      }]
      : selectedDay.timeBlocks.map((block) => ({
        id: block.id,
        start: block.startTime,
        end: block.endTime,
        title: "예약 차단",
        detail: block.reason ?? undefined,
        tone: "danger" as const,
      }))
    : [];
  useEffect(() => {
    setDayReason(selectedDay?.reason ?? "");
  }, [selectedDay?.date, selectedDay?.reason]);

  const refreshCalendar = () => {
    void queryClient.invalidateQueries({ queryKey: queryKeys.admin.slots.all });
    void invalidateSlotAvailability(queryClient);
  };

  const settingsMutation = useAdminMutation(onAuthError, {
    mutationFn: (body: UpdateBookingCalendarSettingsRequest) =>
      saveBookingCalendarSettings(adminKey, body),
    onSuccess: () => {
      toast.show("기본 예약 운영시간을 저장했습니다.");
      refreshCalendar();
    },
  });

  const dayMutation = useAdminMutation(onAuthError, {
    mutationFn: (mode: keyof typeof BookingCalendarDayResponseOverrideMode) =>
      saveBookingCalendarDay(adminKey, selectedDate, {
        mode: BookingCalendarDayResponseOverrideMode[mode],
        reason: dayReason.trim() || null,
      }),
    onSuccess: () => {
      toast.show("선택한 날짜의 예약 상태를 저장했습니다.");
      refreshCalendar();
    },
  });

  const createBlockMutation = useAdminMutation(onAuthError, {
    mutationFn: () => createBookingTimeBlock(adminKey, {
      date: selectedDate,
      startTime: blockStart,
      endTime: blockEnd,
      reason: blockReason.trim() || null,
    }),
    onSuccess: () => {
      setBlockReason("");
      toast.show("예약을 받지 않는 시간을 추가했습니다.");
      refreshCalendar();
    },
  });

  const deleteBlockMutation = useAdminMutation(onAuthError, {
    mutationFn: (id: number) => deleteBookingTimeBlock(adminKey, id),
    onSuccess: () => {
      toast.show("시간 차단을 해제했습니다.");
      refreshCalendar();
    },
  });

  const leadingBlankCount = firstDayOfMonth(month).getDay();
  const canEditSelectedDate = selectedDate >= today;
  const mutationError = settingsMutation.error
    ?? dayMutation.error
    ?? createBlockMutation.error
    ?? deleteBlockMutation.error;

  return (
    <div>
      <p className="text-muted-soft mb-4">
        모든 날짜와 시간을 기본으로 열고, 예약을 받을 수 없는 날짜나 시간만 닫습니다.
        실제 예약 회차는 고객이 일정을 조회할 때 자동으로 준비됩니다.
      </p>

      <Form
        className="border rounded-3 p-3 mb-4"
        onSubmit={(event) => {
          event.preventDefault();
          if (!calendarQuery.data) return;
          settingsMutation.mutate({
            expectedVersion: calendarQuery.data.settings.version,
            openTime: settings.openTime,
            closeTime: settings.closeTime,
            slotIntervalMin: Number(settings.slotIntervalMin),
            blockPublicHolidays: settings.blockPublicHolidays,
          });
        }}
      >
        <h6>기본 운영시간</h6>
        <Row className="g-3 align-items-end">
          <Col xs={6} md={3}>
            <Form.Group controlId="booking-calendar-open-time">
              <Form.Label>시작</Form.Label>
              <Form.Control
                type="time"
                value={settings.openTime}
                onChange={(event) => setSettings((current) => ({
                  ...current, openTime: event.target.value,
                }))}
                required
              />
            </Form.Group>
          </Col>
          <Col xs={6} md={3}>
            <Form.Group controlId="booking-calendar-close-time">
              <Form.Label>종료</Form.Label>
              <Form.Control
                type="time"
                value={settings.closeTime}
                onChange={(event) => setSettings((current) => ({
                  ...current, closeTime: event.target.value,
                }))}
                required
              />
            </Form.Group>
          </Col>
          <Col xs={12} md={3}>
            <Form.Group controlId="booking-calendar-interval">
              <Form.Label>예약 시작 간격</Form.Label>
              <Form.Select
                value={settings.slotIntervalMin}
                onChange={(event) => setSettings((current) => ({
                  ...current, slotIntervalMin: event.target.value,
                }))}
              >
                {[10, 15, 20, 30, 60, 90, 120].map((minutes) => (
                  <option key={minutes} value={minutes}>{minutes}분</option>
                ))}
              </Form.Select>
              <Form.Text className="text-muted">시간표 눈금은 설정과 관계없이 30분입니다.</Form.Text>
            </Form.Group>
          </Col>
          <Col xs={12} md={3}>
            <Button type="submit" className="w-100" disabled={settingsMutation.isPending}>
              {settingsMutation.isPending ? "저장 중..." : "기본 운영시간 저장"}
            </Button>
          </Col>
          <Col xs={12}>
            <Form.Check
              id="booking-calendar-public-holidays"
              type="switch"
              label="법정·대체공휴일은 기본으로 닫기"
              checked={settings.blockPublicHolidays}
              onChange={(event) => setSettings((current) => ({
                ...current, blockPublicHolidays: event.target.checked,
              }))}
            />
          </Col>
        </Row>
      </Form>

      <ErrorAlert error={mutationError} />
      {calendarQuery.isLoading && <LoadingSpinner text="예약 캘린더를 불러오는 중..." />}
      {calendarQuery.error
        && !(calendarQuery.error instanceof ApiError && calendarQuery.error.status === 401) && (
        <ErrorAlert
          error={calendarQuery.error}
          onRetry={() => { void calendarQuery.refetch(); }}
          retrying={calendarQuery.isFetching}
        />
      )}

      {calendarQuery.data && (
        <Row className="g-4">
          <Col xs={12} xl={7}>
            <div className="d-flex align-items-center justify-content-between mb-3">
              <Button
                variant="outline-secondary"
                size="sm"
                aria-label="이전 달"
                onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() - 1, 1))}
              >
                <ChevronLeft size={18} />
              </Button>
              <h6 className="mb-0">{month.getFullYear()}년 {month.getMonth() + 1}월</h6>
              <Button
                variant="outline-secondary"
                size="sm"
                aria-label="다음 달"
                onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() + 1, 1))}
              >
                <ChevronRight size={18} />
              </Button>
            </div>
            <div
              className="booking-calendar-grid"
              style={{ display: "grid", gridTemplateColumns: "repeat(7, minmax(0, 1fr))", gap: 6 }}
            >
              {WEEKDAYS.map((weekday) => (
                <div key={weekday} className="text-center small text-muted py-1">{weekday}</div>
              ))}
              {Array.from({ length: leadingBlankCount }, (_, index) => (
                <span key={`blank-${index}`} aria-hidden="true" />
              ))}
              {calendarQuery.data.days.map((day) => {
                const selected = day.date === selectedDate;
                const closed = day.effectiveAvailability === "CLOSED";
                const dayNumber = Number(day.date.slice(-2));
                return (
                  <Button
                    key={day.date}
                    type="button"
                    variant={selected ? "primary" : closed ? "outline-danger" : "outline-secondary"}
                    className="d-flex flex-column align-items-center justify-content-center gap-1 px-1"
                    style={{ minHeight: 70 }}
                    onClick={() => setSelectedDate(day.date)}
                  >
                    <span>{dayNumber}</span>
                    <small>{closed ? "닫힘" : "예약 가능"}</small>
                    {day.publicHoliday && <Badge bg={selected ? "light" : "danger"} text={selected ? "dark" : undefined}>공휴일</Badge>}
                  </Button>
                );
              })}
            </div>
          </Col>

          <Col xs={12} xl={5}>
            {!selectedDay && <EmptyState message="달력에서 관리할 날짜를 선택하세요." />}
            {selectedDay && (
              <div className="border rounded-3 p-3">
                <div className="d-flex align-items-center justify-content-between gap-2 mb-3">
                  <h6 className="mb-0">{selectedDay.date}</h6>
                  <Stack direction="horizontal" gap={2}>
                    {selectedDay.publicHoliday && <Badge bg="danger">공휴일</Badge>}
                    <Badge bg={selectedDay.effectiveAvailability === "OPEN" ? "success" : "secondary"}>
                      {selectedDay.effectiveAvailability === "OPEN" ? "예약 가능" : "닫힘"}
                    </Badge>
                  </Stack>
                </div>

                <HalfHourDaySchedule
                  ariaLabel={`${selectedDay.date} 예약 운영 시간표`}
                  date={selectedDay.date}
                  startTime={calendarQuery.data.settings.openTime}
                  endTime={calendarQuery.data.settings.closeTime}
                  items={selectedDaySchedule}
                  emptyMessage="기본 운영시간 전체가 예약 가능 상태입니다."
                />

                <Form.Group controlId="booking-calendar-day-reason" className="mb-3">
                  <Form.Label>사유</Form.Label>
                  <Form.Control
                    maxLength={200}
                    value={dayReason}
                    disabled={!canEditSelectedDate}
                    onChange={(event) => setDayReason(event.target.value)}
                    placeholder="휴무, 외부 일정 등"
                  />
                </Form.Group>
                <div className="d-flex flex-wrap gap-2 mb-4">
                  <Button
                    size="sm"
                    variant="outline-secondary"
                    disabled={!canEditSelectedDate || dayMutation.isPending}
                    onClick={() => dayMutation.mutate("DEFAULT")}
                  >
                    기본값 사용
                  </Button>
                  <Button
                    size="sm"
                    variant="outline-success"
                    disabled={!canEditSelectedDate || dayMutation.isPending}
                    onClick={() => dayMutation.mutate("OPEN")}
                  >
                    이 날짜 열기
                  </Button>
                  <Button
                    size="sm"
                    variant="outline-danger"
                    disabled={!canEditSelectedDate || dayMutation.isPending}
                    onClick={() => dayMutation.mutate("CLOSED")}
                  >
                    종일 닫기
                  </Button>
                </div>

                <h6 className="d-flex align-items-center gap-2">
                  <Clock3 size={16} /> 시간만 닫기
                </h6>
                <Row className="g-2 mb-2">
                  <Col xs={6}>
                    <Form.Control
                      aria-label="차단 시작 시각"
                      type="time"
                      value={blockStart}
                      disabled={!canEditSelectedDate}
                      onChange={(event) => setBlockStart(event.target.value)}
                    />
                  </Col>
                  <Col xs={6}>
                    <Form.Control
                      aria-label="차단 종료 시각"
                      type="time"
                      value={blockEnd}
                      disabled={!canEditSelectedDate}
                      onChange={(event) => setBlockEnd(event.target.value)}
                    />
                  </Col>
                  <Col xs={12}>
                    <Form.Control
                      maxLength={200}
                      value={blockReason}
                      disabled={!canEditSelectedDate}
                      onChange={(event) => setBlockReason(event.target.value)}
                      placeholder="점심시간, 재료 준비 등"
                    />
                  </Col>
                  <Col xs={12}>
                    <Button
                      size="sm"
                      className="w-100"
                      disabled={!canEditSelectedDate || createBlockMutation.isPending}
                      onClick={() => createBlockMutation.mutate()}
                    >
                      {createBlockMutation.isPending ? "추가 중..." : "이 시간 닫기"}
                    </Button>
                  </Col>
                </Row>

                {selectedDay.timeBlocks.length === 0 ? (
                  <p className="small text-muted mb-0">닫아 둔 시간이 없습니다.</p>
                ) : (
                  <Stack gap={2} className="mt-3">
                    {selectedDay.timeBlocks.map((block) => (
                      <div
                        key={block.id}
                        className="d-flex align-items-center justify-content-between border rounded-2 px-3 py-2"
                      >
                        <span className="small">
                          {block.startTime.slice(0, 5)}–{block.endTime.slice(0, 5)}
                          {block.reason && <span className="text-muted ms-2">{block.reason}</span>}
                        </span>
                        <Button
                          size="sm"
                          variant="outline-danger"
                          disabled={deleteBlockMutation.isPending}
                          onClick={() => deleteBlockMutation.mutate(block.id)}
                        >
                          해제
                        </Button>
                      </div>
                    ))}
                  </Stack>
                )}
              </div>
            )}
          </Col>
        </Row>
      )}
    </div>
  );
}
