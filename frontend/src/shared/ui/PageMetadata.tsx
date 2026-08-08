import { useEffect } from "react";
import { useLocation } from "react-router";

interface Metadata {
  title: string;
  description: string;
  indexable: boolean;
}

const DEFAULT_DESCRIPTION =
  "충주 해피갤러리의 공예 원데이클래스, 정규 과정, 단체수업과 공방 작품을 만나보세요.";

const PUBLIC_ROUTES: Array<[RegExp, Omit<Metadata, "indexable">]> = [
  [/^\/$/, {
    title: "해피갤러리 | 충주 공예 클래스와 핸드메이드 공방",
    description: DEFAULT_DESCRIPTION,
  }],
  [/^\/classes$/, {
    title: "충주 공예 원데이클래스 | 해피갤러리",
    description: "가죽공예, 레진아트, 위빙 등 해피갤러리 공예 수업의 가격과 예약 가능 시간을 확인하세요.",
  }],
  [/^\/group-classes$/, {
    title: "충주 단체·기관 공예수업 | 해피갤러리",
    description: "학교, 기관, 모임을 위한 충주 해피갤러리 출장·단체 공예수업을 문의하세요.",
  }],
  [/^\/products$/, {
    title: "수공예 작품 | 해피갤러리",
    description: "해피갤러리가 직접 만든 수공예 작품과 주문 제작 상품을 둘러보세요.",
  }],
  [/^\/products\/\d+$/, {
    title: "공방 작품 상세 | 해피갤러리",
    description: "해피갤러리 수공예 작품의 상세 정보와 주문 방법을 확인하세요.",
  }],
  [/^\/notices\/\d+$/, {
    title: "해피갤러리 소식",
    description: "해피갤러리의 클래스와 공방 운영 소식을 확인하세요.",
  }],
  [/^\/events$/, {
    title: "이벤트 | 해피갤러리",
    description: "해피갤러리에서 진행 중이거나 곧 시작할 이벤트를 확인하세요.",
  }],
  [/^\/events\/\d+$/, {
    title: "이벤트 상세 | 해피갤러리",
    description: "해피갤러리 이벤트 일정과 참여 내용을 확인하세요.",
  }],
  [/^\/terms(?:\/[^/]+)?$/, { title: "이용약관 | 해피갤러리", description: "해피갤러리 서비스 이용약관입니다." }],
  [/^\/privacy(?:\/[^/]+)?$/, { title: "개인정보처리방침 | 해피갤러리", description: "해피갤러리 개인정보처리방침입니다." }],
  [/^\/business-info$/, { title: "사업자 정보 | 해피갤러리", description: "해피갤러리 사업자와 공방 정보를 확인하세요." }],
];

function resolveMetadata(pathname: string): Metadata {
  const route = PUBLIC_ROUTES.find(([pattern]) => pattern.test(pathname));
  if (route) {
    return { ...route[1], indexable: true };
  }
  return {
    title: "해피갤러리",
    description: DEFAULT_DESCRIPTION,
    indexable: false,
  };
}

function updateMeta(selector: string, attribute: "name" | "property", key: string, content: string) {
  let element = document.head.querySelector<HTMLMetaElement>(selector);
  if (!element) {
    element = document.createElement("meta");
    element.setAttribute(attribute, key);
    document.head.appendChild(element);
  }
  element.content = content;
}

export function PageMetadata() {
  const { pathname } = useLocation();

  useEffect(() => {
    const metadata = resolveMetadata(pathname);
    document.title = metadata.title;
    updateMeta('meta[name="description"]', "name", "description", metadata.description);
    updateMeta('meta[name="robots"]', "name", "robots", metadata.indexable ? "index,follow" : "noindex,nofollow");
    updateMeta('meta[property="og:title"]', "property", "og:title", metadata.title);
    updateMeta('meta[property="og:description"]', "property", "og:description", metadata.description);
    updateMeta('meta[name="twitter:title"]', "name", "twitter:title", metadata.title);
    updateMeta('meta[name="twitter:description"]', "name", "twitter:description", metadata.description);
  }, [pathname]);

  return null;
}
