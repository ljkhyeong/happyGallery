import { clientOnlyMeta } from "@/shared/seo/metadata";

export { MyInquiryCreatePage as default } from "@/pages/MyInquiryCreatePage";

export function meta() {
  return clientOnlyMeta("1:1 문의 작성");
}
