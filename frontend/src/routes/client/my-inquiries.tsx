import { myNavLabel } from "@/features/my/myNavigation";
import { clientOnlyMeta } from "@/shared/seo/metadata";

export { MyInquiriesPage as default } from "@/pages/MyInquiriesPage";

export function meta() {
  return clientOnlyMeta(myNavLabel("/my/inquiries"));
}
