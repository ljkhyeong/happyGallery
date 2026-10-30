import { clientOnlyMeta } from "@/shared/seo/metadata";

export { BookingManagePage as default } from "@/pages/BookingManagePage";

export function meta() {
  return clientOnlyMeta("비회원 예약 조회");
}
