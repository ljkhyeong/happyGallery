import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Badge, Button, Card, Form, Modal } from "react-bootstrap";
import { cancelMyGroupInquiry, getMyGroupInquiry, updateMyGroupInquiry, type MyGroupInquiryResponse } from "@/generated/api/customerStore";
import { GROUP_INQUIRY_STATUS } from "@/features/group-inquiry/status";
import { ApiError, runForCurrentCustomer } from "@/shared/api";
import { formatDateTime } from "@/shared/lib";
import { ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";

export function MyGroupInquiryDetail({ id }: { id: number }) {
  const query = useQuery({
    queryKey: ["me", "group-inquiries", "detail", id],
    queryFn: ({ signal }) => getMyGroupInquiry(id, { signal }),
    refetchOnWindowFocus: false,
  });
  return (
    <Card className="mt-4"><Card.Body>
      <h2 className="h6">문의 상세 · 접수 번호 {id}</h2>
      <Button size="sm" variant="link" disabled={query.isFetching} onClick={() => { void query.refetch(); }}>최신 문의 불러오기</Button>
      {query.isLoading && <LoadingSpinner />}
      <ErrorAlert error={query.error} />
      {query.data && <MemberInquiryForm key={query.data.version} detail={query.data} />}
    </Card.Body></Card>
  );
}

function MemberInquiryForm({ detail }: { detail: MyGroupInquiryResponse }) {
  const { summary, version } = detail;
  const [headcount, setHeadcount] = useState(String(summary.headcount));
  const [schedule, setSchedule] = useState(summary.preferredSchedule);
  const [confirmCancel, setConfirmCancel] = useState(false);
  const client = useQueryClient();
  const toast = useToast();
  const editable = summary.status === "RECEIVED" || summary.status === "CONSULTING";
  const mutation = useMutation({
    mutationFn: (action: "update" | "cancel") => runForCurrentCustomer(
      () => action === "cancel" ? cancelMyGroupInquiry(summary.id, { version })
        : updateMyGroupInquiry(summary.id, { version, headcount: Number(headcount), preferredSchedule: schedule }),
      async (result, requireCurrent) => {
        await client.invalidateQueries({ queryKey: ["me", "group-inquiries", "list"] });
        requireCurrent();
        toast.show(action === "cancel" ? "문의가 취소되었습니다." : "희망 일정과 참여 인원을 저장했습니다.", "success");
        client.setQueryData(["me", "group-inquiries", "detail", summary.id], result);
      },
    ),
  });
  const mutationError = mutation.error instanceof ApiError && mutation.error.code === "CONFLICT"
    ? <Alert variant="warning">문의가 변경되었거나 수정할 수 없는 상태입니다. 최신 문의를 불러와 확인해 주세요.</Alert>
    : <ErrorAlert error={mutation.error} />;
  const submit = (event: FormEvent) => { event.preventDefault(); mutation.mutate("update"); };
  return (
    <>
      <p>{summary.organization} · <Badge bg="secondary">{GROUP_INQUIRY_STATUS[summary.status]}</Badge></p>
      <Form onSubmit={submit}>
        <fieldset disabled={!editable || mutation.isPending}>
          <Form.Group controlId={`inquiry-${summary.id}-headcount`} className="mb-3">
            <Form.Label>참여 인원</Form.Label>
            <Form.Control type="number" min={1} max={500} required value={headcount} onChange={(event) => setHeadcount(event.target.value)} />
          </Form.Group>
          <Form.Group controlId={`inquiry-${summary.id}-schedule`} className="mb-3">
            <Form.Label>희망 일정</Form.Label>
            <Form.Control required maxLength={200} value={schedule} onChange={(event) => setSchedule(event.target.value)} />
          </Form.Group>
          {editable && <div className="d-flex gap-2">
            <Button type="submit">변경 저장</Button>
            <Button variant="outline-danger" onClick={() => setConfirmCancel(true)}>문의 취소</Button>
          </div>}
        </fieldset>
      </Form>
      {mutationError}
      <p className="small text-muted mt-3">일정과 인원은 수업 확정 전까지 변경할 수 있습니다. 확정 후 변경은 공방에 문의해 주세요.</p>
      <h3 className="h6 mt-4">내 변경 이력</h3>
      {detail.changes.length === 0 ? <p className="small text-muted">변경 이력이 없습니다.</p> : (
        <ul className="ps-3 small">{detail.changes.map((change) => <li key={change.id} className="mb-2">
          <div>{change.note}</div><time className="text-muted">{formatDateTime(change.createdAt)}</time>
        </li>)}</ul>
      )}
      <Modal show={confirmCancel} onHide={() => { if (!mutation.isPending) setConfirmCancel(false); }} centered>
        <Modal.Header closeButton={!mutation.isPending}><Modal.Title>단체 수업 문의 취소</Modal.Title></Modal.Header>
        <Modal.Body>접수 번호 {summary.id}의 상담을 취소합니다. 취소 후에는 새 문의를 접수해 주세요.{mutationError}</Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" disabled={mutation.isPending} onClick={() => setConfirmCancel(false)}>돌아가기</Button>
          <Button variant="danger" disabled={mutation.isPending} onClick={() => mutation.mutate("cancel")}>문의 취소 확인</Button>
        </Modal.Footer>
      </Modal>
    </>
  );
}
