import { useState, useRef, useEffect } from "react";
import { Nav, Badge, Card, Button } from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import type { NotificationResponse } from "@/generated/api/notification";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { useUnreadCount, useNotificationList, useMarkAsRead, useMarkAllAsRead } from "./useNotifications";
import { NOTIFICATION_EVENT_LABEL } from "@/shared/lib";
import { formatRelativeTime } from "./formatRelativeTime";

export function NotificationBell() {
  const { isAuthenticated } = useCustomerAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const { data: unreadCount = 0 } = useUnreadCount(isAuthenticated);
  const { data: notifications = [] } = useNotificationList(0, isAuthenticated && open);
  const markRead = useMarkAsRead();
  const markAllRead = useMarkAllAsRead();

  // 외부 클릭 시 닫기
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    if (open) document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [open]);

  if (!isAuthenticated) return null;

  return (
    <div ref={ref} className="position-relative d-inline-block">
      <Nav.Link
        as="button"
        className="app-nav-link position-relative btn btn-link p-0 border-0"
        onClick={() => setOpen((v) => !v)}
        aria-label="알림"
      >
        <span>&#128276;</span>
        {unreadCount > 0 && (
          <Badge
            bg="danger"
            pill
            className="position-absolute top-0 start-100 translate-middle"
            style={{ fontSize: "0.65rem" }}
          >
            {unreadCount > 99 ? "99+" : unreadCount}
          </Badge>
        )}
      </Nav.Link>

      {open && (
        <Card
          className="position-absolute end-0 shadow-sm"
          style={{ width: 320, maxHeight: 400, overflowY: "auto", zIndex: 1050, top: "100%" }}
        >
          <Card.Header className="d-flex justify-content-between align-items-center py-2 px-3">
            <span className="fw-semibold small">알림</span>
            {unreadCount > 0 && (
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
          <Card.Body className="p-0">
            {notifications.length === 0 ? (
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
    default:
      return fallbackTarget(notification.eventType);
  }
}

function fallbackTarget(eventType: string): string | null {
  if (eventType.startsWith("ORDER_") || eventType === "ORDER_REFUNDED") return "/my/orders";
  if (eventType.startsWith("BOOKING_") || eventType === "DEPOSIT_REFUNDED"
    || eventType.startsWith("REMINDER_")) return "/my/bookings";
  if (eventType.startsWith("PASS_")) return "/my/passes";
  return null;
}
