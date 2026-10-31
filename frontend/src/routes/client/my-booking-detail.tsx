import { clientOnlyMeta } from "@/shared/seo/metadata";

export { MyBookingDetailPage as default } from "@/pages/MyBookingDetailPage";

export function meta() {
  return clientOnlyMeta("예약 상세");
}
