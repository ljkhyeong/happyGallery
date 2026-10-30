import type { MetaDescriptor } from "react-router";

export const SITE_ORIGIN = "https://happy-gallery.com";
export const SITE_NAME = "해피갤러리";
/** 홈과 루트 meta 폴백이 함께 쓰는 기본 문서 제목. */
export const DEFAULT_SEO_TITLE = "해피갤러리 | 충주 공예 클래스와 핸드메이드 공방";
export const DEFAULT_SEO_DESCRIPTION =
  "충주 공예 공방 해피갤러리입니다. 원데이클래스·정규 과정·단체수업을 예약하고 공방에서 만든 작품을 살 수 있습니다.";

type JsonLd = Record<string, unknown>;

interface SeoMetadata {
  title: string;
  description: string;
  pathname: string;
  image: string;
  type?: "website" | "article" | "product";
  indexable?: boolean;
  followLinks?: boolean;
}

export interface BreadcrumbItem {
  name: string;
  pathname: string;
}

export function absoluteSiteUrl(pathOrUrl: string): string {
  return new URL(pathOrUrl, SITE_ORIGIN).toString();
}

/** 화면 이름 뒤에 공방 이름을 붙인 문서 제목(`이름 | 해피갤러리`). */
export function siteTitle(name: string): string {
  return `${name} | ${SITE_NAME}`;
}

/**
 * 로그인·결제·내 정보 같은 client-only 화면의 meta. 탭·방문 기록·화면 낭독기가 화면을 구분하도록 이름을 붙이고,
 * 검색에는 노출하지 않는다. 자식 라우트의 meta가 부모 meta를 대신하므로 robots도 함께 돌려준다.
 */
export function clientOnlyMeta(name?: string): MetaDescriptor[] {
  return [
    { title: name ? siteTitle(name) : SITE_NAME },
    { name: "robots", content: "noindex,nofollow" },
  ];
}

export function seoDescription(value: string | null | undefined, fallback: string): string {
  const normalized = value?.replace(/\s+/g, " ").trim();
  return (normalized || fallback).slice(0, 160);
}

export function buildSeoMeta({
  title,
  description,
  pathname,
  image,
  type = "website",
  indexable = true,
  followLinks = indexable,
}: SeoMetadata): MetaDescriptor[] {
  const canonical = absoluteSiteUrl(pathname);
  const socialImage = absoluteSiteUrl(image);
  // 색인과 링크 추적을 모두 막는 404·오류 응답은 대표 URL이 없으므로 내보내지 않는다.
  const hasRepresentativeUrl = indexable || followLinks;

  return [
    { title },
    { name: "description", content: description },
    {
      name: "robots",
      content: `${indexable ? "index" : "noindex"},${followLinks ? "follow" : "nofollow"}`,
    },
    ...(hasRepresentativeUrl ? [{ tagName: "link", rel: "canonical", href: canonical }] : []),
    { property: "og:type", content: type },
    { property: "og:locale", content: "ko_KR" },
    { property: "og:site_name", content: SITE_NAME },
    { property: "og:title", content: title },
    { property: "og:description", content: description },
    ...(hasRepresentativeUrl ? [{ property: "og:url", content: canonical }] : []),
    { property: "og:image", content: socialImage },
    { name: "twitter:card", content: "summary_large_image" },
    { name: "twitter:title", content: title },
    { name: "twitter:description", content: description },
    { name: "twitter:image", content: socialImage },
  ];
}

export function buildWebPageJsonLd(
  pathname: string,
  name: string,
  description: string,
  type = "WebPage",
): JsonLd {
  const url = absoluteSiteUrl(pathname);
  return {
    "@context": "https://schema.org",
    "@type": type,
    "@id": `${url}#webpage`,
    url,
    name,
    description,
    isPartOf: { "@id": `${SITE_ORIGIN}/#website` },
  };
}

export function buildWebSiteJsonLd(description: string): JsonLd {
  return {
    "@context": "https://schema.org",
    "@type": "WebSite",
    "@id": `${SITE_ORIGIN}/#website`,
    url: `${SITE_ORIGIN}/`,
    name: SITE_NAME,
    description,
    publisher: { "@id": `${SITE_ORIGIN}/#organization` },
  };
}

const HOME_BREADCRUMB: BreadcrumbItem = { name: "홈", pathname: "/" };

/** 홈을 첫 항목으로 붙인 BreadcrumbList. 홈 자체는 빈 배열을 넘긴다. */
export function buildBreadcrumbJsonLd(items: readonly BreadcrumbItem[]): JsonLd {
  return {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [HOME_BREADCRUMB, ...items].map((item, index) => ({
      "@type": "ListItem",
      position: index + 1,
      name: item.name,
      item: absoluteSiteUrl(item.pathname),
    })),
  };
}
