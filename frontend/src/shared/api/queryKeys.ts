import type { QueryKey } from "@tanstack/react-query";

export const queryKeys = {
  admin: {
    all: ["admin"] as const,
    classes: ["admin", "classes"] as const,
    slots: {
      all: ["admin", "slots"] as const,
      byClass: (classId: number) => ["admin", "slots", classId] as const,
    },
    bookings: ["admin", "bookings"] as const,
    bookingCancellationTasks: ["admin", "bookings", "cancellation-tasks"] as const,
  },
  catalog: {
    classes: ["classes"] as const,
  },
  member: {
    orders: {
      all: ["my", "orders"] as const,
      detail: (orderId: number) => ["my", "orders", orderId] as const,
    },
    bookings: {
      all: ["my", "bookings"] as const,
      detail: (bookingId: number) => ["my", "bookings", bookingId] as const,
    },
    passes: ["my", "passes"] as const,
  },
  slotAvailability: {
    upcoming: {
      all: ["upcoming-slots"] as const,
      byClass: (classId: number, days: number) =>
        ["upcoming-slots", classId, days] as const,
    },
    reschedule: {
      all: ["reschedule-slots"] as const,
      byClassAndDate: (classId: number, date: string) =>
        ["reschedule-slots", classId, date] as const,
    },
  },
} as const;

export function isCustomerQueryKey(queryKey: QueryKey): boolean {
  return queryKey[0] === "my" || queryKey[0] === "me";
}
