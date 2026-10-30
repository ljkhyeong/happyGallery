import { Alert } from "react-bootstrap";
import { useWorkshopProfile } from "@/features/workshop/useWorkshopProfile";
import { NaverTalkInquiry } from "@/features/workshop/NaverTalkInquiry";
import type { ProductType } from "@/shared/types/product";

interface Props {
  productName: string;
  type: ProductType | null;
  specification: string | null;
  careInstructions: string | null;
  productionLeadDays: number | null;
  compact?: boolean;
  /** 작품이 여러 개인 목록은 끄고 목록 아래에 `MadeToOrderListNotice`를 한 번만 둔다. */
  showCustomizationInquiry?: boolean;
}

export function ProductPurchaseTerms({
  productName,
  type,
  specification,
  careInstructions,
  productionLeadDays,
  compact = false,
  showCustomizationInquiry = true,
}: Props) {
  const isMadeToOrder = type === "MADE_TO_ORDER";

  return (
    <div className={compact ? "small" : ""}>
      {isMadeToOrder && showCustomizationInquiry && (
        <Alert variant="info" className="py-2 mb-2">
          온라인 주문은 아래 고정 사양으로 결제 후 제작합니다.
          맞춤 변경은 결제 전에 네이버톡톡으로 상담해 주세요.
        </Alert>
      )}
      {specification && (
        <div className="mb-2">
          <strong className="d-block">작품 사양</strong>
          <span className="text-muted-soft" style={{ whiteSpace: "pre-wrap" }}>
            {specification}
          </span>
        </div>
      )}
      {careInstructions && (
        <div className="mb-2">
          <strong className="d-block">관리 방법</strong>
          <span className="text-muted-soft" style={{ whiteSpace: "pre-wrap" }}>
            {careInstructions}
          </span>
        </div>
      )}
      {productionLeadDays != null && (
        <div className="mb-2">
          <strong className="d-block">예상 제작 기간</strong>
          <span className="text-muted-soft">결제 완료 후 {productionLeadDays}일</span>
        </div>
      )}
      {isMadeToOrder && showCustomizationInquiry && (
        <CustomizationInquiry productNames={[productName]} />
      )}
    </div>
  );
}

/** 장바구니·주문서 목록: 주문 제작 작품이 있으면 고정 사양 안내와 맞춤 변경 상담을 목록 아래에 한 번만 보여 준다. */
export function MadeToOrderListNotice({ productNames }: { productNames: readonly string[] }) {
  const names = [...new Set(productNames)];
  if (names.length === 0) return null;

  return (
    <div className="small mt-3">
      <Alert variant="info" className="py-2 mb-2">
        주문 제작 작품은 각 작품에 적힌 사양으로 결제 후 제작합니다.
        맞춤 변경은 결제 전에 네이버톡톡으로 상담해 주세요.
      </Alert>
      <CustomizationInquiry productNames={names} />
    </div>
  );
}

function CustomizationInquiry({ productNames }: { productNames: readonly string[] }) {
  const { data: workshop } = useWorkshopProfile();
  if (!workshop?.naverTalkUrl) return null;

  const message = [
    "해피갤러리 작품 맞춤 변경 문의드립니다.",
    `작품: ${productNames.join(", ")}`,
    "원하는 변경: ",
  ].join("\n");

  return (
    <NaverTalkInquiry message={message} url={workshop.naverTalkUrl} label="맞춤 변경 네이버톡톡 상담" />
  );
}
