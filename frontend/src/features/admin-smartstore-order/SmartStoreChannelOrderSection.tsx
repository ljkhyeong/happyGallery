import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Badge, Button, Col, Form, Modal, Row, Table } from "react-bootstrap";
import type {
  DelaySmartStoreOrderRequest,
  DispatchSmartStoreExchangeRequest,
  DispatchSmartStoreOrderRequest,
  SmartStoreChannelOrderResponse,
  SmartStoreChannelOrderResponseAttentionReason,
  HoldSmartStoreExchangeRequest,
  RequestSmartStoreSellerCancelRequest,
} from "@/generated/api/adminOrder";
import { ApiError } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import {
  approveSmartStoreCancel,
  approveSmartStoreReturn,
  confirmSmartStoreOrder,
  delaySmartStoreOrder,
  dispatchSmartStoreExchange,
  dispatchSmartStoreOrder,
  completeSmartStoreExchangeCollection,
  holdSmartStoreExchange,
  releaseSmartStoreExchange,
  rejectSmartStoreExchange,
  requestSmartStoreOrderCancel,
  fetchSmartStoreChannelOrder,
  fetchSmartStoreChannelOrders,
  rejectSmartStoreReturn,
  resolveSmartStoreReturn,
  retrySmartStoreOrderInventory,
} from "./api";

interface Props {
  adminKey: string;
  onAuthError: () => void;
  attentionOnly?: boolean;
}

const ATTENTION_LABELS: Record<
  Exclude<SmartStoreChannelOrderResponseAttentionReason, null>,
  string
> = {
  MAPPING_REQUIRED: "상품·옵션 매핑 필요",
  STOCK_SHORTAGE: "내부 재고 부족",
  RETURN_REVIEW: "반품 검수 필요",
  STATUS_REVIEW: "새 주문 상태 확인 필요",
};

const STATUS_LABELS: Record<string, string> = {
  PAYMENT_WAITING: "결제 대기",
  PAYED: "결제 완료",
  DELIVERING: "배송 중",
  DELIVERED: "배송 완료",
  PURCHASE_DECIDED: "구매 확정",
  EXCHANGED: "교환 완료",
  CANCELED: "취소",
  RETURNED: "반품 완료",
  CANCELED_BY_NOPAYMENT: "미결제 취소",
};

