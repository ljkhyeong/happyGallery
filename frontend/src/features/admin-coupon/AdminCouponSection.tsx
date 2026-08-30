import { useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Alert, Badge, Button, Card, Col, Form, Row } from "react-bootstrap";
import {
  createCoupon,
  deleteCoupon,
  fetchAdminCoupon,
  fetchAdminCoupons,
  updateCoupon,
  type AdminCouponResponse,
  type CreateCouponRequest,
  type UpdateCouponRequest,
} from "./api";
import { ApiError, queryKeys } from "@/shared/api";
import { isAdminSessionUnauthorized } from "@/shared/hooks/adminSessionUnauthorized";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

interface CouponFormState {
  name: string;
  discountType: "FIXED" | "PERCENT";
  discountValue: string;
  minOrderAmount: string;
  maxDiscountAmount: string;
  validFrom: string;
  validUntil: string;
  active: boolean;
  publiclyClaimable: boolean;
}

function emptyForm(): CouponFormState {
  return {
    name: "",
    discountType: "FIXED",
    discountValue: "",
    minOrderAmount: "0",
    maxDiscountAmount: "",
    validFrom: "",
    validUntil: "",
    active: true,
    publiclyClaimable: false,
  };
}

function formFrom(coupon: AdminCouponResponse): CouponFormState {
  return {
    name: coupon.name,
    discountType: coupon.discountType,
    discountValue: String(coupon.discountValue),
    minOrderAmount: String(coupon.minOrderAmount),
    maxDiscountAmount: coupon.maxDiscountAmount === null
      ? ""
      : String(coupon.maxDiscountAmount),
    validFrom: coupon.validFrom.slice(0, 16),
    validUntil: coupon.validUntil.slice(0, 16),
    active: coupon.active,
    publiclyClaimable: coupon.publiclyClaimable,
  };
}

function positiveInteger(value: string): number | null {
  const parsed = Number(value);
  return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : null;
}

function nonNegativeInteger(value: string): number | null {
  const parsed = Number(value);
  return Number.isSafeInteger(parsed) && parsed >= 0 ? parsed : null;
}

function createRequest(form: CouponFormState): CreateCouponRequest | null {
  const discountValue = positiveInteger(form.discountValue);
  const minOrderAmount = nonNegativeInteger(form.minOrderAmount);
  const maxDiscountAmount = form.discountType === "PERCENT"
    ? positiveInteger(form.maxDiscountAmount)
    : null;
  if (
    !form.name.trim()
    || discountValue === null
    || minOrderAmount === null
    || (form.discountType === "PERCENT" && (
      discountValue > 100 || maxDiscountAmount === null
    ))
    || !form.validFrom
    || !form.validUntil
    || form.validFrom >= form.validUntil
  ) {
    return null;
  }

  return {
    name: form.name.trim(),
    discountType: form.discountType,
    discountValue,
    minOrderAmount,
    ...(maxDiscountAmount === null ? {} : { maxDiscountAmount }),
    validFrom: form.validFrom,
    validUntil: form.validUntil,
    active: form.active,
    publiclyClaimable: form.publiclyClaimable,
  };
}

function updateRequest(
  form: CouponFormState,
  expectedVersion: number,
): UpdateCouponRequest | null {
  const request = createRequest(form);
  return request ? { ...request, expectedVersion } : null;
}

function discountLabel(coupon: AdminCouponResponse): string {
  return coupon.discountType === "FIXED"
    ? `${formatKRW(coupon.discountValue)} 정액 할인`
    : `${coupon.discountValue}% 할인 · 최대 ${formatKRW(coupon.maxDiscountAmount ?? 0)}`;
}

