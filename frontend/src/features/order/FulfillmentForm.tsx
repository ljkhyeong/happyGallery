import type { FulfillmentSelection } from "./fulfillmentSelection";
import { SavedShippingAddressActions } from "@/features/my/DefaultShippingAddress";
import { Button, ButtonGroup } from "react-bootstrap";
import { useOrderPricePolicy } from "./useOrderPricePolicy";
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
      {/* 쓰는 곳마다 "수령 방법"·"받는 방법" 칸 제목이 있어 보이는 이름표는 두지 않고 버튼 묶음 이름으로만 둔다. */}
      <ButtonGroup className="w-100 mb-3" aria-label="수령 방법">
        <Button
          type="button"
          variant={value.fulfillmentType === "PICKUP" ? "dark" : "outline-dark"}
          aria-pressed={value.fulfillmentType === "PICKUP"}
          onClick={() => onChange({ ...value, fulfillmentType: "PICKUP" })}
        >
          매장 수령
        </Button>
        <Button
          type="button"
          variant={value.fulfillmentType === "SHIPPING" ? "dark" : "outline-dark"}
          aria-pressed={value.fulfillmentType === "SHIPPING"}
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
          <WorkshopVisitInfo />
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
