import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Button, Col, Form, Modal, Row } from "react-bootstrap";
import type {
  DelaySmartStoreOrderRequest,
  DispatchSmartStoreExchangeRequest,
  DispatchSmartStoreOrderRequest,
  HoldSmartStoreExchangeRequest,
  HoldSmartStoreReturnRequest,
  RequestSmartStoreSellerCancelRequest,
  RequestSmartStoreSellerReturnRequest,
} from "@/generated/api/adminOrder";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import {
  approveSmartStoreCancel,
  approveSmartStoreReturn,
  confirmSmartStoreOrder,
  delaySmartStoreOrder,
  dispatchSmartStoreExchange,
  dispatchSmartStoreOrder,
  completeSmartStoreExchangeCollection,
  holdSmartStoreExchange,
  holdSmartStoreReturn,
  releaseSmartStoreExchange,
  releaseSmartStoreReturn,
  rejectSmartStoreExchange,
  requestSmartStoreOrderCancel,
  requestSmartStoreOrderReturn,
  fetchSmartStoreChannelOrderActions,
  fetchSmartStoreChannelOrder,
  rejectSmartStoreReturn,
} from "./api";
import { DeliveryCompanyDatalist, DeliveryFields } from "./DeliveryFields";
import { STATUS_LABELS } from "./labels";
import { SmartStoreOrderActionHistory } from "./SmartStoreOrderActionHistory";

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
  | { kind: "holdReturn"; request: HoldSmartStoreReturnRequest }
  | { kind: "releaseReturnHold" }
  | { kind: "requestSellerReturn"; request: RequestSmartStoreSellerReturnRequest }
  | { kind: "requestSellerCancel"; request: RequestSmartStoreSellerCancelRequest };

