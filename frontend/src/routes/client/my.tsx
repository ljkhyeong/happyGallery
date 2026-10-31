import { clientOnlyMeta } from "@/shared/seo/metadata";

export { MyPage as default } from "@/pages/MyPage";

export function meta() {
  return clientOnlyMeta("내 정보");
}
