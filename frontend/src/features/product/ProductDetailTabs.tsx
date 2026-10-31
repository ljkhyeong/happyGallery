import { useScrollSpy } from "@/shared/hooks/useScrollSpy";

const DETAIL_TABS = [
  { id: "product-info", label: "작품 정보" },
  { id: "product-delivery", label: "배송·교환" },
  { id: "product-reviews", label: "후기" },
  { id: "product-qna", label: "문의" },
] as const;

// 구역은 붙어 있는 탭 아래(scroll-margin-top 4rem = 64px)에 멈추므로 그보다 조금 아래를 지나면 그 구역을 보는 중으로 본다.
const ACTIVE_SECTION_OFFSET_PX = 80;

/**
 * 상세 아래 구역(작품 정보·배송·교환·후기·문의)으로 바로 가는 탭. 화면 위에 붙어 따라오며
 * 지금 보고 있는 구역을 먹색 밑줄로 표시한다. 작품 정보가 없는 작품은 그 탭을 뺀다.
 */
export function ProductDetailTabs({ hasProductInfo }: { hasProductInfo: boolean }) {
  const tabs = hasProductInfo ? DETAIL_TABS : DETAIL_TABS.filter(({ id }) => id !== "product-info");
  const activeId = useScrollSpy(tabs.map(({ id }) => id), ACTIVE_SECTION_OFFSET_PX);

  return (
    <nav className="store-detail-tabs" aria-label="작품 상세 구역">
      {tabs.map(({ id, label }) => (
        <a key={id} href={`#${id}`} aria-current={activeId === id ? "location" : undefined}>{label}</a>
      ))}
    </nav>
  );
}
