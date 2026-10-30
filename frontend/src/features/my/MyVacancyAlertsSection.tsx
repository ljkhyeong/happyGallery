import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Card } from "react-bootstrap";
import { Link } from "react-router";
import {
  cancelMyVacancyAlert,
  myVacancyAlertsQuery,
  type VacancyAlertResponse,
} from "@/features/vacancy-alert/api";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { formatDateTime } from "@/shared/lib";
import { EmptyState, ErrorAlert, LinkButton, LoadingSpinner, ToneBadge, useToast, type CustomerBadgeTone } from "@/shared/ui";
import { bookingCreateHref } from "@/features/booking-create/upcomingSlots";

interface AlertStatusBadge {
  label: string;
  tone: CustomerBadgeTone;
}

// 빈자리를 기다리는 신청은 먹색, 빈자리가 나 고객이 예약할 차례인 신청은 노랑이다.
const STATUS_BADGES: Record<VacancyAlertResponse["status"], AlertStatusBadge> = {
  WAITING: { label: "알림 대기", tone: "progress" },
  NOTIFIED: { label: "빈자리 발생", tone: "action" },
  CANCELED: { label: "신청 취소", tone: "done" },
};
const UNKNOWN_STATUS_BADGE: AlertStatusBadge = { label: "상태 확인 필요", tone: "done" };

export function MyVacancyAlertsSection() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const {
    data: alerts,
    isLoading,
    isFetching,
    error,
    refetch,
  } = useQuery(myVacancyAlertsQuery());
  const cancelMutation = useMutation({
    mutationFn: (alert: VacancyAlertResponse) => runForCurrentCustomer(
      () => cancelMyVacancyAlert(alert.slotId),
      (_, requireCurrent) => {
        queryClient.setQueryData<VacancyAlertResponse[]>(
          queryKeys.member.vacancyAlerts,
          (current = []) => current.filter((item) => item.alertId !== alert.alertId),
        );
        requireCurrent();
        toast.show("빈자리 알림 신청을 취소했습니다.", "info");
      },
    ),
  });

  return (
    <section id="my-vacancy-alerts" className="mb-4">
      <div className="d-flex justify-content-between align-items-center mb-2">
        <div>
          <p className="text-muted-soft small mb-0">
            남은 자리는 ‘예약 가능 여부 확인’에서 볼 수 있습니다. 알림을 받아도 자리가 잡히지는 않습니다.
          </p>
        </div>
        {alerts && <span className="text-muted-soft small text-nowrap">
          대기 중 {alerts.filter((alert) => alert.status === "WAITING").length}건
        </span>}
      </div>

      {isLoading && <LoadingSpinner />}
      <ErrorAlert
        error={error}
        onRetry={() => { void refetch(); }}
        retrying={isFetching}
      />
      <ErrorAlert error={cancelMutation.error} />
      {alerts && alerts.length === 0 && (
        <EmptyState message="빈자리 알림 내역이 없습니다." />
      )}
      {alerts?.map((alert) => {
        const canceling = cancelMutation.isPending
          && cancelMutation.variables?.alertId === alert.alertId;
        const status = STATUS_BADGES[alert.status] ?? UNKNOWN_STATUS_BADGE;
        return (
          <Card key={alert.alertId} className="mb-2 my-list-card">
            <Card.Body className="d-flex flex-wrap justify-content-between align-items-center gap-3 py-3 px-3">
              <div>
                <div className="d-flex flex-wrap align-items-center gap-2 mb-1">
                  <Link to={`/classes/${alert.classId}`} className="fw-semibold small">{alert.className}</Link>
                  <ToneBadge tone={status.tone} className="badge-sm">{status.label}</ToneBadge>
                </div>
                <small className="text-muted-soft">
                  {formatDateTime(alert.startAt)} ~ {formatDateTime(alert.endAt)}
                </small>
              </div>
              <div className="d-flex flex-wrap gap-2">
                <LinkButton
                  to={bookingCreateHref(alert.classId, alert.slotId)}
                  variant="outline-primary"
                  size="sm"
                  aria-label={`${alert.className} 예약 가능 여부 확인`}
                >
                  예약 가능 여부 확인
                </LinkButton>
                {alert.status === "WAITING" && (
                  <Button
                    type="button"
                    size="sm"
                    variant="outline-secondary"
                    disabled={cancelMutation.isPending}
                    aria-label={`${alert.className} 빈자리 알림 취소`}
                    onClick={() => cancelMutation.mutate(alert)}
                  >
                    {canceling ? "취소 중..." : "신청 취소"}
                  </Button>
                )}
              </div>
            </Card.Body>
          </Card>
        );
      })}
    </section>
  );
}