export function SmartStoreOrderDetailModal({
  adminKey,
  productOrderId,
  onAuthError,
  onClose,
  onChanged,
}: {
  adminKey: string;
  productOrderId: string;
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
  const [returnHoldbackClassType, setReturnHoldbackClassType] = useState("RETURN_DELIVERYFEE");
  const [returnHoldbackReason, setReturnHoldbackReason] = useState("");
  const [extraReturnFeeAmount, setExtraReturnFeeAmount] = useState("");
  const [sellerReturnReason, setSellerReturnReason] = useState("PRODUCT_UNSATISFIED");
  const [collectDeliveryMethod, setCollectDeliveryMethod] = useState("RETURN_DESIGNATED");
  const [collectDeliveryCompany, setCollectDeliveryCompany] = useState("");
  const [collectTrackingNumber, setCollectTrackingNumber] = useState("");
  const [returnQuantity, setReturnQuantity] = useState("");

  const detailQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "smartstore-orders", "detail", productOrderId],
    queryFn: () => fetchSmartStoreChannelOrder(adminKey, productOrderId),
  });
  const historyQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "smartstore-orders", "actions", productOrderId],
    queryFn: () => fetchSmartStoreChannelOrderActions(adminKey, productOrderId),
  });

  const action = useAdminMutation(onAuthError, {
    mutationFn: async (request: ChannelAction) => {
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
      } else if (request.kind === "holdReturn") {
        await holdSmartStoreReturn(adminKey, productOrderId, request.request);
      } else if (request.kind === "releaseReturnHold") {
        await releaseSmartStoreReturn(adminKey, productOrderId);
      } else if (request.kind === "requestSellerReturn") {
        await requestSmartStoreOrderReturn(adminKey, productOrderId, request.request);
      } else if (request.kind === "requestSellerCancel") {
        await requestSmartStoreOrderCancel(adminKey, productOrderId, request.request);
      }
    },
    onSuccess: async () => {
      toast.show("스마트스토어에 주문 처리를 요청했습니다. 주문 상태는 다음 자동 조회 후 갱신됩니다.");
      onChanged();
      await queryClient.invalidateQueries({
        queryKey: ["admin", "smartstore-orders", "detail", productOrderId],
      });
      await queryClient.invalidateQueries({
        queryKey: ["admin", "smartstore-orders", "actions", productOrderId],
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
  const canPlaceOrder = order?.productOrderStatus === "PAYED";
  const claimStatus = order?.claimStatus;

  return (
    <Modal show onHide={() => { if (!action.isPending) onClose(); }} size="lg" centered>
      <Modal.Header closeButton={!action.isPending}>
        <Modal.Title>스마트스토어 주문 처리</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {detailQuery.isLoading && <LoadingSpinner />}
        <ErrorAlert error={detailQuery.error ?? historyQuery.error ?? action.error} />
        {detail && order && (
          <fieldset disabled={action.isPending}>
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
                  {!!detail.claimDetail.imageUrls.length && <div className="d-flex flex-wrap gap-2 mt-2">
                    {detail.claimDetail.imageUrls.map((imageUrl) => <a key={imageUrl}
                      href={imageUrl} target="_blank" rel="noreferrer">
                      <img src={imageUrl} alt="클레임 첨부" width={72} height={72}
                        className="rounded border object-fit-cover" />
                    </a>)}
                  </div>}
                </div>}
              </Col>
            </Row>

            <SmartStoreOrderActionHistory
              history={historyQuery.data ?? []}
              loading={historyQuery.isLoading}
            />

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

            {order.claimType === "RETURN" && (
              <ReturnHoldPanel
                held={!!detail.claimDetail?.holdbackStatus}
                pending={action.isPending}
                holdbackClassType={returnHoldbackClassType}
                detailedReason={returnHoldbackReason}
                extraReturnFeeAmount={extraReturnFeeAmount}
                onHoldbackClassType={setReturnHoldbackClassType}
                onDetailedReason={setReturnHoldbackReason}
                onExtraReturnFeeAmount={setExtraReturnFeeAmount}
                onAction={action.mutate}
              />
            )}

            {!order.claimType && ["PAYED", "DELIVERING"].includes(order.productOrderStatus) && (
              <SellerCancelRequestForm
                pending={action.isPending}
                remainQuantity={order.remainQuantity}
                reason={cancelReason}
                detailedReason={cancelDetailedReason}
                quantity={cancelQuantity}
                onReason={setCancelReason}
                onDetailedReason={setCancelDetailedReason}
                onQuantity={setCancelQuantity}
                onAction={action.mutate}
              />
            )}

            {!order.claimType && (
              <SellerReturnRequestForm
                pending={action.isPending}
                remainQuantity={order.remainQuantity}
                returnReason={sellerReturnReason}
                collectDeliveryMethod={collectDeliveryMethod}
                collectDeliveryCompany={collectDeliveryCompany}
                collectTrackingNumber={collectTrackingNumber}
                returnQuantity={returnQuantity}
                onReturnReason={setSellerReturnReason}
                onCollectDeliveryMethod={setCollectDeliveryMethod}
                onCollectDeliveryCompany={setCollectDeliveryCompany}
                onCollectTrackingNumber={setCollectTrackingNumber}
                onReturnQuantity={setReturnQuantity}
                onAction={action.mutate}
              />
            )}

            {order.claimType === "EXCHANGE" && (
              <ExchangeClaimPanel
                held={!!detail.claimDetail?.holdbackStatus}
                pending={action.isPending}
                rejectReason={exchangeRejectReason}
                holdbackClassType={holdbackClassType}
                detailedReason={holdbackReason}
                extraExchangeFeeAmount={extraExchangeFeeAmount}
                onRejectReason={setExchangeRejectReason}
                onHoldbackClassType={setHoldbackClassType}
                onDetailedReason={setHoldbackReason}
                onExtraExchangeFeeAmount={setExtraExchangeFeeAmount}
                onAction={action.mutate}
              />
            )}

            {order.claimType === "EXCHANGE" && claimStatus === "COLLECT_DONE" && (
              <ExchangeDispatchForm
                pending={action.isPending}
                deliveryMethod={deliveryMethod}
                deliveryCompanyCode={deliveryCompanyCode}
                trackingNumber={trackingNumber}
                onDeliveryMethod={setDeliveryMethod}
                onDeliveryCompanyCode={setDeliveryCompanyCode}
                onTrackingNumber={setTrackingNumber}
                onAction={action.mutate}
              />
            )}
          </fieldset>
        )}
        <DeliveryCompanyDatalist />
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" disabled={action.isPending} onClick={onClose}>닫기</Button>
      </Modal.Footer>
    </Modal>
  );
}

function ReturnHoldPanel({
  held,
  pending,
  holdbackClassType,
  detailedReason,
  extraReturnFeeAmount,
  onHoldbackClassType,
  onDetailedReason,
  onExtraReturnFeeAmount,
  onAction,
}: {
  held: boolean;
  pending: boolean;
  holdbackClassType: string;
  detailedReason: string;
  extraReturnFeeAmount: string;
  onHoldbackClassType: (value: string) => void;
  onDetailedReason: (value: string) => void;
  onExtraReturnFeeAmount: (value: string) => void;
  onAction: (request: ChannelAction) => void;
}) {
  return (
    <div className="border rounded p-3 mb-3">
      <div className="d-flex justify-content-between align-items-center mb-2">
        <div className="fw-semibold">반품 보류</div>
        {held && (
          <Button type="button" size="sm" variant="outline-primary"
            disabled={pending}
            onClick={() => onAction({ kind: "releaseReturnHold" })}>
            보류 해제
          </Button>
        )}
      </div>
      <Form onSubmit={(event) => {
        event.preventDefault();
        onAction({
          kind: "holdReturn",
          request: {
            holdbackClassType,
            detailedReason,
            extraReturnFeeAmount: extraReturnFeeAmount
              ? Number(extraReturnFeeAmount) : undefined,
          },
        });
      }}>
        <Row className="g-2">
          <Col md={4}><Form.Select value={holdbackClassType}
            onChange={(event) => onHoldbackClassType(event.target.value)}>
            <option value="RETURN_DELIVERYFEE">반품 배송비</option>
            <option value="RETURN_EXTRAFEE">추가 비용</option>
            <option value="PURCHASER_CONFIRM_NEED">구매자 확인 필요</option>
            <option value="SELLER_CONFIRM_NEED">판매자 확인 필요</option>
            <option value="ETC">기타</option>
          </Form.Select></Col>
          <Col md={5}><Form.Control required value={detailedReason}
            onChange={(event) => onDetailedReason(event.target.value)}
            placeholder="보류 상세 사유" /></Col>
          <Col md={3}><Form.Control type="number" min={0} value={extraReturnFeeAmount}
            onChange={(event) => onExtraReturnFeeAmount(event.target.value)}
            placeholder="추가 비용" /></Col>
        </Row>
        <Button className="mt-2" type="submit" size="sm" variant="outline-warning"
          disabled={pending || !detailedReason.trim()}>반품 보류</Button>
      </Form>
    </div>
  );
}

function SellerCancelRequestForm({
  pending,
  remainQuantity,
  reason,
  detailedReason,
  quantity,
  onReason,
  onDetailedReason,
  onQuantity,
  onAction,
}: {
  pending: boolean;
  remainQuantity: number;
  reason: string;
  detailedReason: string;
  quantity: string;
  onReason: (value: string) => void;
  onDetailedReason: (value: string) => void;
  onQuantity: (value: string) => void;
  onAction: (request: ChannelAction) => void;
}) {
  return (
    <Form className="border rounded p-3 mb-3" onSubmit={(event) => {
      event.preventDefault();
      onAction({
        kind: "requestSellerCancel",
        request: {
          reason,
          detailedReason: detailedReason || undefined,
          quantity: quantity ? Number(quantity) : undefined,
        },
      });
    }}>
      <div className="fw-semibold mb-2">판매자 취소 요청</div>
      <Row className="g-2">
        <Col md={4}><Form.Select value={reason}
          onChange={(event) => onReason(event.target.value)}>
          <option value="SOLD_OUT">품절</option>
          <option value="DELAYED_DELIVERY">배송 지연</option>
          <option value="INCORRECT_INFO">상품 정보 오류</option>
          <option value="PRODUCT_UNSATISFIED">상품 문제</option>
        </Form.Select></Col>
        <Col md={2}><Form.Control type="number" min={1} max={remainQuantity}
          value={quantity} onChange={(event) => onQuantity(event.target.value)}
          placeholder="수량(전체)" /></Col>
        <Col md={6}><Form.Control maxLength={500} value={detailedReason}
          onChange={(event) => onDetailedReason(event.target.value)}
          placeholder="상세 사유" /></Col>
      </Row>
      <Button className="mt-2" type="submit" size="sm" variant="outline-danger"
        disabled={pending}>취소 요청</Button>
    </Form>
  );
}

function SellerReturnRequestForm({
  pending,
  remainQuantity,
  returnReason,
  collectDeliveryMethod,
  collectDeliveryCompany,
  collectTrackingNumber,
  returnQuantity,
  onReturnReason,
  onCollectDeliveryMethod,
  onCollectDeliveryCompany,
  onCollectTrackingNumber,
  onReturnQuantity,
  onAction,
}: {
  pending: boolean;
  remainQuantity: number;
  returnReason: string;
  collectDeliveryMethod: string;
  collectDeliveryCompany: string;
  collectTrackingNumber: string;
  returnQuantity: string;
  onReturnReason: (value: string) => void;
  onCollectDeliveryMethod: (value: string) => void;
  onCollectDeliveryCompany: (value: string) => void;
  onCollectTrackingNumber: (value: string) => void;
  onReturnQuantity: (value: string) => void;
  onAction: (request: ChannelAction) => void;
}) {
  return (
    <Form className="border rounded p-3 mb-3" onSubmit={(event) => {
      event.preventDefault();
      onAction({
        kind: "requestSellerReturn",
        request: {
          returnReason,
          collectDeliveryMethod,
          collectDeliveryCompany: collectDeliveryCompany || undefined,
          collectTrackingNumber: collectTrackingNumber || undefined,
          returnQuantity: returnQuantity ? Number(returnQuantity) : undefined,
        },
      });
    }}>
      <div className="fw-semibold mb-2">판매자 반품 요청</div>
      <Row className="g-2">
        <Col md={4}><Form.Select value={returnReason}
          onChange={(event) => onReturnReason(event.target.value)}>
          <option value="PRODUCT_UNSATISFIED">상품 문제</option>
          <option value="DELAYED_DELIVERY">배송 지연</option>
          <option value="SOLD_OUT">품절</option>
          <option value="WRONG_ORDER">오배송</option>
          <option value="BROKEN">상품 파손</option>
        </Form.Select></Col>
        <Col md={4}><Form.Select value={collectDeliveryMethod}
          onChange={(event) => onCollectDeliveryMethod(event.target.value)}>
          <option value="RETURN_DESIGNATED">지정 택배 수거</option>
          <option value="RETURN_INDIVIDUAL">구매자 직접 반송</option>
        </Form.Select></Col>
        <Col md={4}><Form.Control type="number" min={1} max={remainQuantity}
          value={returnQuantity} onChange={(event) => onReturnQuantity(event.target.value)}
          placeholder="수량(전체)" /></Col>
        <Col md={6}><Form.Control list="smartstore-delivery-companies"
          value={collectDeliveryCompany}
          onChange={(event) => onCollectDeliveryCompany(event.target.value.toUpperCase())}
          placeholder="수거 택배사 코드 (선택)" /></Col>
        <Col md={6}><Form.Control value={collectTrackingNumber}
          onChange={(event) => onCollectTrackingNumber(event.target.value)}
          placeholder="수거 운송장 번호 (선택)" /></Col>
      </Row>
      <Button className="mt-2" type="submit" size="sm" variant="outline-danger"
        disabled={pending}>반품 요청</Button>
    </Form>
  );
}

function ExchangeClaimPanel({
  held,
  pending,
  rejectReason,
  holdbackClassType,
  detailedReason,
  extraExchangeFeeAmount,
  onRejectReason,
  onHoldbackClassType,
  onDetailedReason,
  onExtraExchangeFeeAmount,
  onAction,
}: {
  held: boolean;
  pending: boolean;
  rejectReason: string;
  holdbackClassType: string;
  detailedReason: string;
  extraExchangeFeeAmount: string;
  onRejectReason: (value: string) => void;
  onHoldbackClassType: (value: string) => void;
  onDetailedReason: (value: string) => void;
  onExtraExchangeFeeAmount: (value: string) => void;
  onAction: (request: ChannelAction) => void;
}) {
  return (
    <div className="border rounded p-3 mb-3">
      <div className="fw-semibold mb-2">교환 클레임 처리</div>
      <div className="d-flex flex-wrap gap-2 mb-3">
        <Button size="sm" variant="outline-success" disabled={pending}
          onClick={() => onAction({ kind: "completeExchangeCollect" })}>
          수거 완료
        </Button>
        {held && <Button size="sm" variant="outline-primary"
          disabled={pending}
          onClick={() => onAction({ kind: "releaseExchangeHold" })}>
          보류 해제
        </Button>}
      </div>
      <Form className="mb-3" onSubmit={(event) => {
        event.preventDefault();
        onAction({ kind: "rejectExchange", reason: rejectReason });
      }}>
        <Form.Control required maxLength={500} value={rejectReason}
          onChange={(event) => onRejectReason(event.target.value)}
          placeholder="교환 거절 사유" />
        <Button className="mt-2" type="submit" size="sm" variant="outline-danger"
          disabled={pending || !rejectReason.trim()}>교환 거절</Button>
      </Form>
      <Form onSubmit={(event) => {
        event.preventDefault();
        onAction({
          kind: "holdExchange",
          request: {
            holdbackClassType,
            detailedReason,
            extraExchangeFeeAmount: extraExchangeFeeAmount
              ? Number(extraExchangeFeeAmount) : undefined,
          },
        });
      }}>
        <Row className="g-2">
          <Col md={4}><Form.Select value={holdbackClassType}
            onChange={(event) => onHoldbackClassType(event.target.value)}>
            <option value="EXCHANGE_DELIVERYFEE">교환 배송비</option>
            <option value="EXCHANGE_EXTRAFEE">추가 비용</option>
            <option value="EXCHANGE_PRODUCT_READY">교환 상품 준비</option>
            <option value="PURCHASER_CONFIRM_NEED">구매자 확인 필요</option>
            <option value="SELLER_CONFIRM_NEED">판매자 확인 필요</option>
            <option value="ETC">기타</option>
          </Form.Select></Col>
          <Col md={5}><Form.Control required maxLength={500} value={detailedReason}
            onChange={(event) => onDetailedReason(event.target.value)}
            placeholder="보류 상세 사유" /></Col>
          <Col md={3}><Form.Control type="number" min={0} value={extraExchangeFeeAmount}
            onChange={(event) => onExtraExchangeFeeAmount(event.target.value)}
            placeholder="추가 비용" /></Col>
        </Row>
        <Button className="mt-2" type="submit" size="sm" variant="outline-warning"
          disabled={pending || !detailedReason.trim()}>교환 보류</Button>
      </Form>
    </div>
  );
}

function ExchangeDispatchForm({
  pending,
  deliveryMethod,
  deliveryCompanyCode,
  trackingNumber,
  onDeliveryMethod,
  onDeliveryCompanyCode,
  onTrackingNumber,
  onAction,
}: {
  pending: boolean;
  deliveryMethod: string;
  deliveryCompanyCode: string;
  trackingNumber: string;
  onDeliveryMethod: (value: string) => void;
  onDeliveryCompanyCode: (value: string) => void;
  onTrackingNumber: (value: string) => void;
  onAction: (request: ChannelAction) => void;
}) {
  return (
    <Form
      onSubmit={(event) => {
        event.preventDefault();
        onAction({
          kind: "dispatchExchange",
          request: { deliveryMethod, deliveryCompanyCode, trackingNumber },
        });
      }}
    >
      <div className="fw-semibold mb-2">교환품 재배송</div>
      <DeliveryFields
        required
        deliveryMethod={deliveryMethod}
        deliveryCompanyCode={deliveryCompanyCode}
        trackingNumber={trackingNumber}
        onDeliveryMethod={onDeliveryMethod}
        onDeliveryCompanyCode={onDeliveryCompanyCode}
        onTrackingNumber={onTrackingNumber}
      />
      <Button type="submit" size="sm" disabled={pending}>교환품 발송</Button>
    </Form>
  );
}

function amount(value: number | null): string {
  return value === null ? "-" : formatKRW(value);
}
