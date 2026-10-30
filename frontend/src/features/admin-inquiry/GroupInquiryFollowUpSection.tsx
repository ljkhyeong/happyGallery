import { useState } from "react";
import { Button } from "react-bootstrap";
import { listAdminGroupInquiryFollowUps } from "@/generated/api/adminOperations";
import { adminHeaders, queryKeys } from "@/shared/api";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { useCursorHistory } from "@/shared/hooks/useCursorHistory";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { CursorPager } from "@/shared/ui/CursorPager";
import { GroupInquiryDetail } from "./AdminGroupInquirySection";

export function GroupInquiryFollowUpSection({ token, onAuthError }: { token: string; onAuthError: () => void }) {
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const paging = useCursorHistory();
  const query = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.groupInquiries.followUps(paging.cursor),
    queryFn: () => listAdminGroupInquiryFollowUps({ cursor: paging.cursor, size: 20 }, { headers: adminHeaders(token) }),
    refetchInterval: 60_000,
  });
  // 순서 안내는 목록이 있을 때만 둔다. 없으면 빈 상태 한 줄만 남아 오늘 할 일에서 한 줄로 줄어든다.
  return <div id="group-inquiry-follow-ups">
    {!!query.data?.content.length && (
      <p className="small text-muted">연락 예정일이 오늘까지인 문의를 오래된 순서로 표시합니다.</p>
    )}
    {query.isLoading && <LoadingSpinner />}
    <ErrorAlert error={query.error} onRetry={() => { void query.refetch(); }} />
    {query.data?.content.length === 0 && <EmptyState message="오늘까지 연락할 단체 문의가 없습니다." />}
    {query.data?.content.map((row) => <div key={row.id} className="d-flex justify-content-between gap-2 py-2 border-bottom">
      <div><strong>{row.organization}</strong><div className="small">연락 예정일 {row.nextContactOn}</div></div>
      <Button size="sm" variant="outline-primary" onClick={() => setSelectedId(row.id)}>상담 열기</Button>
    </div>)}
    <CursorPager className="d-flex gap-2 my-2" previousVariant="primary" nextVariant="primary"
      hasPreviousPage={paging.hasPreviousPage} page={query.data} busy={query.isFetching}
      onPrevious={paging.showPreviousPage} onNext={paging.showNextPage} />
    {selectedId !== null && <GroupInquiryDetail key={selectedId} id={selectedId} token={token} onAuthError={onAuthError} />}
  </div>;
}
