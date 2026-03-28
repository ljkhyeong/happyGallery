export interface NotificationResponse {
  id: number;
  channel: string;
  eventType: string;
  status: string;
  sentAt: string;
  readAt: string | null;
  read: boolean;
}

export interface UnreadCountResponse {
  count: number;
}
