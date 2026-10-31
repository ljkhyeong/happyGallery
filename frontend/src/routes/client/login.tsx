import { clientOnlyMeta } from "@/shared/seo/metadata";

export { LoginPage as default } from "@/pages/LoginPage";

export function meta() {
  return clientOnlyMeta("로그인");
}
