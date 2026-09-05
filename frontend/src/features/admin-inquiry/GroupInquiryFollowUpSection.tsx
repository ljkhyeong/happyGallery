import { useState } from "react";
import { Button } from "react-bootstrap";
import { listAdminGroupInquiryFollowUps } from "@/generated/api/adminOperations";
import { adminHeaders } from "@/shared/api";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { useCursorHistory } from "@/shared/hooks/useCursorHistory";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { GroupInquiryDetail } from "./AdminGroupInquirySection";

export function GroupInquiryFollowUpSection({ token, onAuthError }: { token: string; onAuthError: () => void }) {
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const paging = useCursorHistory();
  const query = useAdminQuery(onAuthError, {
    queryKey: ["admin", "group-inquiries", "follow-ups", paging.cursor],
    queryFn: () => listAdminGroupInquiryFollowUps({ cursor: paging.cursor, size: 20 }, { headers: adminHeaders(token) }),
    refetchInterval: 60_000,
  });
  return <div id="group-inquiry-follow-ups">
    <p className="small text-muted">오늘과 예정일이 지난 문의를 오래된 연락일 순으로 표시합니다.</p>
    {query.isLoading && <LoadingSpinner />}
    <ErrorAlert error={query.error} onRetry={() => { void query.refetch(); }} />
    {query.data?.content.length === 0 && <EmptyState message="오늘까지 연락할 단체 문의가 없습니다." />}
    {query.data?.content.map((row) => <div key={row.id} className="d-flex justify-content-between gap-2 py-2 border-bottom">
      <div><strong>{row.organization}</strong><div className="small">연락 예정일 {row.nextContactOn}</div></div>
      <Button size="sm" variant="outline-primary" onClick={() => setSelectedId(row.id)}>상담 열기</Button>
    </div>)}
    {(paging.hasPreviousPage || query.data?.hasMore) && <div className="d-flex gap-2 my-2">
      <Button size="sm" disabled={!paging.hasPreviousPage || query.isFetching} onClick={paging.showPreviousPage}>이전</Button>
      <Button size="sm" disabled={!query.data?.hasMore || query.isFetching} onClick={() => paging.showNextPage(query.data?.nextCursor)}>다음</Button>
    </div>}
    {selectedId !== null && <GroupInquiryDetail key={selectedId} id={selectedId} token={token} onAuthError={onAuthError} />}
  </div>;
}
