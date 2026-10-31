import { myNavLabel } from "@/features/my/myNavigation";
import { clientOnlyMeta } from "@/shared/seo/metadata";

export { MyGroupInquiriesPage as default } from "@/pages/MyGroupInquiriesPage";

export function meta() {
  return clientOnlyMeta(myNavLabel("/my/group-inquiries"));
}
