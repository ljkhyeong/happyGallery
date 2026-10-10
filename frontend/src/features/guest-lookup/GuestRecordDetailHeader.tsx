import { Button } from "react-bootstrap";
import { GuestLookupForm } from "./GuestLookupForm";
import { GuestLookupPanel } from "./GuestLookupPanel";
import type { GuestRecordKind } from "./useGuestRecordLookup";

interface Props {
  lookup: {
    kind: GuestRecordKind;
    initialCredentials: { id?: number; token: string };
    relookupOpen: boolean;
    toggleRelookup: () => void;
    lookupRecord: (id: number, token: string) => void;
    query: { isFetching: boolean; refetch: () => unknown };
  };
  /** 기록을 찾으면 머리와 다시 조회 버튼을 보이고 조회 입력은 접는다. */
  found: boolean;
  description: string;
  submitLabel?: string;
}

/** 비회원 주문·예약 상세의 머리(최신 상태 확인·다른 번호로 찾기)와 접어 두는 조회 입력. */
export function GuestRecordDetailHeader({ lookup, found, description, submitLabel }: Props) {
  const { kind, initialCredentials, relookupOpen, toggleRelookup, lookupRecord, query } = lookup;
  const label = kind === "order" ? "주문" : "예약";
  const lookupKind = kind === "order" ? "orders" : "bookings";

  return (
    <>
      {found && (
        <header className="guest-detail-header">
          <div>
            <p className="store-section-kicker mb-1">{`비회원 ${label}`}</p>
            <h1>{`비회원 ${label} 조회`}</h1>
            <p>{description}</p>
          </div>
          <div className="guest-detail-actions">
            <Button variant="outline-dark" size="sm" disabled={query.isFetching} onClick={() => void query.refetch()}>
              {query.isFetching ? "확인 중..." : "최신 상태 확인"}
            </Button>
            <Button
              variant="outline-dark"
              size="sm"
              aria-expanded={relookupOpen}
              aria-controls="guest-relookup"
              onClick={toggleRelookup}
            >
              {relookupOpen ? "다른 번호 입력 닫기" : "다른 번호로 찾기"}
            </Button>
          </div>
        </header>
      )}
      {/* 조회 후에도 입력값은 유지해야 하므로 지우지 않고 숨긴다. */}
      <div id="guest-relookup" hidden={found && !relookupOpen}>
        <GuestLookupPanel title={`비회원 ${label} 조회`} kind={lookupKind}>
          <GuestLookupForm
            kind={lookupKind}
            onLookup={lookupRecord}
            isLoading={query.isFetching}
            initialId={initialCredentials.id ? String(initialCredentials.id) : undefined}
            initialToken={initialCredentials.token}
            submitLabel={submitLabel}
          />
        </GuestLookupPanel>
      </div>
    </>
  );
}
