import { myNavLabel } from "@/features/my/myNavigation";
import { clientOnlyMeta } from "@/shared/seo/metadata";

export { MyBookingsPage as default } from "@/pages/MyBookingsPage";

export function meta() {
  return clientOnlyMeta(myNavLabel("/my/bookings"));
}
