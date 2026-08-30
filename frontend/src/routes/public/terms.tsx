import type { Route } from "./+types/terms";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import {
  resolvePolicyDocument,
  TERMS_POLICY_DOCUMENTS,
} from "@/features/policy-consent/policyDocuments";
import { TERMS_POLICY_VERSION } from "@/features/policy-consent/policyVersions";
import { TermsPage } from "@/pages/TermsPage";
import {
  buildBreadcrumbJsonLd,
  buildSeoMeta,
  buildWebPageJsonLd,
} from "@/shared/seo/metadata";
import { CspJsonLd } from "@/shared/seo/CspJsonLd";

const TITLE = "이용약관 | 해피갤러리";
const DESCRIPTION = "해피갤러리 서비스 이용약관입니다.";

export function loader({ params }: Route.LoaderArgs) {
  const version = params.version ?? TERMS_POLICY_VERSION;
  if (!resolvePolicyDocument(TERMS_POLICY_DOCUMENTS, version)) {
    throw new Response(null, { status: 404, statusText: "Not Found" });
  }
  return { version, current: version === TERMS_POLICY_VERSION };
}

export function meta({ loaderData, params }: Route.MetaArgs) {
  const pathname = loaderData?.current ? "/terms" : `/terms/${params.version ?? ""}`;
  return buildSeoMeta({
    title: TITLE,
    description: DESCRIPTION,
    pathname,
    image: heroWorkshop,
    indexable: Boolean(loaderData?.current),
    followLinks: Boolean(loaderData),
  });
}

export default function TermsRoute({ loaderData }: Route.ComponentProps) {
  const pathname = loaderData.current ? "/terms" : `/terms/${loaderData.version}`;
  return (
    <>
      <CspJsonLd value={[
        buildWebPageJsonLd(pathname, TITLE, DESCRIPTION),
        buildBreadcrumbJsonLd([
          { name: "홈", pathname: "/" },
          { name: "이용약관", pathname },
        ]),
      ]} />
      <TermsPage />
    </>
  );
}

export { PublicRouteErrorBoundary as ErrorBoundary } from "@/shared/seo/PublicRouteErrorBoundary";
