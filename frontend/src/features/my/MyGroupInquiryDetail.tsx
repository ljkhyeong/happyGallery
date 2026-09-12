import { useId, useState } from "react";
import { useBeforeUnload, useBlocker } from "react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Badge, Button, Card, Form, Modal } from "react-bootstrap";
import {
  cancelMyGroupInquiry, getMyGroupInquiry, updateMyGroupInquiry,
  type CancelMyGroupInquiryRequest, type MyGroupInquiryResponse, type UpdateMyGroupInquiryRequest,
} from "@/generated/api/customerStore";
import { GROUP_INQUIRY_STATUS } from "@/features/group-inquiry/status";
import {
  ApiError, captureCustomerSession, requireCurrentCustomerSession, runForCurrentCustomer,
  runForCustomerSession, type CustomerSessionSnapshot,
} from "@/shared/api";
import { formatDateTime } from "@/shared/lib";
import { ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";

interface InquiryDraft {
  version: number;
  headcount: string;
  preferredSchedule: string;
}
type InquiryChange = { customerSession: CustomerSessionSnapshot } & (
  | { action: "update"; request: UpdateMyGroupInquiryRequest }
  | { action: "cancel"; request: CancelMyGroupInquiryRequest }
);

export function MyGroupInquiryDetail({ id }: { id: number }) {
  const [draft, setDraft] = useState<InquiryDraft | null>(null);
  const [confirmCancel, setConfirmCancel] = useState(false);
  const leaveTitleId = useId();
  const cancelTitleId = useId();
  const client = useQueryClient();
  const toast = useToast();
  const detailKey = ["me", "group-inquiries", "detail", id] as const;
  const query = useQuery({
    queryKey: detailKey,
    queryFn: ({ signal }) => runForCurrentCustomer(() => getMyGroupInquiry(id, { signal })),
    refetchOnWindowFocus: false,
  });
  const mutation = useMutation({
    mutationFn: (change: InquiryChange) => runForCustomerSession(change.customerSession, async () => {
      const result = change.action === "cancel"
        ? await cancelMyGroupInquiry(id, change.request)
        : await updateMyGroupInquiry(id, change.request);
      requireCurrentCustomerSession(change.customerSession);
      await client.cancelQueries({ queryKey: detailKey, exact: true });
      requireCurrentCustomerSession(change.customerSession);
      client.setQueryData<MyGroupInquiryResponse>(detailKey,
        (current) => current && current.version > result.version ? current : result);
      setDraft(null);
      setConfirmCancel(false);
      toast.show(change.action === "cancel" ? "문의가 취소되었습니다." : "희망 일정과 참여 인원을 저장했습니다.", "success");
      await client.invalidateQueries({ queryKey: ["me", "group-inquiries", "list"] });
    }),
  });

  const detail = query.data;
  const values = draft ?? (detail ? {
    version: detail.version,
    headcount: String(detail.summary.headcount),
    preferredSchedule: detail.summary.preferredSchedule,
  } : null);
  const dirty = draft !== null && detail !== undefined
    && (draft.headcount !== String(detail.summary.headcount)
      || draft.preferredSchedule !== detail.summary.preferredSchedule);
  const shouldConfirmLeave = dirty || mutation.isPending;
  const blocker = useBlocker(shouldConfirmLeave);
  useBeforeUnload((event) => {
    if (!shouldConfirmLeave) return;
    event.preventDefault();
    event.returnValue = "";
  });

  const conflict = mutation.error instanceof ApiError && mutation.error.code === "CONFLICT";
  const changedElsewhere = !mutation.isPending && draft !== null && detail !== undefined && draft.version !== detail.version;
  const needsReload = changedElsewhere || conflict;
  const editable = detail?.summary.status === "RECEIVED" || detail?.summary.status === "CONSULTING";
  const busy = mutation.isPending || query.isFetching;
  const writeDisabled = busy || query.isError || needsReload || !editable;
  const loadLatest = () => {
    void runForCurrentCustomer(
      () => query.refetch({ throwOnError: true }),
      () => { setDraft(null); mutation.reset(); setConfirmCancel(false); },
    ).catch(() => { /* 조회 실패 시 입력을 유지하고 조회 오류를 표시한다. */ });
  };
  const edit = (change: Partial<Pick<InquiryDraft, "headcount" | "preferredSchedule">>) => {
    if (!values) return;
    setDraft({ ...values, ...change });
    if (!conflict) mutation.reset();
  };
  const mutationError = needsReload ? (
    <Alert variant="warning">
      문의가 변경되었습니다. 입력 내용은 유지했습니다.
      <div className="small mt-1">최신 내용을 불러오면 작성 중인 입력이 바뀝니다.</div>
      <Button size="sm" variant="outline-secondary" className="mt-2" disabled={busy} onClick={loadLatest}>
        최신 내용 불러오기
      </Button>
    </Alert>
  ) : (
    <ErrorAlert error={mutation.error} retrying={busy}
      onRetry={editable && !query.isError ? () => { if (mutation.variables) mutation.mutate(mutation.variables); } : undefined}
      retryLabel={mutation.variables?.action === "cancel" ? "문의 취소 다시 시도" : "변경 저장 다시 시도"} />
  );

  return (
    <Card className="mt-4"><Card.Body>
      <h2 className="h6">문의 상세 · 접수 번호 {id}</h2>
      <Button size="sm" variant="link" disabled={busy} onClick={() => { void query.refetch(); }}>문의 새로고침</Button>
      {query.isLoading && <LoadingSpinner />}
      <ErrorAlert error={query.error} onRetry={() => { void query.refetch(); }}
        retrying={busy} retryLabel="문의 조회 다시 시도" />
      {detail && values && (
        <>
          <p>{detail.summary.organization} · <Badge bg="secondary">{GROUP_INQUIRY_STATUS[detail.summary.status]}</Badge></p>
          {!confirmCancel && mutationError}
          <Form onSubmit={(event) => {
            event.preventDefault();
            if (writeDisabled) return;
            mutation.mutate({
              action: "update",
              request: { version: values.version, headcount: Number(values.headcount), preferredSchedule: values.preferredSchedule },
              customerSession: captureCustomerSession(),
            });
          }}>
            <fieldset disabled={!editable || busy}>
              <Form.Group controlId={`inquiry-${id}-headcount`} className="mb-3">
                <Form.Label>참여 인원</Form.Label>
                <Form.Control type="number" min={1} max={500} required value={values.headcount}
                  onChange={(event) => edit({ headcount: event.target.value })} />
              </Form.Group>
              <Form.Group controlId={`inquiry-${id}-schedule`} className="mb-3">
                <Form.Label>희망 일정</Form.Label>
                <Form.Control required maxLength={200} value={values.preferredSchedule}
                  onChange={(event) => edit({ preferredSchedule: event.target.value })} />
              </Form.Group>
              {editable && <div className="d-flex gap-2">
                <Button type="submit" disabled={writeDisabled}>{mutation.isPending && mutation.variables?.action === "update" ? "저장 중..." : "변경 저장"}</Button>
                <Button variant="outline-danger" disabled={writeDisabled} onClick={() => setConfirmCancel(true)}>문의 취소</Button>
              </div>}
            </fieldset>
          </Form>
          <p className="small text-muted mt-3">일정과 인원은 수업 확정 전까지 변경할 수 있습니다. 확정 후 변경은 공방에 문의해 주세요.</p>
          <h3 className="h6 mt-4">내 변경 이력</h3>
          {detail.changes.length === 0 ? <p className="small text-muted">변경 이력이 없습니다.</p> : (
            <ul className="ps-3 small">{detail.changes.map((change) => <li key={change.id} className="mb-2">
              <div>{change.note}</div><time className="text-muted">{formatDateTime(change.createdAt)}</time>
            </li>)}</ul>
          )}
          <Modal show={confirmCancel && blocker.state !== "blocked"} onHide={() => { if (!mutation.isPending) setConfirmCancel(false); }}
            centered aria-labelledby={cancelTitleId}>
            <Modal.Header closeButton={!mutation.isPending}><Modal.Title id={cancelTitleId}>단체 수업 문의 취소</Modal.Title></Modal.Header>
            <Modal.Body>
              문의 #{id}를 취소합니다. 상담이 필요하면 새로 문의해 주세요.
              {dirty && <p className="mt-2 mb-0">저장하지 않은 일정과 인원 변경은 반영되지 않습니다.</p>}
              {mutationError}
            </Modal.Body>
            <Modal.Footer>
              <Button variant="outline-secondary" disabled={mutation.isPending} onClick={() => setConfirmCancel(false)}>돌아가기</Button>
              <Button variant="danger" disabled={writeDisabled} onClick={() => mutation.mutate({
                action: "cancel", request: { version: values.version }, customerSession: captureCustomerSession(),
              })}>문의 취소 확인</Button>
            </Modal.Footer>
          </Modal>
        </>
      )}
      <Modal show={blocker.state === "blocked"} onHide={() => blocker.reset?.()} centered aria-labelledby={leaveTitleId}>
        <Modal.Header closeButton><Modal.Title id={leaveTitleId} className="fs-6">{dirty ? "문의 수정을 그만하시겠어요?" : "다른 화면으로 이동하시겠어요?"}</Modal.Title></Modal.Header>
        <Modal.Body>
          {mutation.isPending ? "요청을 처리하고 있습니다. 완료 후 이동해 주세요."
            : dirty ? "저장하지 않은 일정과 인원 변경이 사라집니다." : "변경 내용을 처리했습니다. 이동할 수 있습니다."}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" onClick={() => blocker.reset?.()}>계속 수정</Button>
          <Button variant="danger" disabled={mutation.isPending} onClick={() => blocker.proceed?.()}>{dirty ? "변경 버리고 이동" : "이동"}</Button>
        </Modal.Footer>
      </Modal>
    </Card.Body></Card>
  );
}
