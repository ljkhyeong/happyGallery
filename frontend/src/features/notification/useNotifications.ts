import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
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

export function useNotificationList(page: number, enabled: boolean) {
  return useQuery({
    queryKey: [...NOTIFICATION_KEY, page],
    queryFn: () => fetchNotifications(page),
    enabled,
  });
}

export function useMarkAsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [...NOTIFICATION_KEY] });
      queryClient.invalidateQueries({ queryKey: [...UNREAD_KEY] });
    },
  });
}

export function useMarkAllAsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: markAllAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [...NOTIFICATION_KEY] });
      queryClient.invalidateQueries({ queryKey: [...UNREAD_KEY] });
    },
  });
}
