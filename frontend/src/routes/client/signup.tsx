import { clientOnlyMeta } from "@/shared/seo/metadata";

export { SignupPage as default } from "@/pages/SignupPage";

export function meta() {
  return clientOnlyMeta("회원가입");
}
