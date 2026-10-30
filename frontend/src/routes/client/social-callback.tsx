import { clientOnlyMeta } from "@/shared/seo/metadata";

export { SocialCallbackPage as default } from "@/pages/SocialCallbackPage";

export function meta() {
  return clientOnlyMeta("소셜 로그인");
}
