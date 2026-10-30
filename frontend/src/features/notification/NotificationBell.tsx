import { useEffect, useRef, useState } from "react";
import { Badge, Button, Card, Nav } from "react-bootstrap";
import { Bell } from "lucide-react";
import { Link } from "react-router";
import { NotificationContext } from "./NotificationContext";
import { notificationTarget } from "./notificationTarget";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { useUnreadCount, useNotificationList, useReadNotifications } from "./useNotifications";
import { NOTIFICATION_EVENT_LABEL } from "@/shared/lib";
import { formatRelativeTime } from "./formatRelativeTime";

const POPOVER_ID = "customer-notification-popover";

export function NotificationBell() {
  const { sessionVersion } = useCustomerAuth();
  return <NotificationBellContent key={sessionVersion} />;
}

function NotificationBellContent() {
  const { isAuthenticated } = useCustomerAuth();
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const popoverRef = useRef<HTMLDivElement>(null);

  const unreadQuery = useUnreadCount(isAuthenticated);
  const notificationQuery = useNotificationList(0, isAuthenticated && open);
  const unreadCount = unreadQuery.data;
  const notifications = notificationQuery.data ?? [];
  const readMutation = useReadNotifications();
  const loadError = notificationQuery.error ?? unreadQuery.error;

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }

    function handleEscape(e: KeyboardEvent) {
      if (e.key !== "Escape") return;
      setOpen(false);
      triggerRef.current?.focus();
    }

    if (!open) return;
    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleEscape);
    const focusFrame = window.requestAnimationFrame(() => popoverRef.current?.focus());

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleEscape);
      window.cancelAnimationFrame(focusFrame);
    };
  }, [open]);

  if (!isAuthenticated) return null;

  return (
    <div ref={containerRef} className="position-relative d-inline-block">
      <Nav.Link
        as="button"
        ref={triggerRef}
        className="app-nav-link position-relative btn btn-link p-0 border-0"
        onClick={() => setOpen((v) => !v)}
        aria-label={unreadQuery.error ? "알림, 읽지 않은 알림 수 확인 실패" : "알림"}
        aria-expanded={open}
        aria-controls={POPOVER_ID}
        aria-haspopup="dialog"
      >
        <Bell size={21} strokeWidth={1.8} aria-hidden="true" />
        <span aria-hidden="true">알림</span>
        {unreadCount !== undefined && unreadCount > 0 && (
          <Badge
            bg="dark"
            className="position-absolute top-0 start-50 ms-2 translate-middle-y"
            style={{ fontSize: "0.65rem" }}
          >
            {unreadCount > 99 ? "99+" : unreadCount}
          </Badge>
        )}
        {unreadQuery.error && unreadCount === undefined && (
          <Badge
            bg="warning"
            text="dark"
            className="position-absolute top-0 start-50 ms-2 translate-middle-y"
            aria-hidden="true"
          >
            !
          </Badge>
        )}
      </Nav.Link>

      {open && (
        <Card
          ref={popoverRef}
          id={POPOVER_ID}
          role="dialog"
          aria-label="알림 목록"
          tabIndex={-1}
          className="notification-popover position-absolute end-0"
        >
          <Card.Header className="d-flex justify-content-between align-items-center py-2 px-3">
            <span className="fw-semibold small">알림</span>
            {unreadCount !== undefined && unreadCount > 0 && (
              <Button
                variant="link"
                size="sm"
                className="p-0 text-decoration-none"
                disabled={readMutation.isPending}
                onClick={() => readMutation.mutate({})}
              >
                {readMutation.isPending && readMutation.variables?.id === undefined ? "읽음 처리 중..." : "모두 읽음"}
              </Button>
            )}
          </Card.Header>
          <Card.Body className="notification-popover-body p-0">
            {readMutation.isError && (
              <div className="px-3 pt-3">
                <ErrorAlert
                  error={readMutation.error}
                  onRetry={() => readMutation.mutate({ id: readMutation.variables?.id })}
                  retrying={readMutation.isPending}
                />
              </div>
            )}
            {/* 읽지 않은 수·목록 조회 실패는 다른 오류 안내와 같은 모양으로 한 번에 다시 시도한다. */}
            {loadError && (
              <div className="px-3 pt-3">
                <ErrorAlert
                  error={loadError}
                  onRetry={() => {
                    if (unreadQuery.error) void unreadQuery.refetch();
                    if (notificationQuery.error) void notificationQuery.refetch();
                  }}
                  retrying={unreadQuery.isFetching || notificationQuery.isFetching}
                />
              </div>
            )}
            {notificationQuery.isPending ? (
              <LoadingSpinner text="알림을 불러오는 중..." />
            ) : notifications.length === 0 && !notificationQuery.error ? (
              <EmptyState message="알림이 없습니다." />
            ) : (
              notifications.map((notification) => {
                const target = notificationTarget(notification);
                const actionable = Boolean(target) || !notification.read;
                const content = (
                  <>
                    <div className="fw-semibold">
                      {NOTIFICATION_EVENT_LABEL[notification.eventType] ?? "알림 내용 확인 필요"}
                    </div>
                    <NotificationContext notification={notification} />
                    <div className="text-muted" style={{ fontSize: "0.75rem" }}>
                      {formatRelativeTime(notification.deliveredAt)}
                    </div>
                  </>
                );
                const className = `d-block w-100 text-start px-3 py-2 border-0 border-bottom small ${notification.read ? "bg-transparent" : "bg-light"}`;

                // 이동할 화면이 있는 알림은 링크로 열고(새 탭 열기·주소 복사 가능), 여는 순간 읽음 처리한다.
                if (target) {
                  return (
                    <Link
                      key={notification.id}
                      to={target}
                      className={`${className} text-reset text-decoration-none`}
                      onClick={() => {
                        if (!notification.read && !readMutation.isPending) {
                          readMutation.mutate({ id: notification.id, notifyOnError: true });
                        }
                        setOpen(false);
                      }}
                    >
                      {content}
                    </Link>
                  );
                }

                if (!actionable) {
                  return (
                    <div key={notification.id} className={className}>
                      {content}
                    </div>
                  );
                }

                return (
                  <button
                    key={notification.id}
                    type="button"
                    disabled={readMutation.isPending}
                    className={className}
                    style={{ color: "inherit", font: "inherit" }}
                    onClick={() => readMutation.mutate({ id: notification.id, notifyOnError: false })}
                  >
                    {content}
                  </button>
                );
              })
            )}
          </Card.Body>
          <Card.Footer className="text-center"><Link to="/my/notifications" onClick={() => setOpen(false)}>알림 전체 보기</Link></Card.Footer>
        </Card>
      )}
    </div>
  );
}
