import { useEffect, useState } from "react";
import { Button, ButtonGroup, Col, Form, Row } from "react-bootstrap";
import type { FulfillmentType, ShippingAddress } from "@/features/payment";
import { useOrderPricePolicy } from "@/features/order/useOrderPricePolicy";
import { WorkshopVisitInfo } from "@/features/workshop/WorkshopVisitInfo";
import { formatKRW } from "@/shared/lib";
import { isValidPhone, normalizePhone } from "@/shared/validation/phone";

export interface FulfillmentSelection {
  fulfillmentType: FulfillmentType | null;
  shippingAddress: ShippingAddress;
}

const emptyAddress: ShippingAddress = {
  recipientName: "",
  phone: "",
  postalCode: "",
  addressLine1: "",
  addressLine2: null,
};

function initialFulfillmentSelection(
  defaultName?: string,
  defaultPhone?: string,
): FulfillmentSelection {
  return {
    fulfillmentType: null,
    shippingAddress: {
      ...emptyAddress,
      recipientName: defaultName ?? "",
      phone: normalizePhone(defaultPhone),
    },
  };
}

export function useFulfillmentSelection(
  defaultName?: string,
  defaultPhone?: string,
) {
  const [selection, setSelection] = useState<FulfillmentSelection>(
    () => initialFulfillmentSelection(defaultName, defaultPhone),
  );

  useEffect(() => {
    setSelection((current) => ({
      ...current,
      shippingAddress: {
        ...current.shippingAddress,
        recipientName: current.shippingAddress.recipientName || defaultName || "",
        phone: current.shippingAddress.phone || normalizePhone(defaultPhone),
      },
    }));
  }, [defaultName, defaultPhone]);

  return [selection, setSelection] as const;
}

export function isFulfillmentComplete(selection: FulfillmentSelection) {
  if (selection.fulfillmentType === "PICKUP") return true;
  if (selection.fulfillmentType !== "SHIPPING") return false;
  const address = selection.shippingAddress;
  return Boolean(
    address.recipientName.trim()
      && isValidPhone(normalizePhone(address.phone))
      && /^\d{5}$/.test(address.postalCode.trim())
      && address.addressLine1.trim(),
  );
}

export function fulfillmentPayload(selection: FulfillmentSelection) {
  if (!selection.fulfillmentType) {
    throw new Error("수령 방법을 선택해 주세요.");
  }
  return {
    fulfillmentType: selection.fulfillmentType,
    shippingAddress: selection.fulfillmentType === "SHIPPING"
      ? {
          ...selection.shippingAddress,
          phone: normalizePhone(selection.shippingAddress.phone),
          addressLine2: selection.shippingAddress.addressLine2?.trim() || null,
        }
      : null,
  };
}

interface Props {
  value: FulfillmentSelection;
  onChange: (value: FulfillmentSelection) => void;
}

export function FulfillmentForm({ value, onChange }: Props) {
  const { data: pricePolicy, isLoading: isPricePolicyLoading, isError: isPricePolicyError } =
    useOrderPricePolicy();

  const updateAddress = (field: keyof ShippingAddress, fieldValue: string) => {
    onChange({
      ...value,
      shippingAddress: {
        ...value.shippingAddress,
        [field]: field === "addressLine2" ? fieldValue || null : fieldValue,
      },
    });
  };

  return (
    <div>
      <Form.Label className="fw-semibold d-block">수령 방법</Form.Label>
      <ButtonGroup className="w-100 mb-3" aria-label="수령 방법 선택">
        <Button
          type="button"
          variant={value.fulfillmentType === "PICKUP" ? "dark" : "outline-dark"}
          onClick={() => onChange({ ...value, fulfillmentType: "PICKUP" })}
        >
          매장 수령
        </Button>
        <Button
          type="button"
          variant={value.fulfillmentType === "SHIPPING" ? "dark" : "outline-dark"}
          disabled={!pricePolicy}
          onClick={() => onChange({ ...value, fulfillmentType: "SHIPPING" })}
        >
          <span className="d-block">택배 배송</span>
          <small className="d-block">
            {isPricePolicyLoading
              ? "배송비 확인 중"
              : pricePolicy?.shippingFee === 0
                ? "무료"
                : pricePolicy
                  ? formatKRW(pricePolicy.shippingFee)
                  : "선택 불가"}
          </small>
        </Button>
      </ButtonGroup>

      {isPricePolicyError && (
        <div className="small text-danger mb-3">배송비 정보를 불러오지 못해 택배를 선택할 수 없습니다.</div>
      )}

      {value.fulfillmentType === "PICKUP" && (
        <>
          <div className="small text-muted-soft mb-3">준비 완료 알림을 받은 뒤 매장에서 수령합니다.</div>
          <WorkshopVisitInfo compact />
        </>
      )}

      {value.fulfillmentType === "SHIPPING" && (
        <Row className="g-3">
          <Col sm={6}>
            <Form.Group controlId="shipping-recipient-name">
              <Form.Label>받는 분</Form.Label>
              <Form.Control
                value={value.shippingAddress.recipientName}
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
                value={value.shippingAddress.phone}
                isInvalid={
                  value.shippingAddress.phone.length > 0
                  && !isValidPhone(value.shippingAddress.phone)
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
              <Form.Control
                inputMode="numeric"
                maxLength={5}
                value={value.shippingAddress.postalCode}
                onChange={(event) => updateAddress("postalCode", event.target.value.replace(/\D/g, ""))}
              />
            </Form.Group>
          </Col>
          <Col sm={8}>
            <Form.Group controlId="shipping-address-line1">
              <Form.Label>기본 주소</Form.Label>
              <Form.Control
                maxLength={200}
                value={value.shippingAddress.addressLine1}
                onChange={(event) => updateAddress("addressLine1", event.target.value)}
              />
            </Form.Group>
          </Col>
          <Col xs={12}>
            <Form.Group controlId="shipping-address-line2">
              <Form.Label>상세 주소</Form.Label>
              <Form.Control
                maxLength={200}
                value={value.shippingAddress.addressLine2 ?? ""}
                onChange={(event) => updateAddress("addressLine2", event.target.value)}
              />
            </Form.Group>
          </Col>
        </Row>
      )}
    </div>
  );
}
