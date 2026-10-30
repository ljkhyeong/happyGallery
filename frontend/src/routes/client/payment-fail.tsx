import { clientOnlyMeta } from "@/shared/seo/metadata";

export { PaymentFailPage as default } from "@/pages/PaymentFailPage";

export function meta() {
  return clientOnlyMeta("결제 실패");
}
