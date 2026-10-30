import { clientOnlyMeta } from "@/shared/seo/metadata";

export { ForgotPasswordPage as default } from "@/pages/ForgotPasswordPage";

export function meta() {
  return clientOnlyMeta("비밀번호 재설정");
}
