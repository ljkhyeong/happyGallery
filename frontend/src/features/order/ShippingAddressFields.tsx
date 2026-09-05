import { Col, Form, InputGroup, Row } from "react-bootstrap";
import type { ShippingAddress } from "@/features/payment";
import { isValidPhone, normalizePhone } from "@/shared/validation/phone";
import { RoadAddressSearchButton } from "@/shared/ui/RoadAddressSearchButton";

interface Props {
  value: ShippingAddress;
  onChange: (value: ShippingAddress) => void;
}

export function ShippingAddressFields({ value, onChange }: Props) {
  const updateAddress = (field: keyof ShippingAddress, fieldValue: string) => {
    onChange({ ...value, [field]: field === "addressLine2" ? fieldValue || null : fieldValue });
  };

  return (
    <Row className="g-3">
      <Col sm={6}>
        <Form.Group controlId="shipping-recipient-name">
          <Form.Label>받는 분</Form.Label>
          <Form.Control
            value={value.recipientName}
            maxLength={100}
            onChange={(event) => updateAddress("recipientName", event.target.value)}
          />
        </Form.Group>
      </Col>
      <Col sm={6}>
        <Form.Group controlId="shipping-phone">
          <Form.Label>연락처</Form.Label>
          <Form.Control
            type="tel"
            inputMode="numeric"
            autoComplete="tel"
            maxLength={11}
            value={value.phone}
            isInvalid={
              value.phone.length > 0
              && !isValidPhone(value.phone)
            }
            onChange={(event) => updateAddress("phone", normalizePhone(event.target.value))}
            onPaste={(event) => {
              const pastedPhone = normalizePhone(event.clipboardData.getData("text"));
              if (pastedPhone.length <= 11) {
                event.preventDefault();
                updateAddress("phone", pastedPhone);
              }
            }}
          />
          <Form.Control.Feedback type="invalid">
            01로 시작하는 10~11자리 번호를 입력하세요.
          </Form.Control.Feedback>
        </Form.Group>
      </Col>
      <Col sm={4}>
        <Form.Group controlId="shipping-postal-code">
          <Form.Label>우편번호</Form.Label>
          <InputGroup>
            <Form.Control
              inputMode="numeric"
              maxLength={5}
              value={value.postalCode}
              onChange={(event) => updateAddress("postalCode", event.target.value.replace(/\D/g, ""))}
            />
            <RoadAddressSearchButton onSelect={(address) => {
              onChange({
                ...value,
                postalCode: address.postalCode,
                addressLine1: address.roadAddress,
              });
            }} />
          </InputGroup>
        </Form.Group>
      </Col>
      <Col sm={8}>
        <Form.Group controlId="shipping-address-line1">
          <Form.Label>기본 주소</Form.Label>
          <Form.Control
            maxLength={200}
            value={value.addressLine1}
            onChange={(event) => updateAddress("addressLine1", event.target.value)}
          />
        </Form.Group>
      </Col>
      <Col xs={12}>
        <Form.Group controlId="shipping-address-line2">
          <Form.Label>상세 주소</Form.Label>
          <Form.Control
            maxLength={200}
            value={value.addressLine2 ?? ""}
            onChange={(event) => updateAddress("addressLine2", event.target.value)}
          />
        </Form.Group>
      </Col>
    </Row>
  );
}
