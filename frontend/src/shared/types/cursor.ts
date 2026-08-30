import type { AdminOrderPageResponse } from "@/generated/api/adminOrder";

export type CursorPage<T> = Omit<AdminOrderPageResponse, "content"> & {
  content: T[];
};
