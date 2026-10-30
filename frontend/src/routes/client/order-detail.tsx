import { clientOnlyMeta } from "@/shared/seo/metadata";

export { OrderDetailPage as default } from "@/pages/OrderDetailPage";

export function meta() {
  return clientOnlyMeta("비회원 주문 조회");
}
