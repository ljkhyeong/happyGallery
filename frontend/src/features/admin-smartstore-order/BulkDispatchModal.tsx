import { useState } from "react";
import { Alert, Button, Col, Form, Modal, Row, Table } from "react-bootstrap";
import type { SmartStoreOrderBulkActionResponse } from "@/generated/api/adminOrder";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { formatDateTimeInput } from "@/shared/lib";
import { ErrorAlert, useToast } from "@/shared/ui";
import { dispatchSmartStoreOrders } from "./api";
import { DeliveryCompanyDatalist, DeliveryMethodOptions } from "./DeliveryFields";

export function BulkResultAlert({ result }: { result: SmartStoreOrderBulkActionResponse }) {
  return <Alert variant={result.failures.length ? "warning" : "success"}>
    성공 {result.successProductOrderIds.length}건 · 실패 {result.failures.length}건
    {result.failures.length > 0 && <ul className="small mb-0 mt-2">
      {result.failures.map((failure) => <li key={failure.productOrderId}>
        {failure.productOrderId}: {failure.message}
        {failure.code ? ` (${failure.code})` : ""}
      </li>)}
    </ul>}
  </Alert>;
}

export function BulkDispatchModal({
  adminKey,
  productOrderIds,
  onAuthError,
  onClose,
  onCompleted,
}: {
  adminKey: string;
  productOrderIds: string[];
  onAuthError: () => void;
  onClose: () => void;
  onCompleted: (result: SmartStoreOrderBulkActionResponse) => void;
}) {
  const toast = useToast();
  const [deliveryMethod, setDeliveryMethod] = useState("DELIVERY");
  const [deliveryCompanyCode, setDeliveryCompanyCode] = useState("");
  const [dispatchDate, setDispatchDate] = useState(() => formatDateTimeInput(Date.now()));
  const [trackingNumbers, setTrackingNumbers] = useState<Record<string, string>>({});
  const dispatch = useAdminMutation(onAuthError, {
    mutationFn: () => dispatchSmartStoreOrders(adminKey, {
      orders: productOrderIds.map((productOrderId) => ({
        productOrderId,
        deliveryMethod,
        deliveryCompanyCode: deliveryCompanyCode || undefined,
        trackingNumber: trackingNumbers[productOrderId] || undefined,
        dispatchDate,
      })),
    }),
    onSuccess: (result) => {
      toast.show(`발송 ${result.successProductOrderIds.length}건을 요청했습니다.`);
      onCompleted(result);
    },
  });
  const valid = productOrderIds.length > 0 && dispatchDate.length > 0 && !dispatch.isPending;

  return <Modal show onHide={() => { if (!dispatch.isPending) onClose(); }} size="lg" centered>
    <Form onSubmit={(event) => { event.preventDefault(); if (valid) dispatch.mutate(); }}>
      <Modal.Header closeButton={!dispatch.isPending}><Modal.Title className="fs-6">
        스마트스토어 주문 {productOrderIds.length}건 발송
      </Modal.Title></Modal.Header>
      <Modal.Body>
        <ErrorAlert error={dispatch.error} />
        <fieldset disabled={dispatch.isPending}>
          <Row className="g-2 mb-3">
            <Col md={4}><Form.Select value={deliveryMethod}
              onChange={(event) => setDeliveryMethod(event.target.value)}>
              <DeliveryMethodOptions />
            </Form.Select></Col>
            <Col md={4}><Form.Control list="smartstore-delivery-companies"
              value={deliveryCompanyCode}
              onChange={(event) => setDeliveryCompanyCode(event.target.value.toUpperCase())}
              placeholder="택배사 코드" /></Col>
            <Col md={4}><Form.Control type="datetime-local" required value={dispatchDate}
              onChange={(event) => setDispatchDate(event.target.value)} /></Col>
          </Row>
          <Table responsive size="sm" className="align-middle">
            <thead><tr><th>상품 주문 번호</th><th>운송장 번호</th></tr></thead>
            <tbody>{productOrderIds.map((productOrderId) => <tr key={productOrderId}>
              <td className="small">{productOrderId}</td>
              <td><Form.Control value={trackingNumbers[productOrderId] ?? ""}
                onChange={(event) => setTrackingNumbers((current) => ({
                  ...current, [productOrderId]: event.target.value,
                }))} placeholder="운송장 번호" /></td>
            </tr>)}</tbody>
          </Table>
        </fieldset>
        <DeliveryCompanyDatalist />
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" disabled={dispatch.isPending} onClick={onClose}>취소</Button>
        <Button type="submit" disabled={!valid}>
          {dispatch.isPending ? "발송 요청 중..." : "일괄 발송"}
        </Button>
      </Modal.Footer>
    </Form>
  </Modal>;
}
