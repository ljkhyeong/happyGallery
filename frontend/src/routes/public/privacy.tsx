import type { Route } from "./+types/privacy";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import {
  PRIVACY_POLICY_DOCUMENTS,
  resolvePolicyDocument,
} from "@/features/policy-consent/policyDocuments";
import { PRIVACY_POLICY_VERSION } from "@/features/policy-consent/policyVersions";
import { PrivacyPolicyPage } from "@/pages/PrivacyPolicyPage";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
} from "@/shared/seo/metadata";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const TITLE = "개인정보처리방침 | 해피갤러리";
const DESCRIPTION = "해피갤러리 개인정보처리방침입니다.";

export function loader({ params }: Route.LoaderArgs) {
  const version = params.version ?? PRIVACY_POLICY_VERSION;
  if (!resolvePolicyDocument(PRIVACY_POLICY_DOCUMENTS, version)) {
    throw new Response(null, { status: 404, statusText: "Not Found" });
  }
  return { version, current: version === PRIVACY_POLICY_VERSION };
}

export function meta({ loaderData, params }: Route.MetaArgs) {
  const pathname = loaderData?.current ? "/privacy" : `/privacy/${params.version ?? ""}`;
  return buildSeoMeta({
    title: TITLE,
    description: DESCRIPTION,
    pathname,
    image: heroWorkshop,
    indexable: Boolean(loaderData?.current),
    followLinks: Boolean(loaderData),
  });
}

export default function PrivacyRoute({ loaderData }: Route.ComponentProps) {
  const pathname = loaderData.current ? "/privacy" : `/privacy/${loaderData.version}`;
  return (
    <>
      <CspJsonLd value={[
        buildWebPageJsonLd(pathname, TITLE, DESCRIPTION),
        buildBreadcrumbJsonLd([
          { name: "홈", pathname: "/" },
          { name: "개인정보처리방침", pathname },
        ]),
      ]} />
      <PrivacyPolicyPage />
    </>
  );
}

export { PublicRouteErrorBoundary as ErrorBoundary } from "@/shared/seo/PublicRouteErrorBoundary";
