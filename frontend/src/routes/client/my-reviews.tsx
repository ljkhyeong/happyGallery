import { myNavLabel } from "@/features/my/myNavigation";
import { clientOnlyMeta } from "@/shared/seo/metadata";

export { MyReviewsPage as default } from "@/pages/MyReviewsPage";

export function meta() {
  return clientOnlyMeta(myNavLabel("/my/reviews"));
}
