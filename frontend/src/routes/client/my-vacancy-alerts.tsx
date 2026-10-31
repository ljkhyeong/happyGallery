import { myNavLabel } from "@/features/my/myNavigation";
import { clientOnlyMeta } from "@/shared/seo/metadata";

export { MyVacancyAlertsPage as default } from "@/pages/MyVacancyAlertsPage";

export function meta() {
  return clientOnlyMeta(myNavLabel("/my/vacancy-alerts"));
}