export function SmartStoreChannelOrderSection({
  adminKey,
  onAuthError,
  attentionOnly: initialAttentionOnly = false,
}: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [attentionOnly, setAttentionOnly] = useState(initialAttentionOnly);
  const [pendingId, setPendingId] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const queryKey = ["admin", "smartstore-orders", attentionOnly] as const;
  const { data, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey,
    queryFn: () => fetchSmartStoreChannelOrders(adminKey, attentionOnly),
    refetchInterval: 30_000,
  });

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ["admin", "smartstore-orders"] });
  }

  const retryInventory = useAdminMutation(onAuthError, {
    mutationFn: (productOrderId: string) =>
      retrySmartStoreOrderInventory(adminKey, productOrderId),
    onMutate: setPendingId,
    onSuccess: (order) => {
      toast.show(order.attentionReason
        ? "재고를 반영하지 못했습니다. 확인 사유를 검토해 주세요."
        : "스마트스토어 주문 재고를 반영했습니다.");
      invalidate();
    },
    onSettled: () => setPendingId(null),
  });

  const resolveReturn = useAdminMutation(onAuthError, {
    mutationFn: ({ productOrderId, restoreStock }: {
      productOrderId: string;
      restoreStock: boolean;
    }) => resolveSmartStoreReturn(adminKey, productOrderId, restoreStock),
    onMutate: ({ productOrderId }) => setPendingId(productOrderId),
    onSuccess: (_, variables) => {
      toast.show(variables.restoreStock
        ? "반품 검수 수량을 내부 재고에 복원했습니다."
        : "판매 불가 반품으로 확인하고 재고를 복원하지 않았습니다.");
      invalidate();
    },
    onSettled: () => setPendingId(null),
  });

  if (isLoading) return <LoadingSpinner />;
  if (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    return <ErrorAlert error={error} />;
  }

  return (
    <>
      {!initialAttentionOnly && (
        <Form.Check
          className="mb-3"
          type="switch"
          id="smartstore-order-attention-only"
          label="확인이 필요한 주문만 보기"
          checked={attentionOnly}
          onChange={(event) => setAttentionOnly(event.target.checked)}
        />
      )}
      {!data?.length ? (
        <EmptyState message={attentionOnly
          ? "확인이 필요한 스마트스토어 주문이 없습니다."
          : "수집된 스마트스토어 주문이 없습니다."} />
      ) : (
        <Table responsive hover size="sm" className="align-middle">
          <thead>
            <tr>
              <th>상품 주문 번호</th>
              <th>상품·옵션</th>
              <th>주문 상태</th>
              <th>수량</th>
              <th>내부 재고 반영</th>
              <th>확인 사유</th>
              <th>변경일</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {data.map((order) => (
              <tr key={order.productOrderId}>
                <td className="small">{order.productOrderId}</td>
                <td>
                  <div>{order.productName}</div>
                  {order.productOption && (
                    <div className="small text-muted-soft">{order.productOption}</div>
                  )}
                </td>
                <td>{STATUS_LABELS[order.productOrderStatus] ?? order.productOrderStatus}</td>
                <td>{order.initialQuantity}개 / 잔여 {order.remainQuantity}개</td>
                <td>{order.inventoryAppliedQuantity}개 차감</td>
                <td>
                  {order.attentionReason ? (
                    <Badge bg="warning" text="dark">
                      {ATTENTION_LABELS[order.attentionReason]}
                    </Badge>
                  ) : (
                    <Badge bg="success">정상</Badge>
                  )}
                </td>
                <td className="small">{formatDateTime(order.lastChangedAt)}</td>
                <td>{actions(
                  order,
                  pendingId,
                  retryInventory.mutate,
                  resolveReturn.mutate,
                  setSelectedId,
                )}</td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
      <SmartStoreOrderDetailModal
        adminKey={adminKey}
        productOrderId={selectedId}
        onAuthError={onAuthError}
        onClose={() => setSelectedId(null)}
        onChanged={invalidate}
      />
    </>
  );
}

function actions(
  order: SmartStoreChannelOrderResponse,
  pendingId: string | null,
  retry: (productOrderId: string) => void,
  resolveReturn: (variables: { productOrderId: string; restoreStock: boolean }) => void,
  openDetail: (productOrderId: string) => void,
) {
  const disabled = pendingId === order.productOrderId;
  if (order.attentionReason === "MAPPING_REQUIRED"
      || order.attentionReason === "STOCK_SHORTAGE") {
    return <div className="d-flex flex-wrap gap-1">
      <Button size="sm" variant="outline-secondary" onClick={() => openDetail(order.productOrderId)}>
        주문 처리
      </Button>
      <Button
          size="sm"
          variant="outline-primary"
          disabled={disabled}
          onClick={() => retry(order.productOrderId)}
        >
          재고 반영 다시 시도
        </Button>
    </div>;
  }
  if (order.attentionReason !== "RETURN_REVIEW") {
    return <Button
      size="sm"
      variant="outline-secondary"
      onClick={() => openDetail(order.productOrderId)}
    >
      주문 처리
    </Button>;
  }
  return (
    <div className="d-flex flex-wrap gap-1">
      <Button
        size="sm"
        variant="outline-secondary"
        onClick={() => openDetail(order.productOrderId)}
      >
        주문 처리
      </Button>
      <Button
        size="sm"
        variant="outline-success"
        disabled={disabled}
        onClick={() => resolveReturn({ productOrderId: order.productOrderId, restoreStock: true })}
      >
        검수 후 재고 복원
      </Button>
      <Button
        size="sm"
        variant="outline-secondary"
        disabled={disabled}
        onClick={() => resolveReturn({ productOrderId: order.productOrderId, restoreStock: false })}
      >
        복원 없이 종료
      </Button>
    </div>
  );
}

type ChannelAction =
  | { kind: "confirm" }
  | { kind: "dispatch"; request: DispatchSmartStoreOrderRequest }
  | { kind: "delay"; request: DelaySmartStoreOrderRequest }
  | { kind: "approveCancel" }
  | { kind: "approveReturn" }
  | { kind: "rejectReturn" }
  | { kind: "dispatchExchange"; request: DispatchSmartStoreExchangeRequest }
  | { kind: "completeExchangeCollect" }
  | { kind: "rejectExchange"; reason: string }
  | { kind: "holdExchange"; request: HoldSmartStoreExchangeRequest }
  | { kind: "releaseExchangeHold" }
  | { kind: "requestSellerCancel"; request: RequestSmartStoreSellerCancelRequest };

function SmartStoreOrderDetailModal({
  adminKey,
  productOrderId,
  onAuthError,
  onClose,
  onChanged,
}: {
  adminKey: string;
  productOrderId: string | null;
  onAuthError: () => void;
  onClose: () => void;
  onChanged: () => void;
}) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [deliveryMethod, setDeliveryMethod] = useState("DELIVERY");
  const [deliveryCompanyCode, setDeliveryCompanyCode] = useState("");
  const [trackingNumber, setTrackingNumber] = useState("");
  const [dispatchDate, setDispatchDate] = useState("");
  const [dispatchDueDate, setDispatchDueDate] = useState("");
  const [delayReasonCode, setDelayReasonCode] = useState("PRODUCT_PREPARE");
  const [delayDetail, setDelayDetail] = useState("");
  const [exchangeRejectReason, setExchangeRejectReason] = useState("");
  const [holdbackClassType, setHoldbackClassType] = useState("EXCHANGE_DELIVERYFEE");
  const [holdbackReason, setHoldbackReason] = useState("");
  const [extraExchangeFeeAmount, setExtraExchangeFeeAmount] = useState("");
  const [cancelReason, setCancelReason] = useState("SOLD_OUT");
  const [cancelDetailedReason, setCancelDetailedReason] = useState("");
  const [cancelQuantity, setCancelQuantity] = useState("");

  const detailQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "smartstore-orders", "detail", productOrderId],
    queryFn: () => fetchSmartStoreChannelOrder(adminKey, productOrderId!),
    enabled: productOrderId !== null,
  });

  const action = useAdminMutation(onAuthError, {
    mutationFn: async (request: ChannelAction) => {
      if (!productOrderId) return;
      if (request.kind === "confirm") {
        await confirmSmartStoreOrder(adminKey, productOrderId);
      } else if (request.kind === "dispatch") {
        await dispatchSmartStoreOrder(adminKey, productOrderId, request.request);
      } else if (request.kind === "delay") {
        await delaySmartStoreOrder(adminKey, productOrderId, request.request);
      } else if (request.kind === "approveCancel") {
        await approveSmartStoreCancel(adminKey, productOrderId);
      } else if (request.kind === "approveReturn") {
        await approveSmartStoreReturn(adminKey, productOrderId);
      } else if (request.kind === "rejectReturn") {
        await rejectSmartStoreReturn(adminKey, productOrderId);
      } else if (request.kind === "dispatchExchange") {
        await dispatchSmartStoreExchange(adminKey, productOrderId, request.request);
      } else if (request.kind === "completeExchangeCollect") {
        await completeSmartStoreExchangeCollection(adminKey, productOrderId);
      } else if (request.kind === "rejectExchange") {
        await rejectSmartStoreExchange(adminKey, productOrderId, request.reason);
      } else if (request.kind === "holdExchange") {
        await holdSmartStoreExchange(adminKey, productOrderId, request.request);
      } else if (request.kind === "releaseExchangeHold") {
        await releaseSmartStoreExchange(adminKey, productOrderId);
      } else {
        await requestSmartStoreOrderCancel(adminKey, productOrderId, request.request);
      }
    },
    onSuccess: async () => {
      toast.show("스마트스토어에 주문 처리를 요청했습니다. 변경 상태는 주문 동기화 후 반영됩니다.");
      onChanged();
      await queryClient.invalidateQueries({
        queryKey: ["admin", "smartstore-orders", "detail", productOrderId],
      });
    },
  });

  const detail = detailQuery.data;
  const order = detail?.order;
  const dispatchRequest = {
    deliveryMethod,
    deliveryCompanyCode: deliveryCompanyCode || undefined,
    trackingNumber: trackingNumber || undefined,
    dispatchDate,
  } satisfies DispatchSmartStoreOrderRequest;
  const exchangeRequest = {
    deliveryMethod,
    deliveryCompanyCode,
    trackingNumber,
  } satisfies DispatchSmartStoreExchangeRequest;
  const canPlaceOrder = order?.productOrderStatus === "PAYED";
  const claimStatus = order?.claimStatus;

  return (
    <Modal show={productOrderId !== null} onHide={onClose} size="lg" centered>
      <Modal.Header closeButton>
        <Modal.Title>스마트스토어 주문 처리</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {detailQuery.isLoading && <LoadingSpinner />}
        <ErrorAlert error={detailQuery.error ?? action.error} />
        {detail && order && (
          <>
            <div className="mb-3">
              <div className="fw-semibold">{order.productName}</div>
              <div className="small text-muted-soft">
                상품 주문 {order.productOrderId} · {STATUS_LABELS[order.productOrderStatus] ?? order.productOrderStatus}
                {order.productOption ? ` · ${order.productOption}` : ""}
              </div>
            </div>

            <Row className="g-3 mb-3 small">
              <Col md={6}>
                <div className="fw-semibold mb-1">수령인·배송지</div>
                {detail.deliveryInfo ? (
                  <div>
                    <div>{detail.deliveryInfo.recipientName} · {detail.deliveryInfo.phone}</div>
                    <div>[{detail.deliveryInfo.postalCode}] {detail.deliveryInfo.addressLine1} {detail.deliveryInfo.addressLine2}</div>
                    {detail.deliveryInfo.shippingMemo && <div>배송 메모: {detail.deliveryInfo.shippingMemo}</div>}
                  </div>
                ) : <div className="text-muted-soft">배송지 정보 없음</div>}
              </Col>
              <Col md={6}>
                <div className="fw-semibold mb-1">결제·정산</div>
                <div>결제 금액: {amount(detail.paymentAmount)}</div>
                <div>정산 예정: {amount(detail.expectedSettlementAmount)}</div>
                <div>결제 수수료: {amount(detail.paymentCommission)} · 판매 수수료: {amount(detail.saleCommission)}</div>
              </Col>
              <Col md={6}>
                <div className="fw-semibold mb-1">발주·배송</div>
                <div>발주 상태: {detail.placeOrderStatus ?? "-"}</div>
                <div>발송 기한: {detail.shippingDueDate ? formatDateTime(detail.shippingDueDate) : "-"}</div>
                <div>택배사·운송장: {detail.deliveryCompany ?? "-"} {detail.trackingNumber ?? ""}</div>
              </Col>
              <Col md={6}>
                <div className="fw-semibold mb-1">클레임</div>
                <div>{order.claimType ?? "없음"} · {claimStatus ?? "-"}</div>
                {detail.claimDetail && <div className="mt-1">
                  <div>사유: {detail.claimDetail.reason ?? "-"}</div>
                  {detail.claimDetail.detailedReason && <div>{detail.claimDetail.detailedReason}</div>}
                  <div>요청 수량: {detail.claimDetail.requestQuantity ?? "-"}개</div>
                  <div>요청일: {detail.claimDetail.requestedAt
                    ? formatDateTime(detail.claimDetail.requestedAt) : "-"}</div>
                  {detail.claimDetail.collectStatus && <div>
                    수거: {detail.claimDetail.collectStatus}
                    {detail.claimDetail.collectTrackingNumber
                      ? ` · ${detail.claimDetail.collectDeliveryCompany ?? ""} ${detail.claimDetail.collectTrackingNumber}` : ""}
                  </div>}
                  {detail.claimDetail.claimDeliveryFeeDemandAmount !== null && <div>
                    클레임 배송비: {formatKRW(detail.claimDetail.claimDeliveryFeeDemandAmount)}
                  </div>}
                  {detail.claimDetail.holdbackStatus && <div>보류 상태: {detail.claimDetail.holdbackStatus}</div>}
                </div>}
              </Col>
            </Row>

            {canPlaceOrder && detail.placeOrderStatus === "NOT_YET" && (
              <Button
                className="mb-3"
                disabled={action.isPending}
                onClick={() => action.mutate({ kind: "confirm" })}
              >
                발주 확인
              </Button>
            )}

            {canPlaceOrder && detail.placeOrderStatus === "OK" && (
              <Row className="g-3 mb-3">
                <Col md={6}>
                  <Form
                    onSubmit={(event) => {
                      event.preventDefault();
                      action.mutate({ kind: "dispatch", request: dispatchRequest });
                    }}
                  >
                    <div className="fw-semibold mb-2">발송 처리</div>
                    <DeliveryFields
                      deliveryMethod={deliveryMethod}
                      deliveryCompanyCode={deliveryCompanyCode}
                      trackingNumber={trackingNumber}
                      onDeliveryMethod={setDeliveryMethod}
                      onDeliveryCompanyCode={setDeliveryCompanyCode}
                      onTrackingNumber={setTrackingNumber}
                    />
                    <Form.Control
                      className="mb-2"
                      type="datetime-local"
                      required
                      value={dispatchDate}
                      onChange={(event) => setDispatchDate(event.target.value)}
                      aria-label="발송일시"
                    />
                    <Button type="submit" size="sm" disabled={action.isPending}>발송 처리</Button>
                  </Form>
                </Col>
                <Col md={6}>
                  <Form
                    onSubmit={(event) => {
                      event.preventDefault();
                      action.mutate({
                        kind: "delay",
                        request: {
                          dispatchDueDate,
                          reasonCode: delayReasonCode,
                          detailedReason: delayDetail,
                        },
                      });
                    }}
                  >
                    <div className="fw-semibold mb-2">발송 지연</div>
                    <Form.Control
                      className="mb-2"
                      type="datetime-local"
                      required
                      value={dispatchDueDate}
                      onChange={(event) => setDispatchDueDate(event.target.value)}
                      aria-label="변경 발송 기한"
                    />
                    <Form.Select
                      className="mb-2"
                      value={delayReasonCode}
                      onChange={(event) => setDelayReasonCode(event.target.value)}
                      aria-label="발송 지연 사유"
                    >
                      <option value="PRODUCT_PREPARE">상품 준비</option>
                      <option value="CUSTOMER_REQUEST">고객 요청</option>
                      <option value="CUSTOM_BUILD">주문 제작</option>
                      <option value="RESERVED_DISPATCH">예약 발송</option>
                      <option value="OVERSEA_DELIVERY">해외 배송</option>
                      <option value="ETC">기타</option>
                    </Form.Select>
                    <Form.Control
                      className="mb-2"
                      as="textarea"
                      rows={2}
                      maxLength={4000}
                      value={delayDetail}
                      onChange={(event) => setDelayDetail(event.target.value)}
                      placeholder="상세 사유"
                    />
                    <Button type="submit" size="sm" variant="outline-warning" disabled={action.isPending}>
                      발송 기한 변경
                    </Button>
                  </Form>
                </Col>
              </Row>
            )}

            <div className="d-flex flex-wrap gap-2 mb-3">
              {claimStatus === "CANCEL_REQUEST" && (
                <Button variant="danger" size="sm" disabled={action.isPending}
                  onClick={() => action.mutate({ kind: "approveCancel" })}>
                  취소 승인
                </Button>
              )}
              {order.claimType === "RETURN" && ["RETURN_REQUEST", "COLLECT_DONE"].includes(claimStatus ?? "") && (
                <Button variant="danger" size="sm" disabled={action.isPending}
                  onClick={() => action.mutate({ kind: "approveReturn" })}>
                  반품 승인
                </Button>
              )}
              {order.claimType === "RETURN" && ["RETURN_REQUEST", "COLLECTING"].includes(claimStatus ?? "") && (
                <Button variant="outline-secondary" size="sm" disabled={action.isPending}
                  onClick={() => action.mutate({ kind: "rejectReturn" })}>
                  반품 거부
                </Button>
              )}
            </div>

            {!order.claimType && ["PAYED", "DELIVERING"].includes(order.productOrderStatus) && (
              <Form className="border rounded p-3 mb-3" onSubmit={(event) => {
                event.preventDefault();
                action.mutate({
                  kind: "requestSellerCancel",
                  request: {
                    reason: cancelReason,
                    detailedReason: cancelDetailedReason || undefined,
                    quantity: cancelQuantity ? Number(cancelQuantity) : undefined,
                  },
                });
              }}>
                <div className="fw-semibold mb-2">판매자 취소 요청</div>
                <Row className="g-2">
                  <Col md={4}><Form.Select value={cancelReason}
                    onChange={(event) => setCancelReason(event.target.value)}>
                    <option value="SOLD_OUT">품절</option>
                    <option value="DELAYED_DELIVERY">배송 지연</option>
                    <option value="INCORRECT_INFO">상품 정보 오류</option>
                    <option value="PRODUCT_UNSATISFIED">상품 문제</option>
                  </Form.Select></Col>
                  <Col md={2}><Form.Control type="number" min={1} max={order.remainQuantity}
                    value={cancelQuantity} onChange={(event) => setCancelQuantity(event.target.value)}
                    placeholder="수량(전체)" /></Col>
                  <Col md={6}><Form.Control maxLength={500} value={cancelDetailedReason}
                    onChange={(event) => setCancelDetailedReason(event.target.value)}
                    placeholder="상세 사유" /></Col>
                </Row>
                <Button className="mt-2" type="submit" size="sm" variant="outline-danger"
                  disabled={action.isPending}>취소 요청</Button>
              </Form>
            )}

            {order.claimType === "EXCHANGE" && (
              <div className="border rounded p-3 mb-3">
                <div className="fw-semibold mb-2">교환 클레임 처리</div>
                <div className="d-flex flex-wrap gap-2 mb-3">
                  <Button size="sm" variant="outline-success" disabled={action.isPending}
                    onClick={() => action.mutate({ kind: "completeExchangeCollect" })}>
                    수거 완료
                  </Button>
                  {detail.claimDetail?.holdbackStatus && <Button size="sm" variant="outline-primary"
                    disabled={action.isPending}
                    onClick={() => action.mutate({ kind: "releaseExchangeHold" })}>
                    보류 해제
                  </Button>}
                </div>
                <Form className="mb-3" onSubmit={(event) => {
                  event.preventDefault();
                  action.mutate({ kind: "rejectExchange", reason: exchangeRejectReason });
                }}>
                  <Form.Control required maxLength={500} value={exchangeRejectReason}
                    onChange={(event) => setExchangeRejectReason(event.target.value)}
                    placeholder="교환 거절 사유" />
                  <Button className="mt-2" type="submit" size="sm" variant="outline-danger"
                    disabled={action.isPending || !exchangeRejectReason.trim()}>교환 거절</Button>
                </Form>
                <Form onSubmit={(event) => {
                  event.preventDefault();
                  action.mutate({
                    kind: "holdExchange",
                    request: {
                      holdbackClassType,
                      detailedReason: holdbackReason,
                      extraExchangeFeeAmount: extraExchangeFeeAmount
                        ? Number(extraExchangeFeeAmount) : undefined,
                    },
                  });
                }}>
                  <Row className="g-2">
                    <Col md={4}><Form.Select value={holdbackClassType}
                      onChange={(event) => setHoldbackClassType(event.target.value)}>
                      <option value="EXCHANGE_DELIVERYFEE">교환 배송비</option>
                      <option value="EXCHANGE_EXTRAFEE">추가 비용</option>
                      <option value="EXCHANGE_PRODUCT_READY">교환 상품 준비</option>
                      <option value="PURCHASER_CONFIRM_NEED">구매자 확인 필요</option>
                      <option value="SELLER_CONFIRM_NEED">판매자 확인 필요</option>
                      <option value="ETC">기타</option>
                    </Form.Select></Col>
                    <Col md={5}><Form.Control required maxLength={500} value={holdbackReason}
                      onChange={(event) => setHoldbackReason(event.target.value)}
                      placeholder="보류 상세 사유" /></Col>
                    <Col md={3}><Form.Control type="number" min={0} value={extraExchangeFeeAmount}
                      onChange={(event) => setExtraExchangeFeeAmount(event.target.value)}
                      placeholder="추가 비용" /></Col>
                  </Row>
                  <Button className="mt-2" type="submit" size="sm" variant="outline-warning"
                    disabled={action.isPending || !holdbackReason.trim()}>교환 보류</Button>
                </Form>
              </div>
            )}

            {order.claimType === "EXCHANGE" && claimStatus === "COLLECT_DONE" && (
              <Form
                onSubmit={(event) => {
                  event.preventDefault();
                  action.mutate({ kind: "dispatchExchange", request: exchangeRequest });
                }}
              >
                <div className="fw-semibold mb-2">교환품 재배송</div>
                <DeliveryFields
                  required
                  deliveryMethod={deliveryMethod}
                  deliveryCompanyCode={deliveryCompanyCode}
                  trackingNumber={trackingNumber}
                  onDeliveryMethod={setDeliveryMethod}
                  onDeliveryCompanyCode={setDeliveryCompanyCode}
                  onTrackingNumber={setTrackingNumber}
                />
                <Button type="submit" size="sm" disabled={action.isPending}>교환품 발송</Button>
              </Form>
            )}
          </>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onClose}>닫기</Button>
      </Modal.Footer>
    </Modal>
  );
}

function DeliveryFields({
  required = false,
  deliveryMethod,
  deliveryCompanyCode,
  trackingNumber,
  onDeliveryMethod,
  onDeliveryCompanyCode,
  onTrackingNumber,
}: {
  required?: boolean;
  deliveryMethod: string;
  deliveryCompanyCode: string;
  trackingNumber: string;
  onDeliveryMethod: (value: string) => void;
  onDeliveryCompanyCode: (value: string) => void;
  onTrackingNumber: (value: string) => void;
}) {
  return <>
    <Form.Select
      className="mb-2"
      value={deliveryMethod}
      onChange={(event) => onDeliveryMethod(event.target.value)}
      aria-label="배송 수단"
    >
      <option value="DELIVERY">택배</option>
      <option value="DIRECT_DELIVERY">직접 배송</option>
      <option value="VISIT_RECEIPT">방문 수령</option>
      <option value="QUICK_SVC">퀵서비스</option>
    </Form.Select>
    <Form.Control
      className="mb-2"
      required={required}
      value={deliveryCompanyCode}
      maxLength={40}
      onChange={(event) => onDeliveryCompanyCode(event.target.value.toUpperCase())}
      placeholder="택배사 코드 (예: CJGLS)"
    />
    <Form.Control
      className="mb-2"
      required={required}
      value={trackingNumber}
      maxLength={100}
      onChange={(event) => onTrackingNumber(event.target.value)}
      placeholder="운송장 번호"
    />
  </>;
}

function amount(value: number | null): string {
  return value === null ? "-" : formatKRW(value);
}
