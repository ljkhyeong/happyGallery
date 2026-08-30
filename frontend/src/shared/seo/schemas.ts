import type { ClassResponse } from "@/generated/api/booking";
import type { NoticeDetailResponse } from "@/generated/api/notice";
import type { ProductDetailResponse } from "@/generated/api/product";
import type { WorkshopProfileResponse } from "@/generated/api/workshop";
import { absoluteSiteUrl, SITE_ORIGIN, seoDescription } from "./metadata";

type JsonLd = Record<string, unknown>;

const ORGANIZATION_ID = `${SITE_ORIGIN}/#organization`;

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
    brand: { "@id": ORGANIZATION_ID },
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
    provider: { "@id": ORGANIZATION_ID },
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
    datePublished: notice.createdAt,
    mainEntityOfPage: url,
    image: absoluteSiteUrl(image),
    author: { "@id": ORGANIZATION_ID },
    publisher: { "@id": ORGANIZATION_ID },
  };
}
