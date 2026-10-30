import { clientOnlyMeta } from "@/shared/seo/metadata";

export { GuestLookupPage as default } from "@/pages/GuestLookupPage";

export function meta() {
  return clientOnlyMeta("비회원 조회");
}
