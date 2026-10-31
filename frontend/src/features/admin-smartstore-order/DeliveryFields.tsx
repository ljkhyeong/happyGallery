import { Form } from "react-bootstrap";
import { DELIVERY_METHOD_LABELS } from "./labels";

export function DeliveryMethodOptions() {
  return Object.entries(DELIVERY_METHOD_LABELS).map(([value, label]) => (
    <option key={value} value={value}>{label}</option>
  ));
}

export function DeliveryFields({
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
      <DeliveryMethodOptions />
    </Form.Select>
    <Form.Control
      className="mb-2"
      list="smartstore-delivery-companies"
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

export function DeliveryCompanyDatalist() {
  return <datalist id="smartstore-delivery-companies">
    <option value="CJGLS">CJ대한통운</option>
    <option value="HYUNDAI">롯데택배</option>
    <option value="HANJIN">한진택배</option>
    <option value="KGB">로젠택배</option>
    <option value="EPOST">우체국택배</option>
    <option value="CVSNET">GS25 편의점택배</option>
    <option value="CUPARCEL">CU 편의점택배</option>
    <option value="KDEXP">경동택배</option>
    <option value="DAESIN">대신택배</option>
  </datalist>;
}
