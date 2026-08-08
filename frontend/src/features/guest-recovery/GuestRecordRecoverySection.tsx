import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Alert, Button, Card, Col, ListGroup, Row } from "react-bootstrap";
import { Link } from "react-router";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import {
  captureCustomerSession,
  queryKeys,
  runForCustomerSession,
  runForCurrentCustomer,
  type CustomerSessionSnapshot,
} from "@/shared/api";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { ErrorAlert, LoadingSpinner, StatusBadge } from "@/shared/ui";
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

interface RecoveryView {
  customerSession: CustomerSessionSnapshot;
  storage: GuestRecordRecoverySession;
}

function loadRecoveryView(): RecoveryView | null {
  const storage = loadGuestRecordRecovery();
  return storage
    ? { customerSession: captureCustomerSession(), storage }
    : null;
}

export function GuestRecordRecoverySection() {
  const queryClient = useQueryClient();
  const [recoveryView, setRecoveryView] = useState(loadRecoveryView);
  const recovery = useMutation({
    mutationFn: async ({ phone, code }: { phone: string; code: string }) => {
      let storedRecovery: GuestRecordRecoverySession | null = null;
      try {
        return await runForCurrentCustomer(
          () => recoverGuestRecords(phone, code),
          async (result, requireCurrent) => {
            requireCurrent();
            const customerSession = captureCustomerSession();
            storedRecovery = saveGuestRecordRecovery(result, customerSession);
            requireCurrent();
            if (storedRecovery) {
              await queryClient.cancelQueries({
                queryKey: queryKeys.member.guestRecovery.all,
              });
              queryClient.removeQueries({
                queryKey: queryKeys.member.guestRecovery.all,
              });
              requireCurrent();
              setRecoveryView({ customerSession, storage: storedRecovery });
            }
            return result;
          },
        );
      } catch (error) {
        if (storedRecovery) {
          const expectedRecovery = storedRecovery;
          clearGuestRecordRecovery(expectedRecovery);
          setRecoveryView((current) =>
            current?.storage === expectedRecovery ? null : current);
        }
        throw error;
      }
    },
  });

  const result = recoveryView?.storage.value ?? null;
  const owner = recoveryView?.storage.owner;
  const recoverySessionVersion = recoveryView?.customerSession.version ?? -1;
  const recoveryExpiresAt = result?.expiresAt ?? "inactive";
  const ordersQuery = useInfiniteQuery({
    queryKey: queryKeys.member.guestRecovery.orders(
      owner?.boundaryEpoch ?? null,
      owner?.boundaryCustomerId ?? null,
      recoverySessionVersion,
      recoveryExpiresAt,
    ),
    queryFn: ({ pageParam, signal }) => {
      if (!recoveryView) {
        return Promise.reject(new Error("복구 세션이 없습니다."));
      }
      return runForCustomerSession(
        recoveryView.customerSession,
        () => fetchRecoveredGuestOrders(
          recoveryView.storage.value.accessToken,
          pageParam,
          signal,
        ),
      );
    },
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? lastPage.nextCursor ?? undefined : undefined,
    enabled: recoveryView !== null,
  });
  const bookingsQuery = useInfiniteQuery({
    queryKey: queryKeys.member.guestRecovery.bookings(
      owner?.boundaryEpoch ?? null,
      owner?.boundaryCustomerId ?? null,
      recoverySessionVersion,
      recoveryExpiresAt,
    ),
    queryFn: ({ pageParam, signal }) => {
      if (!recoveryView) {
        return Promise.reject(new Error("복구 세션이 없습니다."));
      }
      return runForCustomerSession(
        recoveryView.customerSession,
        () => fetchRecoveredGuestBookings(
          recoveryView.storage.value.accessToken,
          pageParam,
          signal,
        ),
      );
    },
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? lastPage.nextCursor ?? undefined : undefined,
    enabled: recoveryView !== null,
  });
  const orders = ordersQuery.data?.pages.flatMap((page) => page.content) ?? [];
  const bookings = bookingsQuery.data?.pages.flatMap((page) => page.content) ?? [];
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
          title="주문·예약 조회 정보 복구"
          description="주문 번호나 조회 코드를 잃어버렸다면 결제·예약 때 사용한 휴대폰 번호를 인증하세요. 기존 조회 코드는 폐기되고 새 코드가 발급됩니다."
          confirmLabel="조회 정보 복구"
          confirming={recovery.isPending}
          onReset={() => {
            recovery.reset();
            clearGuestRecordRecovery(recoveryView?.storage);
            void queryClient.cancelQueries({
              queryKey: queryKeys.member.guestRecovery.all,
            });
            queryClient.removeQueries({
              queryKey: queryKeys.member.guestRecovery.all,
            });
            setRecoveryView(null);
          }}
          onVerified={(phone, code) => recovery.mutate({ phone, code })}
        />

        <ErrorAlert error={recovery.error} />

        {recoveryPagesLoading && (
          <div className="mt-4">
            <LoadingSpinner text="복구한 주문·예약을 불러오는 중..." />
          </div>
        )}

        <ErrorAlert
          error={ordersQuery.error}
          onRetry={() => { void ordersQuery.refetch(); }}
          retrying={ordersQuery.isFetching && !ordersQuery.isFetchingNextPage}
        />
        <ErrorAlert
          error={bookingsQuery.error}
          onRetry={() => { void bookingsQuery.refetch(); }}
          retrying={bookingsQuery.isFetching && !bookingsQuery.isFetchingNextPage}
        />

        {result && recoveryPagesLoaded && !hasRecords && (
          <Alert variant="light" className="mt-4 mb-0">
            인증한 번호로 확인할 수 있는 비회원 주문이나 예약이 없습니다.
          </Alert>
        )}

        {result && hasRecords && recoveryView && (
          <div className="mt-4">
            <Alert variant="success">
              조회 정보를 복구했습니다. 새 조회 코드는 {formatDateTime(result.expiresAt)}까지 사용할 수 있습니다.
            </Alert>

            <Row className="g-4">
              {orders.length > 0 && (
                <Col xs={12} lg={6}>
                  <h6>비회원 주문 · 불러온 {orders.length}건</h6>
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
                          <strong className="d-block">주문 #{order.orderId}</strong>
                          <small className="text-muted-soft">
                            {formatKRW(order.totalAmount)} · {formatDateTime(order.createdAt)}
                          </small>
                        </span>
                        <StatusBadge status={order.status} />
                      </ListGroup.Item>
                    ))}
                  </ListGroup>
                  {ordersQuery.hasNextPage && (
                    <div className="d-grid mt-2">
                      <Button
                        type="button"
                        variant="outline-primary"
                        disabled={ordersQuery.isFetchingNextPage}
                        onClick={() => { void ordersQuery.fetchNextPage(); }}
                      >
                        {ordersQuery.isFetchingNextPage
                          ? "주문 불러오는 중..."
                          : "비회원 주문 더 보기"}
                      </Button>
                    </div>
                  )}
                </Col>
              )}

              {bookings.length > 0 && (
                <Col xs={12} lg={6}>
                  <h6>비회원 예약 · 불러온 {bookings.length}건</h6>
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
                            예약 #{booking.bookingId} · {formatDateTime(booking.startAt)}
                          </small>
                        </span>
                        <StatusBadge status={booking.status} />
                      </ListGroup.Item>
                    ))}
                  </ListGroup>
                  {bookingsQuery.hasNextPage && (
                    <div className="d-grid mt-2">
                      <Button
                        type="button"
                        variant="outline-primary"
                        disabled={bookingsQuery.isFetchingNextPage}
                        onClick={() => { void bookingsQuery.fetchNextPage(); }}
                      >
                        {bookingsQuery.isFetchingNextPage
                          ? "예약 불러오는 중..."
                          : "비회원 예약 더 보기"}
                      </Button>
                    </div>
                  )}
                </Col>
              )}
            </Row>
          </div>
        )}
      </Card.Body>
    </Card>
  );
}
