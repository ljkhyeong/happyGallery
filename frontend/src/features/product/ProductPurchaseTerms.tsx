import { Alert, Button } from "react-bootstrap";
import { MessageCircleMore } from "lucide-react";
import { useWorkshopProfile } from "@/features/workshop/useWorkshopProfile";
import type { ProductType } from "@/shared/types/product";

interface Props {
  productName: string;
  type: ProductType | null;
  specification: string | null;
  careInstructions: string | null;
  productionLeadDays: number | null;
  compact?: boolean;
  showCustomizationInquiry?: boolean;
  showLegacySnapshotNotice?: boolean;
}

export function ProductPurchaseTerms({
  productName,
  type,
  specification,
  careInstructions,
  productionLeadDays,
  compact = false,
  showCustomizationInquiry = true,
  showLegacySnapshotNotice = false,
}: Props) {
  const isMadeToOrder = type === "MADE_TO_ORDER";

  return (
    <div className={compact ? "small" : ""}>
      {type == null && showLegacySnapshotNotice && (
        <Alert variant="secondary" className="py-2 mb-2">
          구매 조건 저장 기능을 도입하기 전 주문입니다.
          당시 안내 내용은 주문 상담 기록으로 확인해 주세요.
        </Alert>
      )}
      {isMadeToOrder && showCustomizationInquiry && (
        <Alert variant="info" className="py-2 mb-2">
          온라인 주문은 아래 고정 사양으로 결제 후 제작합니다.
          맞춤 변경은 결제 전에 네이버톡톡으로 상담해 주세요.
        </Alert>
      )}
      {specification && (
        <div className="mb-2">
          <strong className="d-block">상품 사양</strong>
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
        <CustomizationInquiry productName={productName} />
      )}
    </div>
  );
}

function CustomizationInquiry({ productName }: { productName: string }) {
  const { data: workshop } = useWorkshopProfile();
  if (!workshop?.naverTalkUrl) return null;

  const openInquiry = () => {
    const message = [
      "해피갤러리 상품 맞춤 변경 문의드립니다.",
      `상품: ${productName}`,
      "원하는 변경: ",
    ].join("\n");
    void navigator.clipboard?.writeText(message);
  };

  return (
    <Button
      as="a"
      href={workshop.naverTalkUrl}
      target="_blank"
      rel="noreferrer"
      variant="outline-success"
      size="sm"
      onClick={openInquiry}
    >
      <MessageCircleMore size={15} aria-hidden="true" className="me-1" />
      맞춤 변경 네이버톡톡 상담
    </Button>
  );
}
