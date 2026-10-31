import { useState, type FormEvent } from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  Alert,
  Button,
  Col,
  Form,
  Row,
  Table,
} from "react-bootstrap";
import { BadgeDollarSign, ChevronRight, RefreshCcw, Search } from "lucide-react";
import {
  expirePasses,
  getAdminPass,
  refundPass,
  searchAdminPasses,
} from "./api";
import {
  EmptyState,
  ErrorAlert,
  LoadingSpinner,
  StatusBadge,
  useToast,
} from "@/shared/ui";
import { ConfirmModal } from "@/shared/ui/ConfirmModal";
import { visibleAdminError } from "@/shared/hooks/adminSessionUnauthorized";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { ADMIN_REFUND_STATUS_LABEL } from "@/features/admin-refund/refundStatusLabel";
import { useAdminRefundPolling } from "@/features/admin-refund/useAdminRefundPolling";
import { AdminSearchPagination } from "@/features/admin-search/AdminSearchPagination";
import type { AdminPassResponse } from "@/generated/api/adminOperations";
import { queryKeys } from "@/shared/api";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

const PAGE_SIZE = 10;
const REFUNDABLE_STATUSES = new Set<AdminPassResponse["status"]>(["ACTIVE", "USED_UP"]);
export function PassActionPanel({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { trackRefund } = useAdminRefundPolling(adminKey, onAuthError);
  const [keyword, setKeyword] = useState("");
  const [submittedKeyword, setSubmittedKeyword] = useState("");
  const [page, setPage] = useState(0);
  const [selectedPassId, setSelectedPassId] = useState<number | null>(null);
  const [showRefundConfirm, setShowRefundConfirm] = useState(false);

  const passSearch = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.passes.search(submittedKeyword, page),
    queryFn: () => searchAdminPasses(
      adminKey,
      submittedKeyword || undefined,
      page,
      PAGE_SIZE,
    ),
  });

  const passDetail = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.passes.detail(selectedPassId),
    queryFn: () => getAdminPass(adminKey, selectedPassId!),
    enabled: selectedPassId !== null,
  });

  const expire = useAdminMutation(onAuthError, {
    mutationFn: () => expirePasses(adminKey),
    onSuccess: (result) => {
      toast.show(`기간 지난 이용권 처리: 완료 ${result.successCount}건 · 확인 필요 ${result.failureCount}건`);
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.passes.all });
    },
  });

  const refund = useAdminMutation(onAuthError, {
    mutationFn: (passId: number) => refundPass(adminKey, passId),
    onSuccess: (result, refundedPassId) => {
      setShowRefundConfirm(false);
      if (result.refundId != null && result.refundStatus != null) {
        toast.show(
          `환불 요청 접수: ${result.refundCredits}회분 ${formatKRW(result.refundAmount)}, 취소 예약 ${result.canceledBookings}건`,
          "info",
        );
        trackRefund(
          result.refundId,
          passDetail.data?.passNumber ?? `이용권 #${refundedPassId}`,
        );
      } else {
        toast.show(`환불할 금액이 없습니다. 함께 취소된 예약은 ${result.canceledBookings}건입니다.`);
      }
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.passes.all });
    },
  });

  const selectedPass = passDetail.data;
  const refundable = selectedPass ? REFUNDABLE_STATUSES.has(selectedPass.status) : false;
  const pending = expire.isPending || refund.isPending;

  function handleSearch(event: FormEvent) {
    event.preventDefault();
    setPage(0);
    setSelectedPassId(null);
    setShowRefundConfirm(false);
    setSubmittedKeyword(keyword.trim());
  }

  function movePage(nextPage: number) {
    setPage(nextPage);
    setSelectedPassId(null);
    setShowRefundConfirm(false);
  }

  return (
    <div>
      <Form onSubmit={handleSearch}>
        <Row className="g-2 align-items-end">
          <Col xs={12} md={7} lg={5}>
            <Form.Group controlId="admin-pass-search-keyword">
              <Form.Label>이용권 또는 고객 검색</Form.Label>
              <Form.Control
                type="search"
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="이용권 번호, 고객명, 휴대폰 번호"
              />
            </Form.Group>
          </Col>
          <Col xs="auto">
            <Button type="submit" variant="dark" disabled={passSearch.isFetching}>
              <Search size={16} aria-hidden="true" className="me-1" />
              {passSearch.isFetching ? "검색 중..." : "검색"}
            </Button>
          </Col>
        </Row>
      </Form>

      {passSearch.isLoading && <LoadingSpinner text="이용권 조회 중..." />}
      <ErrorAlert error={visibleAdminError(passSearch.error)} />

      {passSearch.data?.content.length === 0 && (
        <EmptyState message="검색 조건에 맞는 이용권이 없습니다." />
      )}

      {passSearch.data && passSearch.data.content.length > 0 && (
        <div className="admin-search-results">
          <Table responsive hover size="sm" className="mb-0 align-middle">
            <thead>
              <tr>
                <th>이용권 번호</th>
                <th>고객</th>
                <th>상태</th>
                <th>잔여</th>
                <th>만료일</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {passSearch.data.content.map((pass) => (
                <tr key={pass.passId} className={selectedPassId === pass.passId ? "table-active" : undefined}>
                  <td>{pass.passNumber}</td>
                  <td>
                    <div>{pass.customerName}</div>
                    <small className="text-muted-soft">{pass.customerPhone ?? "-"}</small>
                  </td>
                  <td><StatusBadge status={pass.status} audience="admin" /></td>
                  <td>{pass.remainingCredits}/{pass.totalCredits}회</td>
                  <td><small>{formatDateTime(pass.expiresAt)}</small></td>
                  <td className="text-end">
                    <Button
                      type="button"
                      size="sm"
                      variant={selectedPassId === pass.passId ? "dark" : "outline-secondary"}
                      onClick={() => setSelectedPassId(pass.passId)}
                    >
                      상세
                      <ChevronRight size={14} aria-hidden="true" className="ms-1" />
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>

          <AdminSearchPagination
            page={passSearch.data}
            onMove={movePage}
            disabled={passSearch.isFetching}
            ariaLabel="이용권 검색 결과 페이지 이동"
          />
        </div>
      )}

      {selectedPassId !== null && (
        <section className="border-top mt-4 pt-4" aria-labelledby="admin-pass-detail-title">
          {passDetail.isLoading && <LoadingSpinner text="이용권 상세 조회 중..." />}
          <ErrorAlert error={visibleAdminError(passDetail.error)} />

          {selectedPass && (
            <>
              <div className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-3">
                <div>
                  <h6 id="admin-pass-detail-title" className="mb-1">{selectedPass.passNumber}</h6>
                  <div className="small text-muted-soft">
                    {selectedPass.customerName} · {selectedPass.customerPhone ?? "휴대폰 번호 없음"}
                  </div>
                </div>
                <StatusBadge status={selectedPass.status} audience="admin" />
              </div>

              <dl className="row small mb-3">
                <dt className="col-6 col-md-3">잔여 횟수</dt>
                <dd className="col-6 col-md-3">{selectedPass.remainingCredits}/{selectedPass.totalCredits}회</dd>
                <dt className="col-6 col-md-3">미래 예약</dt>
                <dd className="col-6 col-md-3">{selectedPass.futureBookingCount}건</dd>
                <dt className="col-6 col-md-3">예상 환불액</dt>
                <dd className="col-6 col-md-3 fw-semibold">{formatKRW(selectedPass.expectedRefundAmount)}</dd>
                <dt className="col-6 col-md-3">만료일</dt>
                <dd className="col-6 col-md-3">{formatDateTime(selectedPass.expiresAt)}</dd>
                {selectedPass.refundStatus && (
                  <>
                    <dt className="col-6 col-md-3">환불 상태</dt>
                    <dd className="col-6 col-md-3">{ADMIN_REFUND_STATUS_LABEL[selectedPass.refundStatus]}</dd>
                  </>
                )}
              </dl>

              <Button
                type="button"
                variant="danger"
                disabled={!refundable || pending}
                title={!refundable ? "현재 상태에서는 새 환불을 시작할 수 없습니다." : undefined}
                onClick={() => {
                  refund.reset();
                  setShowRefundConfirm(true);
                }}
              >
                <BadgeDollarSign size={17} aria-hidden="true" className="me-1" />
                전체 환불
              </Button>
            </>
          )}
        </section>
      )}

      <section className="border-top mt-4 pt-3">
        <div className="d-flex flex-wrap justify-content-between align-items-center gap-2">
          <div>
            <div className="fw-semibold small">만료 처리</div>
            <div className="text-muted-soft small">현재 시각을 지난 이용권을 즉시 만료 처리합니다.</div>
          </div>
          <Button
            variant="outline-secondary"
            size="sm"
            disabled={pending}
            onClick={() => expire.mutate()}
          >
            <RefreshCcw size={14} aria-hidden="true" className="me-1" />
            {expire.isPending ? "처리 중..." : "기간 지난 이용권 만료 처리"}
          </Button>
        </div>
        <ErrorAlert error={visibleAdminError(expire.error)} />
      </section>

      <ConfirmModal
        show={showRefundConfirm}
        title="이용권 전체 환불 확인"
        pending={refund.isPending}
        onClose={() => setShowRefundConfirm(false)}
        confirmLabel={refund.isPending ? "환불 처리 중..." : "전체 환불 확정"}
        confirmVariant="danger"
        confirmDisabled={!selectedPass || !refundable}
        onConfirm={() => selectedPass && refund.mutate(selectedPass.passId)}
      >
        <ErrorAlert error={visibleAdminError(refund.error)} />
        {selectedPass && (
          <>
            <p className="mb-3">
              <strong>{selectedPass.passNumber}</strong>을 전체 환불하고 남은 이용 권한을 종료합니다.
            </p>
            <dl className="row small mb-3">
              <dt className="col-6">현재 상태</dt>
              <dd className="col-6"><StatusBadge status={selectedPass.status} audience="admin" /></dd>
              <dt className="col-6">잔여 횟수</dt>
              <dd className="col-6">{selectedPass.remainingCredits}/{selectedPass.totalCredits}회</dd>
              <dt className="col-6">자동 취소될 미래 예약</dt>
              <dd className="col-6 fw-semibold">{selectedPass.futureBookingCount}건</dd>
              <dt className="col-6">예상 환불액</dt>
              <dd className="col-6 fw-semibold">{formatKRW(selectedPass.expectedRefundAmount)}</dd>
            </dl>
            <Alert variant="warning" className="mb-0 py-2 small">
              미래 예약은 자동 취소되고 잔여 횟수는 0회가 됩니다. 실행 후에는 되돌릴 수 없습니다.
            </Alert>
          </>
        )}
      </ConfirmModal>
    </div>
  );
}
