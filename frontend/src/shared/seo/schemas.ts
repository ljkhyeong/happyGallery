import type { ClassResponse } from "@/generated/api/booking";
import type { NoticeDetailResponse } from "@/generated/api/notice";
import type { ProductDetailResponse } from "@/generated/api/product";
import type { WorkshopProfileResponse } from "@/generated/api/workshop";
import { withUtcOffset } from "../lib/format.ts";
import { absoluteSiteUrl, SITE_NAME, SITE_ORIGIN, seoDescription } from "./metadata.ts";

type JsonLd = Record<string, unknown>;

const ORGANIZATION_ID = `${SITE_ORIGIN}/#organization`;

/**
 * 검색엔진은 다른 페이지의 노드를 `@id`로 이어 붙이지 않는다.
 * 홈·사업자 정보 밖에서 조직을 참조할 때도 이름과 대표 URL을 함께 둔다.
 */
function organizationReference(): JsonLd {
  return {
    "@type": "Organization",
    "@id": ORGANIZATION_ID,
    name: SITE_NAME,
    url: `${SITE_ORIGIN}/`,
  };
}

export function buildLocalBusinessJsonLd(
  workshop: WorkshopProfileResponse,
  image: string,
): JsonLd {
  const streetAddress = [workshop.addressLine1, workshop.addressLine2]
    .filter(Boolean)
    .join(" ");
  const sameAs = [
    workshop.naverBlogUrl,
    workshop.instagramUrl,
    workshop.smartStoreUrl,
  ].filter((url): url is string => Boolean(url));

  return {
    "@context": "https://schema.org",
    "@type": ["LocalBusiness", "Organization"],
    "@id": ORGANIZATION_ID,
    name: workshop.name,
    url: `${SITE_ORIGIN}/`,
    image: absoluteSiteUrl(image),
    ...(workshop.introduction && { description: workshop.introduction }),
    ...(workshop.phone && { telephone: workshop.phone }),
    ...(workshop.email && { email: workshop.email }),
    ...(workshop.businessRegistrationNumber && {
      taxID: workshop.businessRegistrationNumber,
    }),
    ...(streetAddress && {
      address: {
        "@type": "PostalAddress",
        streetAddress,
        ...(workshop.postalCode && { postalCode: workshop.postalCode }),
        addressCountry: "KR",
      },
    }),
    ...(workshop.mapUrl && { hasMap: workshop.mapUrl }),
    ...(sameAs.length > 0 && { sameAs }),
  };
}

export function buildProductJsonLd(
  product: ProductDetailResponse,
  pathname: string,
  fallbackDescription: string,
): JsonLd {
  const url = absoluteSiteUrl(pathname);
  return {
    "@context": "https://schema.org",
    "@type": "Product",
    "@id": `${url}#product`,
    name: product.name,
    description: seoDescription(product.description, fallbackDescription),
    url,
    ...(product.imageUrl && { image: absoluteSiteUrl(product.imageUrl) }),
    brand: organizationReference(),
    offers: {
      "@type": "Offer",
      url,
      priceCurrency: "KRW",
      price: product.price,
      availability: product.available
        ? "https://schema.org/InStock"
        : "https://schema.org/OutOfStock",
    },
  };
}

export function buildCourseJsonLd(
  bookingClass: ClassResponse,
  pathname: string,
  fallbackDescription: string,
): JsonLd {
  const url = absoluteSiteUrl(pathname);
  return {
    "@context": "https://schema.org",
    "@type": "Course",
    "@id": `${url}#course`,
    name: bookingClass.name,
    description: seoDescription(bookingClass.description, fallbackDescription),
    url,
    provider: organizationReference(),
    timeRequired: `PT${bookingClass.durationMin}M`,
    ...(bookingClass.imageUrl && { image: absoluteSiteUrl(bookingClass.imageUrl) }),
    ...(bookingClass.targetAudience && {
      audience: {
        "@type": "Audience",
        audienceType: bookingClass.targetAudience,
      },
    }),
    offers: {
      "@type": "Offer",
      url: absoluteSiteUrl(`/bookings/new?classId=${bookingClass.id}`),
      priceCurrency: "KRW",
      price: bookingClass.price,
    },
  };
}

export function buildNoticeArticleJsonLd(
  notice: NoticeDetailResponse,
  pathname: string,
  image: string,
): JsonLd {
  const url = absoluteSiteUrl(pathname);
  return {
    "@context": "https://schema.org",
    "@type": "Article",
    "@id": `${url}#article`,
    headline: notice.title,
    articleBody: notice.content,
    datePublished: withUtcOffset(notice.createdAt),
    mainEntityOfPage: url,
    image: absoluteSiteUrl(image),
    author: organizationReference(),
    publisher: organizationReference(),
  };
}
