import { useState } from "react";
import { Badge, Button, Card, Col, Form, Row } from "react-bootstrap";
import { useQueryClient } from "@tanstack/react-query";
import { createAdminGroupInquiry, getAdminGroupInquiry, listAdminGroupInquiries, updateAdminGroupInquiry, scheduleAdminGroupInquiryContact,
  type AdminGroupInquiryResponse, type GroupInquiryRequest, type ListAdminGroupInquiriesParams } from "@/generated/api/adminOperations";
import { GroupInquiryForm } from "@/features/group-inquiry/GroupInquiryForm";
import { GROUP_INQUIRY_STATUS, type GroupInquiryStatus } from "@/features/group-inquiry/status";
import { adminHeaders } from "@/shared/api";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useCursorHistory } from "@/shared/hooks/useCursorHistory";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";

interface Props { token: string; onAuthError: () => void }
const statuses = Object.keys(GROUP_INQUIRY_STATUS) as GroupInquiryStatus[];

export function AdminGroupInquirySection({ token, onAuthError }: Props) {
  const [status, setStatus] = useState<GroupInquiryStatus | "">("");
  const [searchForm, setSearchForm] = useState({ inquiryId: "", source: "", from: "", to: "" });
  const [filters, setFilters] = useState<ListAdminGroupInquiriesParams>({});
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [creating, setCreating] = useState(false);
  const paging = useCursorHistory();
  const client = useQueryClient();
  const query = useAdminQuery(onAuthError, { queryKey: ["admin", "group-inquiries", status, filters, paging.cursor],
    queryFn: () => listAdminGroupInquiries({ ...filters, status: status || undefined, cursor: paging.cursor, size: 20 }, { headers: adminHeaders(token) }) });
  const create = useAdminMutation(onAuthError, {
    mutationFn: (request: GroupInquiryRequest) => createAdminGroupInquiry(request, { headers: adminHeaders(token) }),
    onSuccess: (result) => {
      setCreating(false); setSelectedId(result.summary.id); paging.resetCursor();
      void client.invalidateQueries({ queryKey: ["admin", "group-inquiries"] });
    },
  });
  return (
    <div id="admin-group-inquiries">
      <div className="d-flex flex-wrap gap-2 justify-content-between mb-3">
        <Form.Group controlId="group-inquiry-status-filter"><Form.Label>문의 상태</Form.Label>
          <Form.Select value={status} onChange={(event) => { setStatus(event.target.value as GroupInquiryStatus | ""); paging.resetCursor(); setSelectedId(null); }}>
            <option value="">전체</option>{statuses.map((value) => <option key={value} value={value}>{GROUP_INQUIRY_STATUS[value]}</option>)}
          </Form.Select>
        </Form.Group>
        <Button variant="outline-primary" onClick={() => setCreating(!creating)}>{creating ? "등록 닫기" : "외부 문의 등록"}</Button>
      </div>
      <Form className="border rounded p-3 mb-3" onSubmit={(event) => {
        event.preventDefault();
        setFilters({ inquiryId: searchForm.inquiryId ? Number(searchForm.inquiryId) : undefined,
          source: (searchForm.source || undefined) as ListAdminGroupInquiriesParams["source"],
          from: searchForm.from || undefined, to: searchForm.to || undefined });
        paging.resetCursor(); setSelectedId(null);
      }}>
        <Row className="g-2 align-items-end">
          <Col sm={6} lg={3}><Form.Group controlId="group-search-id"><Form.Label>접수 번호</Form.Label>
            <Form.Control type="number" min={1} max={Number.MAX_SAFE_INTEGER} step={1} value={searchForm.inquiryId}
              onChange={(event) => setSearchForm({ ...searchForm, inquiryId: event.target.value })} /></Form.Group></Col>
          <Col sm={6} lg={3}><Form.Group controlId="group-search-source"><Form.Label>문의 경로</Form.Label>
            <Form.Select value={searchForm.source} onChange={(event) => setSearchForm({ ...searchForm, source: event.target.value })}>
              <option value="">전체</option><option value="WEBSITE">웹 접수</option><option value="EXTERNAL">외부 문의</option>
            </Form.Select></Form.Group></Col>
          <Col sm={6} lg={3}><Form.Group controlId="group-search-from"><Form.Label>접수 시작일</Form.Label>
            <Form.Control type="date" max={searchForm.to || undefined} value={searchForm.from}
              onChange={(event) => setSearchForm({ ...searchForm, from: event.target.value })} /></Form.Group></Col>
          <Col sm={6} lg={3}><Form.Group controlId="group-search-to"><Form.Label>접수 종료일</Form.Label>
            <Form.Control type="date" min={searchForm.from || undefined} value={searchForm.to}
              onChange={(event) => setSearchForm({ ...searchForm, to: event.target.value })} /></Form.Group></Col>
        </Row>
        <div className="d-flex gap-2 mt-3">
          <Button size="sm" type="submit">검색</Button>
          <Button size="sm" variant="outline-secondary" onClick={() => {
            setSearchForm({ inquiryId: "", source: "", from: "", to: "" }); setFilters({}); setStatus(""); paging.resetCursor(); setSelectedId(null);
          }}>검색 초기화</Button>
        </div>
      </Form>
      {creating && <Card className="mb-3"><Card.Body><h6>전화·네이버톡톡 등 외부 문의 등록</h6>
        <GroupInquiryForm onSubmit={(request) => create.mutate(request)} pending={create.isPending} error={create.error} submitLabel="외부 문의 저장" />
      </Card.Body></Card>}
      {query.isLoading && <LoadingSpinner />}
      <ErrorAlert error={query.error} onRetry={() => { void query.refetch(); }} />
      {query.data?.content.length === 0 && <EmptyState message="조건에 맞는 단체 문의가 없습니다." />}
      {query.data?.content.map((inquiry) => <Card key={inquiry.id} className="mb-2"><Card.Body className="d-flex gap-3 justify-content-between">
        <div><strong>{inquiry.organization}</strong> <Badge bg="secondary">{GROUP_INQUIRY_STATUS[inquiry.status]}</Badge>
          <div className="small">{inquiry.headcount}명 · {inquiry.classInterest} · {inquiry.preferredSchedule}</div>
          <div className="small text-muted">{inquiry.source === "EXTERNAL" ? "외부 문의" : "웹 접수"} · 접수 번호 {inquiry.id} · {formatDateTime(inquiry.createdAt)}</div>
        </div>
        <Button size="sm" variant="outline-primary" onClick={() => setSelectedId(inquiry.id)}>상담 열기</Button>
      </Card.Body></Card>)}
      {(paging.hasPreviousPage || query.data?.hasMore) && <div className="d-flex gap-2 my-3">
        <Button size="sm" disabled={!paging.hasPreviousPage || query.isFetching} onClick={paging.showPreviousPage}>이전</Button>
        <Button size="sm" disabled={!query.data?.hasMore || query.isFetching} onClick={() => paging.showNextPage(query.data?.nextCursor)}>다음</Button>
      </div>}
      {selectedId !== null && <GroupInquiryDetail key={selectedId} id={selectedId} token={token} onAuthError={onAuthError} />}
    </div>
  );
}

