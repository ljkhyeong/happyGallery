import { useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Alert, Badge, Button, Card, Col, Form, Row } from "react-bootstrap";
import {
  createEvent,
  deleteEvent,
  fetchAdminEvent,
  fetchAdminEvents,
  updateEvent,
  type CreateEventRequest,
  type EventResponse,
  type UpdateEventRequest,
} from "./api";
import { fetchProducts } from "@/features/admin-product/api";
import { AdminImageField } from "@/features/admin-media/AdminImageField";
import { eventTimingLabel } from "@/features/event/time";
import { ApiError, queryKeys } from "@/shared/api";
import { isAdminSessionUnauthorized } from "@/shared/hooks/adminSessionUnauthorized";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDateTime } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import {
  CONTENT_BODY_MAX_LENGTH,
  CONTENT_TITLE_MAX_LENGTH,
  contentLengthLabel,
} from "@/shared/validation/contentText";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

interface EventFormState {
  title: string;
  summary: string;
  content: string;
  imageUrl: string;
  startAt: string;
  endAt: string;
  published: boolean;
  featured: boolean;
  relatedProductIds: number[];
}

const SUMMARY_MAX_LENGTH = 500;

function emptyForm(): EventFormState {
  return {
    title: "",
    summary: "",
    content: "",
    imageUrl: "",
    startAt: "",
    endAt: "",
    published: false,
    featured: false,
    relatedProductIds: [],
  };
}

function formFrom(event: EventResponse): EventFormState {
  return {
    title: event.title,
    summary: event.summary,
    content: event.content,
    imageUrl: event.imageUrl ?? "",
    startAt: event.startAt.slice(0, 16),
    endAt: event.endAt.slice(0, 16),
    published: event.published,
    featured: event.featured,
    relatedProductIds: [...event.relatedProductIds],
  };
}

function createRequest(form: EventFormState): CreateEventRequest {
  return {
    title: form.title,
    summary: form.summary,
    content: form.content,
    imageUrl: form.imageUrl.trim() || undefined,
    startAt: form.startAt,
    endAt: form.endAt,
    published: form.published,
    featured: form.featured,
    relatedProductIds: form.relatedProductIds.length > 0
      ? [...form.relatedProductIds].sort((left, right) => left - right)
      : undefined,
  };
}

function updateRequest(form: EventFormState, expectedVersion: number): UpdateEventRequest {
  return { ...createRequest(form), expectedVersion };
}

