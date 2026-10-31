import { clientOnlyMeta } from "@/shared/seo/metadata";

export { PaymentSuccessPage as default } from "@/pages/PaymentSuccessPage";

export function meta() {
  return clientOnlyMeta("결제 결과");
}
