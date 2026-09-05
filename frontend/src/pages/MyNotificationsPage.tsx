import { Button, Card, Form } from "react-bootstrap";
import { Link, useSearchParams } from "react-router";
import { NotificationContext } from "@/features/notification/NotificationContext";
import { MySectionPage } from "@/features/my/MySectionPage";
import { useMarkAllAsRead, useMarkAsRead, useNotificationList, useUnreadCount } from "@/features/notification/useNotifications";
import { notificationTarget } from "@/features/notification/notificationTarget";
import { runForCurrentCustomer } from "@/shared/api";
import { formatDateTime, NOTIFICATION_EVENT_LABEL } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";

export function MyNotificationsPage() {
  return <MySectionPage title="전체 알림"><NotificationInbox /></MySectionPage>;
}

function NotificationInbox() {
  const [params, setParams] = useSearchParams();
  const rawPage = Number(params.get("page") ?? "0");
  const page = Number.isSafeInteger(rawPage) && rawPage >= 0 ? rawPage : 0;
  const unreadOnly = params.get("unreadOnly") === "true";
  const query = useNotificationList(page, true, unreadOnly);
  const count = useUnreadCount(true);
  const markRead = useMarkAsRead();
  const markAll = useMarkAllAsRead();
  const pending = markRead.isPending || markAll.isPending;
  const update = (nextPage: number, unread = unreadOnly) => {
    const next = new URLSearchParams();
    if (nextPage > 0) next.set("page", String(nextPage));
    if (unread) next.set("unreadOnly", "true");
    setParams(next);
  };
  const read = (id?: number) => {
    void runForCurrentCustomer(
      () => id === undefined ? markAll.mutateAsync() : markRead.mutateAsync(id),
      () => { if (unreadOnly) update(0); },
    ).catch(() => { /* 요청 실패는 아래 오류 안내에서 표시한다. */ });
  };
  return (
    <>
      <div className="d-flex flex-wrap justify-content-between gap-3 mb-3">
        <Form.Check id="unread-notifications" label="읽지 않은 알림만" checked={unreadOnly}
          disabled={pending} onChange={(event) => update(0, event.target.checked)} />
        <Button size="sm" variant="outline-secondary" disabled={pending || count.data === 0}
          onClick={() => read()}>모두 읽음</Button>
      </div>
      <ErrorAlert error={query.error} onRetry={() => { void query.refetch(); }} />
      <ErrorAlert error={markRead.error ?? markAll.error} />
      {query.isLoading && <LoadingSpinner />}
      {query.data?.length === 0 && <EmptyState message={unreadOnly ? "읽지 않은 알림이 없습니다." : "표시할 알림이 없습니다."} />}
      {query.data?.map((notification) => {
        const target = notificationTarget(notification);
        const label = NOTIFICATION_EVENT_LABEL[notification.eventType] ?? "알림 내용 확인 필요";
        return (
          <Card key={notification.id} className="mb-2"><Card.Body>
            <div className="d-flex gap-3 justify-content-between align-items-start">
              <div className="flex-grow-1" style={{ minWidth: 0 }}>
                {target ? <Link to={target} onClick={() => { if (!notification.read) markRead.mutate(notification.id); }}>{label}</Link> : <strong>{label}</strong>}
                <NotificationContext notification={notification} />
                <div className="small text-muted mt-1">{formatDateTime(notification.deliveredAt)} · {notification.read ? "읽음" : "읽지 않음"}</div>
              </div>
              {!notification.read && <Button size="sm" variant="outline-primary" disabled={pending}
                aria-label={`${label} 알림 ${notification.id} 읽음 처리`} onClick={() => read(notification.id)}>읽음 처리</Button>}
            </div>
          </Card.Body></Card>
        );
      })}
      <nav aria-label="알림 페이지" className="d-flex align-items-center gap-3 mt-3">
        <Button size="sm" variant="outline-secondary" disabled={page === 0 || query.isFetching || pending} onClick={() => update(page - 1)}>이전</Button>
        <span>{page + 1}페이지</span>
        <Button size="sm" variant="outline-secondary" disabled={query.data?.length !== 20 || query.isFetching || pending} onClick={() => update(page + 1)}>다음</Button>
      </nav>
    </>
  );
}