export function AdminEventSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const editRequestId = useRef(0);
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [editVersion, setEditVersion] = useState<number | null>(null);
  const [form, setForm] = useState<EventFormState>(emptyForm);
  const [editLoading, setEditLoading] = useState(false);
  const [actionError, setActionError] = useState<Error | null>(null);
  const [conflict, setConflict] = useState<EventResponse | null>(null);

  const eventsQuery = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.events,
    queryFn: () => fetchAdminEvents(adminKey),
  });
  const productsQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "products"],
    queryFn: () => fetchProducts(adminKey),
  });

  const invalidateEvents = () => {
    void Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.events }),
      queryClient.invalidateQueries({ queryKey: queryKeys.events.all }),
    ]);
  };

  const resetForm = () => {
    editRequestId.current += 1;
    setShowForm(false);
    setEditId(null);
    setEditVersion(null);
    setForm(emptyForm());
    setEditLoading(false);
    setActionError(null);
    setConflict(null);
  };

  const createMutation = useAdminMutation(onAuthError, {
    mutationFn: () => createEvent(createRequest(form), adminKey),
    onMutate: () => setActionError(null),
    onSuccess: () => {
      toast.show("이벤트가 등록되었습니다.");
      resetForm();
      invalidateEvents();
    },
    onError: setActionError,
  });

  const updateMutation = useAdminMutation(onAuthError, {
    mutationFn: () => updateEvent(
      editId!,
      updateRequest(form, editVersion!),
      adminKey,
    ),
    onMutate: () => setActionError(null),
    onSuccess: () => {
      toast.show("이벤트가 수정되었습니다.");
      resetForm();
      invalidateEvents();
    },
    onError: async (error) => {
      if (!(error instanceof ApiError) || error.status !== 409 || editId === null) {
        setActionError(error);
        return;
      }

      const requestId = editRequestId.current;
      setEditLoading(true);
      try {
        const latest = await fetchAdminEvent(editId, adminKey);
        if (requestId !== editRequestId.current) return;
        setConflict(latest);
        setActionError(null);
        void queryClient.invalidateQueries({ queryKey: queryKeys.admin.events });
      } catch (refreshError) {
        if (requestId !== editRequestId.current) return;
        if (isAdminSessionUnauthorized(refreshError)) onAuthError();
        setActionError(
          refreshError instanceof Error
            ? refreshError
            : new Error("최신 이벤트를 불러오지 못했습니다."),
        );
      } finally {
        if (requestId === editRequestId.current) setEditLoading(false);
      }
    },
  });

  const deleteMutation = useAdminMutation(onAuthError, {
    mutationFn: ({ id, version }: Pick<EventResponse, "id" | "version">) =>
      deleteEvent(id, version, adminKey),
    onMutate: () => setActionError(null),
    onSuccess: () => {
      toast.show("이벤트가 삭제되었습니다.");
      invalidateEvents();
    },
    onError: (error) => {
      setActionError(error);
      if (error instanceof ApiError && error.status === 409) {
        void queryClient.invalidateQueries({ queryKey: queryKeys.admin.events });
      }
    },
  });

  const startEdit = async (event: EventResponse) => {
    const requestId = ++editRequestId.current;
    setEditId(event.id);
    setEditVersion(null);
    setForm(formFrom(event));
    setShowForm(true);
    setEditLoading(true);
    setActionError(null);
    setConflict(null);
    try {
      const latest = await fetchAdminEvent(event.id, adminKey);
      if (requestId !== editRequestId.current) return;
      setForm(formFrom(latest));
      setEditVersion(latest.version);
    } catch (error) {
      if (requestId !== editRequestId.current) return;
      if (isAdminSessionUnauthorized(error)) onAuthError();
      setActionError(error instanceof Error ? error : new Error("이벤트를 불러오지 못했습니다."));
    } finally {
      if (requestId === editRequestId.current) setEditLoading(false);
    }
  };

  const valid = form.title.trim().length > 0
    && form.summary.trim().length > 0
    && form.content.trim().length > 0
    && form.startAt.length > 0
    && form.endAt.length > 0
    && form.startAt < form.endAt;

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    if (!valid) return;
    if (editId === null) createMutation.mutate();
    else if (editVersion !== null) updateMutation.mutate();
  };

  return (
    <div>
      <div className="d-flex justify-content-end mb-3">
        <Button
          size="sm"
          variant="outline-primary"
          onClick={() => {
            if (showForm) resetForm();
            else {
              setForm(emptyForm());
              setShowForm(true);
            }
          }}
        >
          {showForm ? "취소" : "새 이벤트 작성"}
        </Button>
      </div>

      <ErrorAlert error={actionError} />

      {showForm && (
        <Card className="mb-4">
          <Card.Body>
            {editLoading && <LoadingSpinner text="이벤트 불러오는 중..." />}
            {conflict && (
              <Alert variant="warning">
                <p className="mb-2">
                  다른 관리자가 먼저 수정했습니다. 작성 중인 초안은 그대로 보존했습니다.
                </p>
                <div className="d-flex flex-wrap gap-2">
                  <Button
                    type="button"
                    size="sm"
                    variant="outline-dark"
                    onClick={() => {
                      setEditVersion(conflict.version);
                      setConflict(null);
                      toast.show("내 입력 내용은 그대로 유지했습니다. 다른 관리자의 변경 내용을 확인한 뒤 다시 저장해 주세요.");
                    }}
                  >
                    내 초안 유지
                  </Button>
                  <Button
                    type="button"
                    size="sm"
                    variant="outline-secondary"
                    onClick={() => {
                      setForm(formFrom(conflict));
                      setEditVersion(conflict.version);
                      setConflict(null);
                    }}
                  >
                    다른 관리자가 저장한 내용 불러오기
                  </Button>
                </div>
              </Alert>
            )}

            <Form onSubmit={submit}>
              <Row className="g-3">
                <Col xs={12}>
                  <Form.Group controlId="admin-event-title">
                    <Form.Label>제목</Form.Label>
                    <Form.Control
                      value={form.title}
                      maxLength={CONTENT_TITLE_MAX_LENGTH}
                      onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
                      disabled={editLoading}
                      required
                    />
                    <Form.Text className="d-block text-end">
                      {contentLengthLabel(form.title, CONTENT_TITLE_MAX_LENGTH)}
                    </Form.Text>
                  </Form.Group>
                </Col>
                <Col xs={12}>
                  <Form.Group controlId="admin-event-summary">
                    <Form.Label>요약</Form.Label>
                    <Form.Control
                      as="textarea"
                      rows={2}
                      value={form.summary}
                      maxLength={SUMMARY_MAX_LENGTH}
                      onChange={(event) => setForm((current) => ({ ...current, summary: event.target.value }))}
                      disabled={editLoading}
                      required
                    />
                    <Form.Text className="d-block text-end">
                      {contentLengthLabel(form.summary, SUMMARY_MAX_LENGTH)}
                    </Form.Text>
                  </Form.Group>
                </Col>
                <Col xs={12}>
                  <Form.Group controlId="admin-event-content">
                    <Form.Label>내용</Form.Label>
                    <Form.Control
                      as="textarea"
                      rows={7}
                      value={form.content}
                      maxLength={CONTENT_BODY_MAX_LENGTH}
                      onChange={(event) => setForm((current) => ({ ...current, content: event.target.value }))}
                      disabled={editLoading}
                      required
                    />
                    <Form.Text className="d-block text-end">
                      {contentLengthLabel(form.content, CONTENT_BODY_MAX_LENGTH)}
                    </Form.Text>
                  </Form.Group>
                </Col>
                <Col xs={12}>
                  <AdminImageField
                    adminKey={adminKey}
                    value={form.imageUrl}
                    onChange={(imageUrl) => setForm((current) => ({ ...current, imageUrl }))}
                    onAuthError={onAuthError}
                    controlId="admin-event-image"
                    previewAlt="이벤트 대표 이미지 미리보기"
                  />
                </Col>
                <Col xs={12} md={6}>
                  <Form.Group controlId="admin-event-start-at">
                    <Form.Label>시작 시각</Form.Label>
                    <Form.Control
                      type="datetime-local"
                      value={form.startAt}
                      onChange={(event) => setForm((current) => ({ ...current, startAt: event.target.value }))}
                      disabled={editLoading}
                      required
                    />
                  </Form.Group>
                </Col>
                <Col xs={12} md={6}>
                  <Form.Group controlId="admin-event-end-at">
                    <Form.Label>종료 시각</Form.Label>
                    <Form.Control
                      type="datetime-local"
                      value={form.endAt}
                      min={form.startAt || undefined}
                      onChange={(event) => setForm((current) => ({ ...current, endAt: event.target.value }))}
                      disabled={editLoading}
                      required
                    />
                  </Form.Group>
                </Col>
                <Col xs={12}>
                  <Form.Label>연관 상품</Form.Label>
                  {productsQuery.isLoading && <LoadingSpinner text="상품 불러오는 중..." />}
                  <ErrorAlert error={productsQuery.error} />
                  {productsQuery.data && productsQuery.data.length === 0 && (
                    <p className="small text-muted-soft">연결할 수 있는 상품이 없습니다.</p>
                  )}
                  {productsQuery.data && productsQuery.data.length > 0 && (
                    <div className="d-flex flex-wrap gap-3 rounded border p-3">
                      {productsQuery.data.map((product) => (
                        <Form.Check
                          key={product.id}
                          id={`admin-event-product-${product.id}`}
                          type="checkbox"
                          label={`${product.name} (#${product.id})`}
                          checked={form.relatedProductIds.includes(product.id)}
                          disabled={editLoading}
                          onChange={(changeEvent) => setForm((current) => ({
                            ...current,
                            relatedProductIds: changeEvent.target.checked
                              ? [...current.relatedProductIds, product.id]
                              : current.relatedProductIds.filter((id) => id !== product.id),
                          }))}
                        />
                      ))}
                    </div>
                  )}
                </Col>
                <Col xs={12} className="d-flex flex-wrap align-items-center justify-content-between gap-3">
                  <div className="d-flex flex-wrap gap-3">
                    <Form.Check
                      id="admin-event-published"
                      type="checkbox"
                      label="공개"
                      checked={form.published}
                      onChange={(event) => setForm((current) => ({ ...current, published: event.target.checked }))}
                      disabled={editLoading}
                    />
                    <Form.Check
                      id="admin-event-featured"
                      type="checkbox"
                      label="홈 추천"
                      checked={form.featured}
                      onChange={(event) => setForm((current) => ({ ...current, featured: event.target.checked }))}
                      disabled={editLoading}
                    />
                  </div>
                  <Button
                    type="submit"
                    disabled={
                      !valid
                      || editLoading
                      || conflict !== null
                      || (editId !== null && editVersion === null)
                      || createMutation.isPending
                      || updateMutation.isPending
                    }
                  >
                    {editId === null ? "등록" : "수정"}
                  </Button>
                </Col>
              </Row>
            </Form>
          </Card.Body>
        </Card>
      )}

      {eventsQuery.isLoading && <LoadingSpinner />}
      <ErrorAlert error={eventsQuery.error} />
      {eventsQuery.data?.length === 0 && <EmptyState message="등록된 이벤트가 없습니다." />}

      {eventsQuery.data?.map((event) => (
        <Card key={event.id} className="mb-2">
          <Card.Body className="d-flex flex-wrap align-items-center justify-content-between gap-3 py-3">
            <div>
              <div className="d-flex flex-wrap align-items-center gap-2 mb-1">
                <strong>{event.title}</strong>
                <Badge bg={event.published ? "success" : "secondary"}>
                  {event.published ? "공개" : "비공개"}
                </Badge>
                {event.featured && <Badge bg="dark">홈 추천</Badge>}
                {event.published && <Badge bg="light" text="dark">{eventTimingLabel(event)}</Badge>}
              </div>
              <div className="small text-muted-soft">
                {formatDateTime(event.startAt)} ~ {formatDateTime(event.endAt)}
                {event.relatedProductIds.length > 0
                  ? ` · 연관 상품 ${event.relatedProductIds.length}개`
                  : ""}
              </div>
            </div>
            <div className="d-flex gap-2">
              <Button
                size="sm"
                variant="outline-secondary"
                onClick={() => void startEdit(event)}
                disabled={editLoading}
              >
                수정
              </Button>
              <Button
                size="sm"
                variant="outline-danger"
                onClick={() => {
                  if (confirm("이벤트를 삭제하시겠습니까?")) {
                    deleteMutation.mutate({ id: event.id, version: event.version });
                  }
                }}
                disabled={deleteMutation.isPending}
              >
                삭제
              </Button>
            </div>
          </Card.Body>
        </Card>
      ))}
    </div>
  );
}
