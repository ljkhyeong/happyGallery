import { useInfiniteQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Card, Col, ListGroup, Row } from "react-bootstrap";
import { Link } from "react-router";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import {
  queryKeys,
  runForCustomerSession,
  cursorPageParams,
  flattenCursorPages,
  type CursorPage,
} from "@/shared/api";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { bookingNumberLabel, orderNumberLabel } from "@/shared/lib/labels";
import { EmptyState, ErrorAlert, LoadingSpinner, StatusBadge, CursorLoadMore, CursorQueryErrorAlert } from "@/shared/ui";
import {
  fetchRecoveredGuestBookings,
  fetchRecoveredGuestOrders,
  recoverGuestRecords,
} from "./api";
import {
  clearGuestRecordRecovery,
  loadGuestRecordRecovery,
  saveGuestRecordRecovery,
  type GuestRecordRecoverySession,
} from "./session";
import { useOwnedRecoveryResult, type OwnedRecoveryView } from "./useOwnedRecoveryResult";

type RecoveryView = OwnedRecoveryView<GuestRecordRecoverySession>;

/** 재발급한 조회 코드로 비회원 주문·예약 이력을 이어 받는다. 키에는 토큰 대신 소유자·만료 시각을 쓴다. */
function useRecoveredRecords<TPage extends CursorPage>(
  kind: "orders" | "bookings",
  recoveryView: RecoveryView | null,
  fetchPage: (accessToken: string, cursor?: string, signal?: AbortSignal) => Promise<TPage>,
) {
  const owner = recoveryView?.storage.owner;
  return useInfiniteQuery({
    queryKey: queryKeys.member.guestRecovery.records(
      kind,
      owner?.boundaryEpoch ?? null,
      owner?.boundaryCustomerId ?? null,
      recoveryView?.customerSession.version ?? -1,
      recoveryView?.storage.value.expiresAt ?? "inactive",
    ),
    queryFn: ({ pageParam, signal }): Promise<TPage> => {
      if (!recoveryView) {
        return Promise.reject(new Error("휴대폰 번호를 인증하고 조회 코드를 다시 발급받아 주세요."));
      }
      return runForCustomerSession(
        recoveryView.customerSession,
        () => fetchPage(recoveryView.storage.value.accessToken, pageParam, signal),
      );
    },
    ...cursorPageParams,
    enabled: recoveryView !== null,
  });
}