export function GroupInquiryDetail({ id, token, onAuthError }: Props & { id: number }) {
  const query = useAdminQuery(onAuthError, { queryKey: ["admin", "group-inquiry", id],
    queryFn: () => getAdminGroupInquiry(id, { headers: adminHeaders(token) }) });
  return <Card className="mt-3"><Card.Body>
    <h6>상담 내용 · 접수 번호 {id}</h6>
    {query.isLoading && <LoadingSpinner />}
    <ErrorAlert error={query.error} />
    <Button size="sm" variant="link" disabled={query.isFetching} onClick={() => { void query.refetch(); }}>최신 상담 불러오기</Button>
    {query.data && <ConsultationForm key={query.data.version} detail={query.data} token={token} onAuthError={onAuthError} />}
  </Card.Body></Card>;
}

function ConsultationForm({ detail, token, onAuthError }: Props & { detail: AdminGroupInquiryResponse }) {
  const [status, setStatus] = useState<GroupInquiryStatus>(detail.summary.status);
  const [note, setNote] = useState("");
  const [nextContactOn, setNextContactOn] = useState(detail.nextContactOn ?? "");
  const client = useQueryClient();
  const toast = useToast();
  const id = detail.summary.id;
  const canceled = detail.summary.status === "CANCELED";
  const allowed = statuses.filter((value) => value === detail.summary.status || value === "CONSULTING" || value === "CLOSED"
    || (detail.summary.status === "CONSULTING" && value === "CONFIRMED"));
  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => updateAdminGroupInquiry(id, { version: detail.version, status, note }, { headers: adminHeaders(token) }),
    onSuccess: (result) => {
      client.setQueryData(["admin", "group-inquiry", id], result);
      void client.invalidateQueries({ queryKey: ["admin", "group-inquiries"] });
      toast.show("상담 상태와 메모를 저장했습니다.");
    },
  });
  const contactMutation = useAdminMutation(onAuthError, {
    mutationFn: () => scheduleAdminGroupInquiryContact(id, { version: detail.version, nextContactOn: nextContactOn || null }, { headers: adminHeaders(token) }),
    onSuccess: (result) => {
      client.setQueryData(["admin", "group-inquiry", id], result);
      void client.invalidateQueries({ queryKey: ["admin", "group-inquiries"] });
      toast.show("다음 연락일을 저장했습니다.");
    },
  });
  return <>
    <p className="mb-1">{detail.details.organization} · {detail.details.contactName} · {detail.details.phone} {detail.details.email && `· ${detail.details.email}`}</p>
    <p className="mb-1">{detail.details.headcount}명 · {detail.details.preferredSchedule} · {detail.details.location} · {detail.details.classInterest}</p>
    <p className="small" style={{ whiteSpace: "pre-wrap" }}>{detail.details.message}</p>
    {detail.summary.status !== "CLOSED" && !canceled && <Form className="my-3" onSubmit={(event) => { event.preventDefault(); contactMutation.mutate(); }}>
      <Form.Group controlId={`next-contact-${id}`} className="mb-2"><Form.Label>다음 연락일</Form.Label>
        <Form.Control type="date" value={nextContactOn} disabled={contactMutation.isPending || mutation.isPending}
          onChange={(event) => setNextContactOn(event.target.value)} />
        <Form.Text>서울 날짜 기준입니다. 비우고 저장하면 연락 예정일을 해제합니다.</Form.Text>
      </Form.Group>
      <ErrorAlert error={contactMutation.error} />
      <Button type="submit" variant="outline-primary" disabled={contactMutation.isPending || mutation.isPending}>연락일 저장</Button>
    </Form>}
    <Form onSubmit={(event) => { event.preventDefault(); mutation.mutate(); }}>
      <Form.Group controlId={`consultation-status-${id}`} className="mb-2"><Form.Label>상담 상태</Form.Label>
        <Form.Select value={status} disabled={mutation.isPending || canceled} onChange={(event) => setStatus(event.target.value as GroupInquiryStatus)}>
          {allowed.map((value) => <option key={value} value={value}>{GROUP_INQUIRY_STATUS[value]}</option>)}
        </Form.Select>
      </Form.Group>
      <Form.Group controlId={`consultation-note-${id}`} className="mb-2"><Form.Label>상담 메모</Form.Label>
        <Form.Control as="textarea" rows={3} required maxLength={2000} disabled={mutation.isPending || canceled} value={note} onChange={(event) => setNote(event.target.value)} />
      </Form.Group>
      <ErrorAlert error={mutation.error} />
      <Button type="submit" disabled={mutation.isPending || contactMutation.isPending || canceled || !note.trim()}>{mutation.isPending ? "저장 중..." : "상담 저장"}</Button>
    </Form>
    <p className="small text-muted mt-2">확정은 상담 결과를 기록합니다. 결제나 일반 클래스 예약은 자동 생성되지 않습니다.</p>
    {detail.activities.map((activity) => <div key={activity.id} className="border-top py-2 small">
      <strong>{GROUP_INQUIRY_STATUS[activity.toStatus]}</strong> · {formatDateTime(activity.createdAt)} · {activity.memberAction ? "회원" : activity.adminId === null ? "로컬 관리자" : `관리자 ${activity.adminId}`}
      <div style={{ whiteSpace: "pre-wrap" }}>{activity.note}</div>
    </div>)}
  </>;
}
