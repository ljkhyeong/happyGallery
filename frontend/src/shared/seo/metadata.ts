import type { MetaDescriptor } from "react-router";

export const SITE_ORIGIN = "https://happy-gallery.com";
export const SITE_NAME = "해피갤러리";
export const DEFAULT_SEO_DESCRIPTION =
  "충주 해피갤러리의 공예 원데이클래스, 정규 과정, 단체수업과 공방 작품을 만나보세요.";

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

  return [
    { title },
    { name: "description", content: description },
    {
      name: "robots",
      content: `${indexable ? "index" : "noindex"},${followLinks ? "follow" : "nofollow"}`,
    },
    { tagName: "link", rel: "canonical", href: canonical },
    { property: "og:type", content: type },
    { property: "og:locale", content: "ko_KR" },
    { property: "og:site_name", content: SITE_NAME },
    { property: "og:title", content: title },
    { property: "og:description", content: description },
    { property: "og:url", content: canonical },
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

export function buildBreadcrumbJsonLd(items: readonly BreadcrumbItem[]): JsonLd {
  return {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: items.map((item, index) => ({
      "@type": "ListItem",
      position: index + 1,
      name: item.name,
      item: absoluteSiteUrl(item.pathname),
    })),
  };
}