export function GuestRecordRecoverySection() {
  const queryClient = useQueryClient();
  // 새 조회 코드를 보이기 전·지운 뒤에는 이전 코드로 받은 이력 캐시를 남기지 않는다.
  const recoveredRecords = { queryKey: queryKeys.member.guestRecovery.all };
  const { view: recoveryView, mutation: recovery, reset } = useOwnedRecoveryResult({
    load: loadGuestRecordRecovery,
    save: saveGuestRecordRecovery,
    clear: clearGuestRecordRecovery,
    request: recoverGuestRecords,
    beforeShow: async () => {
      await queryClient.cancelQueries(recoveredRecords);
      queryClient.removeQueries(recoveredRecords);
    },
    afterClear: () => {
      void queryClient.cancelQueries(recoveredRecords);
      queryClient.removeQueries(recoveredRecords);
    },
  });

  const result = recoveryView?.storage.value ?? null;
  const ordersQuery = useRecoveredRecords("orders", recoveryView, fetchRecoveredGuestOrders);
  const bookingsQuery = useRecoveredRecords("bookings", recoveryView, fetchRecoveredGuestBookings);
  const orders = flattenCursorPages(ordersQuery.data);
  const bookings = flattenCursorPages(bookingsQuery.data);
  const recoveryPagesLoading = recoveryView !== null
    && (ordersQuery.isLoading || bookingsQuery.isLoading);
  const recoveryPagesLoaded = ordersQuery.data !== undefined
    && bookingsQuery.data !== undefined;
  const hasRecords = orders.length > 0 || bookings.length > 0;

  return (
    <Card className="mb-4">
      <Card.Body className="p-4">
        <PhoneVerificationStep
          purpose="GUEST_RECORD_RECOVERY"
          title="주문·예약 조회 코드 재발급"
          titleAs="h3"
          description="주문 번호나 조회 코드를 잃어버렸다면 결제·예약 때 사용한 휴대폰 번호를 인증하세요. 기존 조회 코드는 폐기되고 새 코드가 발급됩니다."
          confirmLabel="조회 코드 재발급"
          confirming={recovery.isPending}
          onReset={reset}
          onVerified={(phone, code) => recovery.mutate({ phone, code })}
        />

        <ErrorAlert error={recovery.error} />

        {recoveryPagesLoading && (
          <div className="mt-4">
            <LoadingSpinner text="주문·예약을 불러오는 중..." />
          </div>
        )}

        <CursorQueryErrorAlert query={ordersQuery} />
        <CursorQueryErrorAlert query={bookingsQuery} />

        {result && recoveryPagesLoaded && !hasRecords && (
          <EmptyState message="인증한 번호로 확인할 수 있는 비회원 주문이나 예약이 없습니다." />
        )}

        {result && hasRecords && recoveryView && (
          <div className="mt-4">
            <Alert variant="success">
              조회 코드를 재발급했습니다. 새 코드는 {formatDateTime(result.expiresAt)}까지 사용할 수 있습니다.
            </Alert>

            <Row className="g-4">
              {orders.length > 0 && (
                <Col xs={12} lg={6}>
                  <h3 className="h6">비회원 주문 · 불러온 {orders.length}건</h3>
                  <ListGroup>
                    {orders.map((order) => (
                      <ListGroup.Item
                        key={order.orderId}
                        as={Link}
                        to={`/guest/orders?orderId=${order.orderId}`}
                        state={{
                          orderId: order.orderId,
                          token: result.accessToken,
                          customerSession: recoveryView.customerSession,
                        }}
                        action
                        className="d-flex justify-content-between align-items-start gap-3"
                      >
                        <span>
                          <strong className="d-block">{orderNumberLabel(order.orderId)}</strong>
                          <small className="text-muted-soft">
                            {formatKRW(order.totalAmount)} · {formatDateTime(order.createdAt)}
                          </small>
                        </span>
                        <StatusBadge status={order.status} />
                      </ListGroup.Item>
                    ))}
                  </ListGroup>
                  <CursorLoadMore
                    query={ordersQuery}
                    label="비회원 주문 더 보기"
                    loadingLabel="주문 불러오는 중..."
                    wrapperClassName="d-grid mt-2"
                  />
                </Col>
              )}

              {bookings.length > 0 && (
                <Col xs={12} lg={6}>
                  <h3 className="h6">비회원 예약 · 불러온 {bookings.length}건</h3>
                  <ListGroup>
                    {bookings.map((booking) => (
                      <ListGroup.Item
                        key={booking.bookingId}
                        as={Link}
                        to={`/guest/bookings?bookingId=${booking.bookingId}`}
                        state={{
                          bookingId: booking.bookingId,
                          token: result.accessToken,
                          customerSession: recoveryView.customerSession,
                        }}
                        action
                        className="d-flex justify-content-between align-items-start gap-3"
                      >
                        <span>
                          <strong className="d-block">{booking.className}</strong>
                          <small className="text-muted-soft">
                            {bookingNumberLabel(booking.bookingId)} · {formatDateTime(booking.startAt)}
                          </small>
                        </span>
                        <StatusBadge status={booking.status} />
                      </ListGroup.Item>
                    ))}
                  </ListGroup>
                  <CursorLoadMore
                    query={bookingsQuery}
                    label="비회원 예약 더 보기"
                    loadingLabel="예약 불러오는 중..."
                    wrapperClassName="d-grid mt-2"
                  />
                </Col>
              )}
            </Row>
          </div>
        )}
      </Card.Body>
    </Card>
  );
}
