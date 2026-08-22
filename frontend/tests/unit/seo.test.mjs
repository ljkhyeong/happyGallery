import assert from "node:assert/strict";
import test from "node:test";

import {
  absoluteSiteUrl,
  buildSeoMeta,
  buildWebSiteJsonLd,
  seoDescription,
} from "../../src/shared/seo/metadata.ts";

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

test("과거 정책 문서는 색인하지 않되 링크 추적은 허용할 수 있다", () => {
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
});
