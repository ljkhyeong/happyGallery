import type { Route } from "./+types/terms";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import { loadPolicyRoute, policyDocument } from "@/features/policy-consent/policyRoute";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
  siteTitle,
} from "@/shared/seo/metadata";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const TITLE = siteTitle("이용약관");
const DESCRIPTION = "해피갤러리 서비스 이용약관입니다.";

export function loader({ params }: Route.LoaderArgs) {
  return loadPolicyRoute("terms", params.version);
}

export function meta({ loaderData, params }: Route.MetaArgs) {
  return buildSeoMeta({
    title: TITLE,
    description: DESCRIPTION,
    pathname: loaderData?.pathname ?? `/terms/${params.version ?? ""}`,
    image: heroWorkshop,
    indexable: Boolean(loaderData?.current),
    followLinks: Boolean(loaderData),
  });
}

export default function TermsRoute({ loaderData }: Route.ComponentProps) {
  const PolicyDocument = policyDocument("terms", loaderData.version);
  return (
    <>
      <CspJsonLd value={[
        buildWebPageJsonLd(loaderData.pathname, TITLE, DESCRIPTION),
        buildBreadcrumbJsonLd([{ name: "이용약관", pathname: loaderData.pathname }]),
      ]} />
      <PolicyDocument />
    </>
  );
}

export { PublicRouteErrorBoundary as ErrorBoundary } from "@/shared/seo/PublicRouteErrorBoundary";
