import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  captureCustomerSession,
  isCurrentCustomerSession,
  requireCurrentCustomerSession,
  runForCurrentCustomer,
  type CustomerSessionSnapshot,
} from "@/shared/api";
import { useToast } from "@/shared/ui";
import { fetchNotifications, fetchUnreadCount, markAsRead, markAllAsRead } from "./api";

const NOTIFICATION_KEY = ["me", "notifications"] as const;
const UNREAD_KEY = ["me", "notifications", "unread-count"] as const;

export function useUnreadCount(enabled: boolean) {
  return useQuery({
    queryKey: [...UNREAD_KEY],
    queryFn: fetchUnreadCount,
    enabled,
    refetchInterval: 30_000,
    select: (data) => data.count,
  });
}

export function useNotificationList(page: number, enabled: boolean, unreadOnly = false) {
  return useQuery({
    queryKey: [...NOTIFICATION_KEY, page, unreadOnly],
    queryFn: ({ signal }) => runForCurrentCustomer(() => fetchNotifications(page, 20, unreadOnly, signal)),
    enabled,
  });
}

interface NotificationReadRequest {
  id?: number;
  notifyOnError?: boolean;
}

interface OwnedNotificationReadRequest extends NotificationReadRequest {
  customerSession: CustomerSessionSnapshot;
}

export function useReadNotifications() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const mutation = useMutation({
    mutationFn: ({ id, customerSession }: OwnedNotificationReadRequest) => runForCurrentCustomer(
      () => {
        requireCurrentCustomerSession(customerSession);
        return id === undefined ? markAllAsRead() : markAsRead(id);
      },
      () => queryClient.invalidateQueries({ queryKey: NOTIFICATION_KEY }),
    ),
    onError: (_error, request) => {
      if (request.notifyOnError && isCurrentCustomerSession(request.customerSession)) {
        toast.show("알림을 읽음 처리하지 못했습니다. 알림 목록에서 다시 시도해 주세요.", "danger");
      }
    },
  });
  return {
    ...mutation,
    mutate: (request: NotificationReadRequest) => mutation.mutate({
      ...request, customerSession: captureCustomerSession(),
    }),
    mutateAsync: (request: NotificationReadRequest) => mutation.mutateAsync({
      ...request, customerSession: captureCustomerSession(),
    }),
  };
}
