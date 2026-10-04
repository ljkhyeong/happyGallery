import assert from "node:assert/strict";
import test from "node:test";

import {
  absoluteSiteUrl,
  buildSeoMeta,
  buildWebSiteJsonLd,
  seoDescription,
} from "../../src/shared/seo/metadata.ts";
import {
  buildCourseJsonLd,
  buildNoticeArticleJsonLd,
  buildProductJsonLd,
} from "../../src/shared/seo/schemas.ts";

const notice = {
  id: 3,
  title: "추석 휴무 안내",
  content: "추석 연휴에는 공방을 쉽니다.",
  pinned: false,
  viewCount: 0,
  version: 0,
  createdAt: "2026-10-03T23:30:00",
};
const product = {
  id: 5,
  name: "레진 코스터",
  description: "투명 레진 코스터",
  imageUrl: null,
  price: 18000,
  available: true,
};
const bookingClass = {
  id: 7,
  name: "레진아트 원데이",
  description: "레진아트 기초 수업",
  imageUrl: null,
  durationMin: 120,
  price: 45000,
  targetAudience: null,
};

test("대표 도메인으로 canonical과 공유 메타데이터를 만든다", () => {
  const meta = buildSeoMeta({
    title: "레진아트 클래스 | 해피갤러리",
    description: "충주 레진아트 클래스 안내",
    pathname: "/classes/7",
    image: "/images/class.jpg",
  });

  assert.deepEqual(
    meta.find((descriptor) => descriptor.tagName === "link"),
    {
      tagName: "link",
      rel: "canonical",
      href: "https://happy-gallery.com/classes/7",
    },
  );
  assert.deepEqual(
    meta.find((descriptor) => descriptor.property === "og:image"),
    { property: "og:image", content: "https://happy-gallery.com/images/class.jpg" },
  );
  assert.deepEqual(
    meta.find((descriptor) => descriptor.name === "twitter:card"),
    { name: "twitter:card", content: "summary_large_image" },
  );
});

test("외부 이미지 주소는 그대로 사용하고 설명의 공백과 길이를 정리한다", () => {
  assert.equal(
    absoluteSiteUrl("https://cdn.example.com/product.jpg"),
    "https://cdn.example.com/product.jpg",
  );
  assert.equal(seoDescription("  여러\n줄의   설명  ", "대체 설명"), "여러 줄의 설명");
  assert.equal(seoDescription("가".repeat(200), "대체 설명").length, 160);
});

test("홈 WebSite 구조화 데이터는 WebPage가 참조하는 안정적인 식별자를 제공한다", () => {
  assert.deepEqual(buildWebSiteJsonLd("공방 소개"), {
    "@context": "https://schema.org",
    "@type": "WebSite",
    "@id": "https://happy-gallery.com/#website",
    url: "https://happy-gallery.com/",
    name: "해피갤러리",
    description: "공방 소개",
    publisher: { "@id": "https://happy-gallery.com/#organization" },
  });
});

test("과거 정책 문서는 색인하지 않되 링크 추적은 허용하고, 없는 문서는 대표 URL을 선언하지 않는다", () => {
  const historyMeta = buildSeoMeta({
    title: "이전 이용약관 | 해피갤러리",
    description: "이전 이용약관",
    pathname: "/terms/2025-01-01",
    image: "/images/policy.jpg",
    indexable: false,
    followLinks: true,
  });
  const missingMeta = buildSeoMeta({
    title: "문서를 찾을 수 없습니다 | 해피갤러리",
    description: "문서를 찾을 수 없습니다.",
    pathname: "/terms/missing",
    image: "/images/policy.jpg",
    indexable: false,
  });

  assert.deepEqual(
    historyMeta.find((descriptor) => descriptor.name === "robots"),
    { name: "robots", content: "noindex,follow" },
  );
  assert.deepEqual(
    missingMeta.find((descriptor) => descriptor.name === "robots"),
    { name: "robots", content: "noindex,nofollow" },
  );
  assert.deepEqual(
    historyMeta.find((descriptor) => descriptor.rel === "canonical"),
    { tagName: "link", rel: "canonical", href: "https://happy-gallery.com/terms/2025-01-01" },
  );
  assert.equal(
    missingMeta.some((descriptor) => descriptor.rel === "canonical" || descriptor.property === "og:url"),
    false,
  );
});

test("공지 게시 시각은 DB 생성 시각 기준인 UTC offset을 붙여 내보낸다", () => {
  const article = buildNoticeArticleJsonLd(notice, "/notices/3", "/images/notice.jpg");
  const offsetArticle = buildNoticeArticleJsonLd(
    { ...notice, createdAt: "2026-10-04T08:30:00+09:00" },
    "/notices/3",
    "/images/notice.jpg",
  );

  assert.equal(article.datePublished, "2026-10-03T23:30:00Z");
  assert.equal(Date.parse(article.datePublished), Date.parse("2026-10-04T08:30:00+09:00"));
  assert.equal(offsetArticle.datePublished, "2026-10-04T08:30:00+09:00");
});

test("상세 구조화 데이터의 조직 참조는 같은 식별자와 이름을 함께 제공한다", () => {
  const organization = {
    "@type": "Organization",
    "@id": "https://happy-gallery.com/#organization",
    name: "해피갤러리",
    url: "https://happy-gallery.com/",
  };
  const article = buildNoticeArticleJsonLd(notice, "/notices/3", "/images/notice.jpg");

  assert.deepEqual(buildProductJsonLd(product, "/products/5", "대체 설명").brand, organization);
  assert.deepEqual(buildCourseJsonLd(bookingClass, "/classes/7", "대체 설명").provider, organization);
  assert.deepEqual(article.author, organization);
  assert.deepEqual(article.publisher, organization);
});

test("클래스 가격 정보는 noindex 예약 화면이 아닌 색인 대상 상세를 가리킨다", () => {
  const course = buildCourseJsonLd(bookingClass, "/classes/7", "대체 설명");

  assert.equal(course.offers.url, "https://happy-gallery.com/classes/7");
});
