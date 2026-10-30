import { myNavLabel } from "@/features/my/myNavigation";
import { clientOnlyMeta } from "@/shared/seo/metadata";

export { MyNotificationsPage as default } from "@/pages/MyNotificationsPage";

export function meta() {
  return clientOnlyMeta(myNavLabel("/my/notifications"));
}
