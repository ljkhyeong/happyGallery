import { clientOnlyMeta } from "@/shared/seo/metadata";

export { MyOrderDetailPage as default } from "@/pages/MyOrderDetailPage";

export function meta() {
  return clientOnlyMeta("주문 상세");
}
