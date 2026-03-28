import { useState, useRef, useEffect } from "react";
import { Nav, Badge, Card, Button } from "react-bootstrap";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { useUnreadCount, useNotificationList, useMarkAsRead, useMarkAllAsRead } from "./useNotifications";
import { NOTIFICATION_EVENT_LABEL } from "@/shared/lib";
import { formatRelativeTime } from "./formatRelativeTime";

export function NotificationBell() {
  const { isAuthenticated } = useCustomerAuth();
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
              notifications.map((n) => (
                <div
                  key={n.id}
                  className={`px-3 py-2 border-bottom small ${n.read ? "" : "bg-light"}`}
                  style={{ cursor: n.read ? "default" : "pointer" }}
                  onClick={() => {
                    if (!n.read) markRead.mutate(n.id);
                  }}
                >
                  <div className="fw-semibold">
                    {NOTIFICATION_EVENT_LABEL[n.eventType] ?? n.eventType}
                  </div>
                  <div className="text-muted" style={{ fontSize: "0.75rem" }}>
                    {n.channel} &middot; {formatRelativeTime(n.sentAt)}
                  </div>
                </div>
              ))
            )}
          </Card.Body>
        </Card>
      )}
    </div>
  );
}
