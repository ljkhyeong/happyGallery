import { clientOnlyMeta } from "@/shared/seo/metadata";

export { BookingCreatePage as default } from "@/pages/BookingCreatePage";

export function meta() {
  return clientOnlyMeta("체험 예약");
}