export function AdminCouponSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const editRequestId = useRef(0);
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [editVersion, setEditVersion] = useState<number | null>(null);
  const [form, setForm] = useState<CouponFormState>(emptyForm);
  const [editLoading, setEditLoading] = useState(false);
  const [actionError, setActionError] = useState<Error | null>(null);
  const [conflict, setConflict] = useState<AdminCouponResponse | null>(null);

  const couponsQuery = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.coupons,
    queryFn: () => fetchAdminCoupons(adminKey),
  });

  const invalidateCoupons = () => {
    void Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.coupons }),
      queryClient.invalidateQueries({ queryKey: queryKeys.member.coupons }),
      queryClient.invalidateQueries({ queryKey: queryKeys.member.claimableCoupons }),
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
    mutationFn: () => createCoupon(createRequest(form)!, adminKey),
    onMutate: () => setActionError(null),
    onSuccess: () => {
      toast.show("쿠폰을 등록했습니다.");
      resetForm();
      invalidateCoupons();
    },
    onError: setActionError,
  });

  const updateMutation = useAdminMutation(onAuthError, {
    mutationFn: () => updateCoupon(
      editId!,
      updateRequest(form, editVersion!)!,
      adminKey,
    ),
    onMutate: () => setActionError(null),
    onSuccess: () => {
      toast.show("쿠폰을 수정했습니다.");
      resetForm();
      invalidateCoupons();
    },
    onError: async (error) => {
      if (
        !(error instanceof ApiError)
        || error.status !== 409
        || error.code !== "CONFLICT"
        || editId === null
      ) {
        setActionError(error);
        return;
      }
      const requestId = editRequestId.current;
      setEditLoading(true);
      try {
        const latest = await fetchAdminCoupon(editId, adminKey);
        if (requestId !== editRequestId.current) return;
        setConflict(latest);
        setActionError(null);
        void queryClient.invalidateQueries({ queryKey: queryKeys.admin.coupons });
      } catch (refreshError) {
        if (requestId !== editRequestId.current) return;
        if (isAdminSessionUnauthorized(refreshError)) onAuthError();
        setActionError(
          refreshError instanceof Error
            ? refreshError
            : new Error("최신 쿠폰 정보를 불러오지 못했습니다."),
        );
      } finally {
        if (requestId === editRequestId.current) setEditLoading(false);
      }
    },
  });

  const deleteMutation = useAdminMutation(onAuthError, {
    mutationFn: ({ id, version }: Pick<AdminCouponResponse, "id" | "version">) =>
      deleteCoupon(id, version, adminKey),
    onMutate: () => setActionError(null),
    onSuccess: () => {
      toast.show("쿠폰 사용을 중지했습니다.");
      invalidateCoupons();
    },
    onError: (error) => {
      setActionError(error);
      if (error instanceof ApiError && error.status === 409) {
        void queryClient.invalidateQueries({ queryKey: queryKeys.admin.coupons });
      }
    },
  });

  const startEdit = async (coupon: AdminCouponResponse) => {
    const requestId = ++editRequestId.current;
    setEditId(coupon.id);
    setEditVersion(null);
    setForm(formFrom(coupon));
    setShowForm(true);
    setEditLoading(true);
    setActionError(null);
    setConflict(null);
    try {
      const latest = await fetchAdminCoupon(coupon.id, adminKey);
      if (requestId !== editRequestId.current) return;
      setForm(formFrom(latest));
      setEditVersion(latest.version);
    } catch (error) {
      if (requestId !== editRequestId.current) return;
      if (isAdminSessionUnauthorized(error)) onAuthError();
      setActionError(error instanceof Error ? error : new Error("쿠폰을 불러오지 못했습니다."));
    } finally {
      if (requestId === editRequestId.current) setEditLoading(false);
    }
  };

  const request = createRequest(form);
  const saving = createMutation.isPending || updateMutation.isPending;

  return (
    <div>
      <div className="d-flex justify-content-end mb-3">
        <Button
          type="button"
          size="sm"
          variant="outline-primary"
          onClick={() => {
            if (showForm) resetForm();
            else {
              resetForm();
              setShowForm(true);
            }
          }}
        >
          {showForm ? "취소" : "새 쿠폰 등록"}
        </Button>
      </div>

      <ErrorAlert error={actionError} />

      {showForm && (
        <Card className="mb-3">
          <Card.Body>
            {editLoading && <LoadingSpinner text="쿠폰 정보를 불러오는 중입니다" />}
            {conflict && (
              <Alert variant="warning">
                <p className="mb-2">다른 관리자가 먼저 수정했습니다. 작성 중인 초안은 보존했습니다.</p>
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

            <Form onSubmit={(event) => {
              event.preventDefault();
              if (!request) return;
              if (editId === null) createMutation.mutate();
              else if (editVersion !== null) updateMutation.mutate();
            }}>
              <Row className="g-3">
                <Col md={8}>
                  <Form.Group controlId="admin-coupon-name">
                    <Form.Label>쿠폰 이름</Form.Label>
                    <Form.Control
                      value={form.name}
                      maxLength={100}
                      required
                      disabled={editLoading}
                      onChange={(event) => setForm({ ...form, name: event.target.value })}
                    />
                  </Form.Group>
                </Col>
                <Col md={4}>
                  <Form.Group controlId="admin-coupon-discount-type">
                    <Form.Label>할인 방식</Form.Label>
                    <Form.Select
                      value={form.discountType}
                      disabled={editLoading}
                      onChange={(event) => setForm({
                        ...form,
                        discountType: event.target.value as CouponFormState["discountType"],
                        maxDiscountAmount: event.target.value === "FIXED"
                          ? ""
                          : form.maxDiscountAmount,
                      })}
                    >
                      <option value="FIXED">정액</option>
                      <option value="PERCENT">정률</option>
                    </Form.Select>
                  </Form.Group>
                </Col>
                <Col md={4}>
                  <Form.Group controlId="admin-coupon-discount-value">
                    <Form.Label>{form.discountType === "FIXED" ? "할인 금액" : "할인율 (%)"}</Form.Label>
                    <Form.Control
                      type="number"
                      min={1}
                      max={form.discountType === "PERCENT" ? 100 : undefined}
                      step={1}
                      value={form.discountValue}
                      required
                      disabled={editLoading}
                      onChange={(event) => setForm({ ...form, discountValue: event.target.value })}
                    />
                  </Form.Group>
                </Col>
                <Col md={4}>
                  <Form.Group controlId="admin-coupon-min-order-amount">
                    <Form.Label>최소 상품 금액</Form.Label>
                    <Form.Control
                      type="number"
                      min={0}
                      step={1}
                      value={form.minOrderAmount}
                      required
                      disabled={editLoading}
                      onChange={(event) => setForm({ ...form, minOrderAmount: event.target.value })}
                    />
                  </Form.Group>
                </Col>
                <Col md={4}>
                  <Form.Group controlId="admin-coupon-max-discount-amount">
                    <Form.Label>최대 할인 금액</Form.Label>
                    <Form.Control
                      type="number"
                      min={1}
                      step={1}
                      value={form.maxDiscountAmount}
                      required={form.discountType === "PERCENT"}
                      disabled={editLoading || form.discountType === "FIXED"}
                      placeholder={form.discountType === "FIXED" ? "정액 쿠폰은 사용하지 않음" : "필수"}
                      onChange={(event) => setForm({ ...form, maxDiscountAmount: event.target.value })}
                    />
                  </Form.Group>
                </Col>
                <Col md={6}>
                  <Form.Group controlId="admin-coupon-valid-from">
                    <Form.Label>사용 시작</Form.Label>
                    <Form.Control
                      type="datetime-local"
                      value={form.validFrom}
                      required
                      disabled={editLoading}
                      onChange={(event) => setForm({ ...form, validFrom: event.target.value })}
                    />
                  </Form.Group>
                </Col>
                <Col md={6}>
                  <Form.Group controlId="admin-coupon-valid-until">
                    <Form.Label>사용 종료</Form.Label>
                    <Form.Control
                      type="datetime-local"
                      value={form.validUntil}
                      required
                      disabled={editLoading}
                      isInvalid={Boolean(form.validFrom && form.validUntil && form.validFrom >= form.validUntil)}
                      onChange={(event) => setForm({ ...form, validUntil: event.target.value })}
                    />
                    <Form.Control.Feedback type="invalid">
                      사용 종료 시각은 시작 시각보다 뒤여야 합니다.
                    </Form.Control.Feedback>
                  </Form.Group>
                </Col>
              </Row>

              <div className="d-flex flex-wrap gap-4 my-3">
                <Form.Check
                  id="admin-coupon-active"
                  type="checkbox"
                  label="사용 가능"
                  checked={form.active}
                  disabled={editLoading}
                  onChange={(event) => setForm({ ...form, active: event.target.checked })}
                />
                <Form.Check
                  id="admin-coupon-publicly-claimable"
                  type="checkbox"
                  label="회원이 직접 발급 가능"
                  checked={form.publiclyClaimable}
                  disabled={editLoading}
                  onChange={(event) => setForm({ ...form, publiclyClaimable: event.target.checked })}
                />
              </div>

              <Button
                type="submit"
                size="sm"
                disabled={
                  editLoading
                  || saving
                  || request === null
                  || conflict !== null
                  || (editId !== null && editVersion === null)
                }
              >
                {saving ? "저장 중..." : editId === null ? "등록" : "수정"}
              </Button>
            </Form>
          </Card.Body>
        </Card>
      )}

      {couponsQuery.isLoading && <LoadingSpinner />}
      <ErrorAlert error={couponsQuery.error} />
      {couponsQuery.data?.length === 0 && <EmptyState message="등록된 쿠폰이 없습니다." />}

      {couponsQuery.data?.map((coupon) => (
        <Card key={coupon.id} className="mb-2">
          <Card.Body className="d-flex flex-wrap justify-content-between align-items-start gap-3 py-3">
            <div>
              <div className="d-flex flex-wrap gap-2 align-items-center mb-1">
                <strong>{coupon.name}</strong>
                <Badge bg={coupon.active ? "success" : "secondary"}>
                  {coupon.active ? "사용 가능" : "사용 중지"}
                </Badge>
                {coupon.publiclyClaimable && <Badge bg="info">공개 발급</Badge>}
              </div>
              <div className="small">{discountLabel(coupon)}</div>
              <small className="text-muted-soft">
                최소 {formatKRW(coupon.minOrderAmount)} · {formatDateTime(coupon.validFrom)} ~ {formatDateTime(coupon.validUntil)}
              </small>
            </div>
            <div className="d-flex gap-2">
              <Button
                type="button"
                size="sm"
                variant="outline-secondary"
                disabled={editLoading}
                onClick={() => { void startEdit(coupon); }}
              >
                수정
              </Button>
              <Button
                type="button"
                size="sm"
                variant="outline-danger"
                disabled={!coupon.active || deleteMutation.isPending}
                onClick={() => {
                  if (confirm("이 쿠폰의 신규 발급과 사용을 중지하시겠습니까?")) {
                    deleteMutation.mutate({ id: coupon.id, version: coupon.version });
                  }
                }}
              >
                사용 중지
              </Button>
            </div>
          </Card.Body>
        </Card>
      ))}
    </div>
  );
}
