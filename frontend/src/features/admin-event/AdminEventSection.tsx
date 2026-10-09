import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Badge, Button, Card, Col, Form, Row } from "react-bootstrap";
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
import {
  fetchAdminCoupons,
  type AdminCouponResponse,
} from "@/features/admin-coupon/api";
import { fetchProducts } from "@/features/admin-product/api";
import { AdminImageField } from "@/features/admin-media/AdminImageField";
import { eventTimingLabel } from "@/features/event/time";
import { queryKeys } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { isVersionConflict, useVersionedEdit } from "@/shared/hooks/useVersionedEdit";
import { formatDateTime } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import { VersionConflictAlert } from "@/shared/ui/VersionConflictAlert";
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
  couponDefinitionId: number | null;
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
    couponDefinitionId: null,
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
    couponDefinitionId: event.couponDefinitionId,
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
    couponDefinitionId: form.couponDefinitionId ?? undefined,
    relatedProductIds: form.relatedProductIds.length > 0
      ? [...form.relatedProductIds].sort((left, right) => left - right)
      : undefined,
  };
}

function couponOptionLabel(coupon: AdminCouponResponse): string {
  const status = !coupon.active
    ? "사용 중지"
    : coupon.publiclyClaimable
      ? "회원 직접 발급 설정"
      : "회원 직접 발급 불가";
  return `${coupon.name} (#${coupon.id}) · ${status}`;
}

function updateRequest(form: EventFormState, expectedVersion: number): UpdateEventRequest {
  return { ...createRequest(form), expectedVersion };
}

export function AdminEventSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [form, setForm] = useState<EventFormState>(emptyForm);
  const [actionError, setActionError] = useState<Error | null>(null);
  const edit = useVersionedEdit<EventResponse>({
    onAuthError,
    isConflict: isVersionConflict,
    applyToForm: (event) => setForm(formFrom(event)),
    setError: setActionError,
    onConflict: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.admin.events });
    },
  });

  const eventsQuery = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.events,
    queryFn: () => fetchAdminEvents(adminKey),
  });
  const productsQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "products"],
    queryFn: () => fetchProducts(adminKey),
  });
  const couponsQuery = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.coupons,
    queryFn: () => fetchAdminCoupons(adminKey),
  });

  const invalidateEvents = () => {
    void Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.events }),
      queryClient.invalidateQueries({ queryKey: queryKeys.events.all }),
    ]);
  };

  const resetForm = () => {
    edit.reset();
    setShowForm(false);
    setEditId(null);
    setForm(emptyForm());
    setActionError(null);
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
      updateRequest(form, edit.version!),
      adminKey,
    ),
    onMutate: () => setActionError(null),
    onSuccess: () => {
      toast.show("이벤트가 수정되었습니다.");
      resetForm();
      invalidateEvents();
    },
    onError: (error) => edit.handleSaveError(error, () => fetchAdminEvent(editId!, adminKey)),
  });

  const deleteMutation = useAdminMutation(onAuthError, {
    mutationFn: ({ id, version }: Pick<EventResponse, "id" | "version">) =>
      deleteEvent(id, version, adminKey),
    onMutate: () => setActionError(null),
    onSuccess: () => {
      toast.show("이벤트가 삭제되었습니다.");
      invalidateEvents();
    },
    onError: edit.handleDeleteError,
  });

  const startEdit = (event: EventResponse) => {
    setEditId(event.id);
    setForm(formFrom(event));
    setShowForm(true);
    void edit.startEdit(() => fetchAdminEvent(event.id, adminKey));
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
    else if (edit.version !== null) updateMutation.mutate();
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
            {edit.loading && <LoadingSpinner text="이벤트 불러오는 중..." />}
            {edit.conflict && (
              <VersionConflictAlert
                message="다른 관리자가 먼저 수정했습니다. 작성 중인 초안은 그대로 보존했습니다."
                onKeepDraft={edit.keepDraft}
                onLoadLatest={edit.loadLatest}
              />
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
                      disabled={edit.loading}
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
                      disabled={edit.loading}
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
                      disabled={edit.loading}
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
                      disabled={edit.loading}
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
                      disabled={edit.loading}
                      required
                    />
                  </Form.Group>
                </Col>
                <Col xs={12}>
                  <Form.Group controlId="admin-event-coupon">
                    <Form.Label>이벤트 쿠폰</Form.Label>
                    {couponsQuery.isLoading && <LoadingSpinner text="쿠폰 불러오는 중..." />}
                    <ErrorAlert error={couponsQuery.error} />
                    <Form.Select
                      value={form.couponDefinitionId ?? ""}
                      onChange={(event) => setForm((current) => ({
                        ...current,
                        couponDefinitionId: event.target.value
                          ? Number(event.target.value)
                          : null,
                      }))}
                      disabled={edit.loading || couponsQuery.isLoading}
                    >
                      <option value="">연결하지 않음</option>
                      {couponsQuery.data?.map((coupon) => (
                        <option key={coupon.id} value={coupon.id}>
                          {couponOptionLabel(coupon)}
                        </option>
                      ))}
                    </Form.Select>
                    <Form.Text>
                      이벤트 상세에서 회원이 받을 쿠폰 한 개를 연결합니다. 사용 중이고 회원 직접 발급이 설정된 쿠폰을 발급 기간에만 받을 수 있습니다.
                    </Form.Text>
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
                          disabled={edit.loading}
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
                      disabled={edit.loading}
                    />
                    <Form.Check
                      id="admin-event-featured"
                      type="checkbox"
                      label="홈 추천"
                      checked={form.featured}
                      onChange={(event) => setForm((current) => ({ ...current, featured: event.target.checked }))}
                      disabled={edit.loading}
                    />
                  </div>
                  <Button
                    type="submit"
                    disabled={
                      !valid
                      || edit.loading
                      || edit.conflict !== null
                      || (editId !== null && edit.version === null)
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
                {event.couponDefinitionId !== null
                  ? ` · 이벤트 쿠폰 ${
                    couponsQuery.data?.find((coupon) => coupon.id === event.couponDefinitionId)?.name
                      ?? `#${event.couponDefinitionId}`
                  }`
                  : ""}
              </div>
            </div>
            <div className="d-flex gap-2">
              <Button
                size="sm"
                variant="outline-secondary"
                onClick={() => startEdit(event)}
                disabled={edit.loading}
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
