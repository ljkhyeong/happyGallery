import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  captureCustomerSession,
  isCurrentCustomerSession,
  queryKeys,
  requireCurrentCustomerSession,
  runForCurrentCustomer,
  type CustomerSessionSnapshot,
} from "@/shared/api";
import { useToast } from "@/shared/ui";
import { fetchNotifications, fetchUnreadCount, markAsRead, markAllAsRead } from "./api";

export function useUnreadCount(enabled: boolean) {
  return useQuery({
    queryKey: queryKeys.member.notifications.unreadCount,
    queryFn: fetchUnreadCount,
    enabled,
    refetchInterval: 30_000,
    select: (data) => data.count,
  });
}

export function useNotificationList(page: number, enabled: boolean, unreadOnly = false) {
  return useQuery({
    queryKey: queryKeys.member.notifications.page(page, unreadOnly),
    queryFn: ({ signal }) => runForCurrentCustomer(() => fetchNotifications(page, unreadOnly, signal)),
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
      () => queryClient.invalidateQueries({ queryKey: queryKeys.member.notifications.all }),
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
