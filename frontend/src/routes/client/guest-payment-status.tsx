import { clientOnlyMeta } from "@/shared/seo/metadata";

export { GuestPaymentStatusPage as default } from "@/pages/GuestPaymentStatusPage";

export function meta() {
  return clientOnlyMeta("비회원 결제 상태");
}
