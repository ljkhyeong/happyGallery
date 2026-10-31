import { useSearchParams } from "react-router";
import { GroupInquiryStatusBadge } from "./GroupInquiryStatusBadge";
import { GROUP_INQUIRY_DETAIL_ID, MyGroupInquiryDetail } from "./MyGroupInquiryDetail";
import { useQuery } from "@tanstack/react-query";
import { Button, Card } from "react-bootstrap";
import { listMyGroupInquiries } from "@/generated/api/customerStore";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { useCursorHistory } from "@/shared/hooks/useCursorHistory";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { CursorPager } from "@/shared/ui/CursorPager";
import { formatDateTime, parsePositiveId } from "@/shared/lib";

export function MyGroupInquiriesSection() {
  const paging = useCursorHistory();
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedId = parsePositiveId(searchParams.get("inquiryId"));
  const query = useQuery({ queryKey: queryKeys.member.groupInquiries.list(paging.cursor),
    queryFn: ({ signal }) => runForCurrentCustomer(() => listMyGroupInquiries({ cursor: paging.cursor, size: 20 }, { signal })) });
  return (
    <section id="my-group-inquiries" className="mb-4">
      {query.isLoading && <LoadingSpinner />}
      <ErrorAlert error={query.error} onRetry={() => { void query.refetch(); }} />
      {query.data?.content.length === 0 && <EmptyState message="접수한 단체 수업 문의가 없습니다." />}
      {/* 상세는 쪽을 넘겨도 작성 중인 입력이 남도록 목록 아래 한 자리에 열고, 고른 문의는 카드에 먹색 선으로 표시한다. */}
      {query.data?.content.map((inquiry) => {
        const selected = selectedId === inquiry.id;
        return (
          <Card key={inquiry.id} className={`mb-2 my-list-card${selected ? " is-selected" : ""}`}
            aria-current={selected ? "true" : undefined}><Card.Body>
            <strong>{inquiry.organization}</strong> <GroupInquiryStatusBadge status={inquiry.status} />
            <div>{inquiry.classInterest} · {inquiry.headcount}명 · {inquiry.preferredSchedule}</div>
            <div className="small text-muted">{inquiry.location} · 접수 번호 {inquiry.id} · {formatDateTime(inquiry.createdAt)}</div>
            <Button size="sm" variant="outline-primary" className="mt-2"
              aria-controls={selected ? GROUP_INQUIRY_DETAIL_ID : undefined}
              onClick={() => {
                if (selected) {
                  document.getElementById(GROUP_INQUIRY_DETAIL_ID)?.scrollIntoView({ block: "start" });
                  return;
                }
                const next = new URLSearchParams(searchParams);
                next.set("inquiryId", String(inquiry.id));
                setSearchParams(next);
              }}>{selected ? "열린 상세로 이동" : "상세·변경 이력"}</Button>
          </Card.Body></Card>
        );
      })}
      <CursorPager className="d-flex gap-2"
        hasPreviousPage={paging.hasPreviousPage} page={query.data} busy={query.isFetching}
        onPrevious={paging.showPreviousPage} onNext={paging.showNextPage} />
      {selectedId !== null && <MyGroupInquiryDetail key={selectedId} id={selectedId} />}
    </section>
  );
}
