import { clientOnlyMeta } from "@/shared/seo/metadata";

export { AdminPage as default } from "@/pages/admin/AdminPage";

export function meta() {
  return clientOnlyMeta("관리자");
}
