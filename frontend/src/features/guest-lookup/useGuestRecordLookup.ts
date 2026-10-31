import { useState } from "react";
import { skipToken, useQuery } from "@tanstack/react-query";
import { useLocation, useSearchParams } from "react-router";
import { loadGuestRecordRecovery } from "@/features/guest-recovery/session";
import {
  isCurrentCustomerSessionState,
  queryKeys,
  runForCurrentCustomer,
  type CustomerSessionOwnedState,
} from "@/shared/api";
import { customerRefundRefetchInterval, parsePositiveId } from "@/shared/lib";
import type { RefundStatus } from "@/shared/types";

export type GuestRecordKind = "order" | "booking";

interface GuestRecordLocationState extends CustomerSessionOwnedState {
  orderId?: number;
  bookingId?: number;
  token?: string;
}

interface GuestRecordCredentials {
  id: number;
  token: string;
}

interface GuestRecordLookupRequest {
  credentials: GuestRecordCredentials;
  /** 같은 번호를 다시 조회해도 이전 응답 캐시를 쓰지 않도록 조회마다 새로 만든다. */
  requestId: string;
}

const ID_PARAM: Record<GuestRecordKind, "orderId" | "bookingId"> = {
  order: "orderId",
  booking: "bookingId",
};

/**
 * 비회원 주문·예약 상세 조회. 첫 값은 router state(현재 세션 snapshot 확인) → 주소의 번호 →
 * 비회원 기록 복구 조회 코드 순으로 정하고, 응답은 캐시에 남기지 않으며 환불 진행 중이면 다시 조회한다.
 */
export function useGuestRecordLookup<TRecord extends { refund: { status: RefundStatus } | null }>(
  kind: GuestRecordKind,
  fetchRecord: (id: number, token: string) => Promise<TRecord>,
) {
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const [initialCredentials] = useState(() => {
    const navState = isCurrentCustomerSessionState(location.state)
      ? location.state as GuestRecordLocationState
      : null;
    const idParam = ID_PARAM[kind];
    const id = navState?.[idParam]
      ?? parsePositiveId(searchParams.get(idParam))
      ?? undefined;
    const token = navState?.token
      ?? loadGuestRecordRecovery()?.value.accessToken
      ?? "";
    return { id, token: token.trim() };
  });
  const [lookup, setLookup] = useState<GuestRecordLookupRequest | null>(() =>
    initialCredentials.id && initialCredentials.token
      ? {
          credentials: { id: initialCredentials.id, token: initialCredentials.token },
          requestId: crypto.randomUUID(),
        }
      : null,
  );
  const query = useQuery({
    queryKey: queryKeys.guest.record(kind, lookup?.credentials.id, lookup?.requestId),
    queryFn: lookup
      ? () => runForCurrentCustomer(
          () => fetchRecord(lookup.credentials.id, lookup.credentials.token),
        )
      : skipToken,
    gcTime: 0,
    refetchInterval: customerRefundRefetchInterval((record: TRecord) => record.refund?.status),
  });
  const [relookupOpen, setRelookupOpen] = useState(false);

  function lookupRecord(id: number, token: string) {
    setRelookupOpen(false);
    if (lookup?.credentials.id === id && lookup.credentials.token === token) {
      void query.refetch();
      return;
    }
    setLookup({ credentials: { id, token }, requestId: crypto.randomUUID() });
  }

  return {
    kind,
    initialCredentials,
    lookup,
    query,
    relookupOpen,
    toggleRelookup: () => setRelookupOpen((open) => !open),
    lookupRecord,
  };
}
