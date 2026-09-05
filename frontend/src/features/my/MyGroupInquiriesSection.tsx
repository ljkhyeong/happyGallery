import { useState } from "react";
import { MyGroupInquiryDetail } from "./MyGroupInquiryDetail";
import { useQuery } from "@tanstack/react-query";
import { Badge, Button, Card } from "react-bootstrap";
import { listMyGroupInquiries } from "@/generated/api/customerStore";
import { GROUP_INQUIRY_STATUS } from "@/features/group-inquiry/status";
import { runForCurrentCustomer } from "@/shared/api";
import { useCursorHistory } from "@/shared/hooks/useCursorHistory";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";

export function MyGroupInquiriesSection() {
  const paging = useCursorHistory();
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const query = useQuery({ queryKey: ["me", "group-inquiries", "list", paging.cursor],
    queryFn: ({ signal }) => runForCurrentCustomer(() => listMyGroupInquiries({ cursor: paging.cursor, size: 20 }, { signal })) });
  return (
    <section id="my-group-inquiries" className="mb-4">
      <h6>내 단체 수업 문의</h6>
      {query.isLoading && <LoadingSpinner />}
      <ErrorAlert error={query.error} onRetry={() => { void query.refetch(); }} />
      {query.data?.content.length === 0 && <EmptyState message="접수한 단체 수업 문의가 없습니다." />}
      {query.data?.content.map((inquiry) => <Card key={inquiry.id} className="mb-2 border-0 my-list-card"><Card.Body>
        <strong>{inquiry.organization}</strong> <Badge bg="secondary">{GROUP_INQUIRY_STATUS[inquiry.status]}</Badge>
        <div>{inquiry.classInterest} · {inquiry.headcount}명 · {inquiry.preferredSchedule}</div>
        <div className="small text-muted">{inquiry.location} · 접수 번호 {inquiry.id} · {formatDateTime(inquiry.createdAt)}</div>
        <Button size="sm" variant="outline-primary" className="mt-2" onClick={() => setSelectedId(inquiry.id)}>상세·변경 이력</Button>
    </Card.Body></Card>)}
      {(paging.hasPreviousPage || query.data?.hasMore) && <div className="d-flex gap-2">
        <Button size="sm" variant="outline-secondary" disabled={!paging.hasPreviousPage || query.isFetching} onClick={paging.showPreviousPage}>이전</Button>
        <Button size="sm" variant="outline-primary" disabled={!query.data?.hasMore || query.isFetching} onClick={() => paging.showNextPage(query.data?.nextCursor)}>다음</Button>
      </div>}
      {selectedId !== null && <MyGroupInquiryDetail key={selectedId} id={selectedId} />}
    </section>
  );
}
