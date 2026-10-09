import type { Route } from "./+types/privacy";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import { loadPolicyRoute, policyDocument } from "@/features/policy-consent/policyRoute";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
} from "@/shared/seo/metadata";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const TITLE = "개인정보처리방침 | 해피갤러리";
const DESCRIPTION = "해피갤러리 개인정보처리방침입니다.";

export function loader({ params }: Route.LoaderArgs) {
  return loadPolicyRoute("privacy", params.version);
}

export function meta({ loaderData, params }: Route.MetaArgs) {
  return buildSeoMeta({
    title: TITLE,
    description: DESCRIPTION,
    pathname: loaderData?.pathname ?? `/privacy/${params.version ?? ""}`,
    image: heroWorkshop,
    indexable: Boolean(loaderData?.current),
    followLinks: Boolean(loaderData),
  });
}

export default function PrivacyRoute({ loaderData }: Route.ComponentProps) {
  const PolicyDocument = policyDocument("privacy", loaderData.version);
  return (
    <>
      <CspJsonLd value={[
        buildWebPageJsonLd(loaderData.pathname, TITLE, DESCRIPTION),
        buildBreadcrumbJsonLd([
          { name: "홈", pathname: "/" },
          { name: "개인정보처리방침", pathname: loaderData.pathname },
        ]),
      ]} />
      <PolicyDocument />
    </>
  );
}

export { PublicRouteErrorBoundary as ErrorBoundary } from "@/shared/seo/PublicRouteErrorBoundary";
