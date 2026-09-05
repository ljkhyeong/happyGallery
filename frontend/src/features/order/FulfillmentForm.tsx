import type { FulfillmentSelection } from "./fulfillmentSelection";
export { useFulfillmentSelection, isFulfillmentComplete, fulfillmentPayload } from "./fulfillmentSelection";
export type { FulfillmentSelection } from "./fulfillmentSelection";
import { SavedShippingAddressActions } from "@/features/my/DefaultShippingAddress";
import { Button, ButtonGroup, Form } from "react-bootstrap";
import { useOrderPricePolicy } from "@/features/order/useOrderPricePolicy";
import { WorkshopVisitInfo } from "@/features/workshop/WorkshopVisitInfo";
import { formatKRW } from "@/shared/lib";
import { ShippingAddressFields } from "./ShippingAddressFields";

interface Props {
  value: FulfillmentSelection;
  onChange: (value: FulfillmentSelection) => void;
}

export function FulfillmentForm({ value, onChange }: Props) {
  const { data: pricePolicy, isLoading: isPricePolicyLoading, isError: isPricePolicyError } =
    useOrderPricePolicy();


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
        <>
        <SavedShippingAddressActions address={value.shippingAddress} onLoad={(shippingAddress) => onChange({ ...value, shippingAddress })} />
        <ShippingAddressFields
          value={value.shippingAddress}
          onChange={(shippingAddress) => onChange({ ...value, shippingAddress })}
        />
        </>
      )}
    </div>
  );
}
