import { useEffect, useRef, useState } from "react";
import { Badge, Button, Card, Nav } from "react-bootstrap";
import { Bell } from "lucide-react";
import { useNavigate } from "react-router";
import type { NotificationResponse } from "@/generated/api/notification";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { LoadingSpinner } from "@/shared/ui/LoadingSpinner";
import { useUnreadCount, useNotificationList, useMarkAsRead, useMarkAllAsRead } from "./useNotifications";
import { NOTIFICATION_EVENT_LABEL } from "@/shared/lib";
import { formatRelativeTime } from "./formatRelativeTime";

const POPOVER_ID = "customer-notification-popover";

export function NotificationBell() {
  const { isAuthenticated } = useCustomerAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const popoverRef = useRef<HTMLDivElement>(null);

  const unreadQuery = useUnreadCount(isAuthenticated);
  const notificationQuery = useNotificationList(0, isAuthenticated && open);
  const unreadCount = unreadQuery.data;
  const notifications = notificationQuery.data ?? [];
  const markRead = useMarkAsRead();
  const markAllRead = useMarkAllAsRead();

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
        <Bell size={20} aria-hidden="true" />
        {unreadCount !== undefined && unreadCount > 0 && (
          <Badge
            bg="danger"
            pill
            className="position-absolute top-0 start-100 translate-middle"
            style={{ fontSize: "0.65rem" }}
          >
            {unreadCount > 99 ? "99+" : unreadCount}
          </Badge>
        )}
        {unreadQuery.error && unreadCount === undefined && (
          <Badge
            bg="warning"
            text="dark"
            pill
            className="position-absolute top-0 start-100 translate-middle"
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
          className="notification-popover position-absolute end-0 shadow-sm"
        >
          <Card.Header className="d-flex justify-content-between align-items-center py-2 px-3">
            <span className="fw-semibold small">알림</span>
            {unreadCount !== undefined && unreadCount > 0 && (
              <Button
                variant="link"
                size="sm"
                className="p-0 text-decoration-none"
                onClick={() => markAllRead.mutate()}
              >
                모두 읽음
              </Button>
            )}
          </Card.Header>
          <Card.Body className="notification-popover-body p-0">
            {unreadQuery.error && (
              <div className="d-flex align-items-center justify-content-between gap-2 border-bottom px-3 py-2">
                <span className="small text-danger">읽지 않은 알림 수를 확인하지 못했습니다.</span>
                <Button
                  type="button"
                  variant="outline-danger"
                  size="sm"
                  disabled={unreadQuery.isFetching}
                  onClick={() => void unreadQuery.refetch()}
                >
                  {unreadQuery.isFetching ? "확인 중..." : "다시 시도"}
                </Button>
              </div>
            )}
            {notificationQuery.error && (
              <div className="d-flex align-items-center justify-content-between gap-2 border-bottom px-3 py-2">
                <span className="small text-danger">
                  {notificationQuery.data
                    ? "새 알림을 불러오지 못했습니다."
                    : "알림을 불러오지 못했습니다."}
                </span>
                <Button
                  type="button"
                  variant="outline-danger"
                  size="sm"
                  disabled={notificationQuery.isFetching}
                  onClick={() => void notificationQuery.refetch()}
                >
                  다시 시도
                </Button>
              </div>
            )}
            {notificationQuery.isPending ? (
              <LoadingSpinner text="알림을 불러오는 중..." />
            ) : notifications.length === 0 && !notificationQuery.error ? (
              <div className="text-center text-muted py-4 small">알림이 없습니다.</div>
            ) : (
              notifications.map((notification) => {
                const target = notificationTarget(notification);
                const actionable = Boolean(target) || !notification.read;
                const content = (
                  <>
                    <div className="fw-semibold">
                      {NOTIFICATION_EVENT_LABEL[notification.eventType] ?? notification.eventType}
                    </div>
                    <div className="text-muted" style={{ fontSize: "0.75rem" }}>
                      {formatRelativeTime(notification.deliveredAt)}
                    </div>
                  </>
                );
                const className = `w-100 text-start px-3 py-2 border-0 border-bottom small ${notification.read ? "bg-transparent" : "bg-light"}`;

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
                    className={className}
                    style={{ color: "inherit", font: "inherit" }}
                    onClick={() => {
                      if (!notification.read) markRead.mutate(notification.id);
                      if (target) {
                        setOpen(false);
                        navigate(target);
                      }
                    }}
                  >
                    {content}
                  </button>
                );
              })
            )}
          </Card.Body>
        </Card>
      )}
    </div>
  );
}

function notificationTarget(notification: NotificationResponse): string | null {
  const aggregateId = notification.aggregateId;
  switch (notification.aggregateType) {
    case "ORDER":
      return aggregateId ? `/my/orders/${aggregateId}` : "/my/orders";
    case "BOOKING":
      return aggregateId ? `/my/bookings/${aggregateId}` : "/my/bookings";
    case "PASS_PURCHASE":
      return "/my/passes";
    case "INQUIRY":
      return "/my/inquiries";
    case "REVIEW":
    case "REVIEW_MODERATION_ACTION":
      return "/my/reviews";
    default:
      return fallbackTarget(notification.eventType);
  }
}

function fallbackTarget(eventType: string): string | null {
  if (eventType.startsWith("ORDER_") || eventType === "ORDER_REFUNDED") return "/my/orders";
  if (eventType.startsWith("BOOKING_") || eventType === "DEPOSIT_REFUNDED"
    || eventType.startsWith("REMINDER_")) return "/my/bookings";
  if (eventType.startsWith("PASS_")) return "/my/passes";
  if (eventType.startsWith("REVIEW_")) return "/my/reviews";
  return null;
}
