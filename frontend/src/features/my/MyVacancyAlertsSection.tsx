import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Badge, Button, Card } from "react-bootstrap";
import { Link } from "react-router";
import {
  cancelMyVacancyAlert,
  fetchMyVacancyAlerts,
  type VacancyAlertResponse,
} from "@/features/vacancy-alert/api";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { formatDateTime } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";

export function MyVacancyAlertsSection() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const {
    data: alerts,
    isLoading,
    isFetching,
    error,
    refetch,
  } = useQuery({
    queryKey: queryKeys.member.vacancyAlerts,
    queryFn: ({ signal }) => runForCurrentCustomer(() => fetchMyVacancyAlerts(signal)),
  });
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
          <h6 className="mb-1">내 빈자리 알림</h6>
          <p className="text-muted-soft small mb-0">
            신청한 일정의 잔여 좌석을 확인할 수 있습니다. 알림으로 자리가 확보되지는 않습니다.
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
        return (
          <Card key={alert.alertId} className="mb-2 my-list-card border-0">
            <Card.Body className="d-flex flex-wrap justify-content-between align-items-center gap-3 py-3 px-3">
              <div>
                <div className="d-flex flex-wrap align-items-center gap-2 mb-1">
                  <span className="fw-semibold small">{alert.className}</span>
                  <Badge bg={alert.status === "WAITING" ? "secondary" : "info"} className="badge-sm">
                    {alert.status === "WAITING" ? "알림 대기" : "빈자리 발생"}
                  </Badge>
                </div>
                <small className="text-muted-soft">
                  {formatDateTime(alert.startAt)} ~ {formatDateTime(alert.endAt)}
                </small>
              </div>
              <div className="d-flex flex-wrap gap-2">
                <Link
                  to={`/bookings/new?classId=${alert.classId}&slotId=${alert.slotId}`}
                  className="btn btn-outline-primary btn-sm"
                  aria-label={`${alert.className} 예약 가능 여부 확인`}
                >
                  예약 가능 여부 확인
                </Link>
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
