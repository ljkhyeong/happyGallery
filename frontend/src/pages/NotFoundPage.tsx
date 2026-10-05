import { useLocation } from "react-router";
import { StatusPage } from "@/shared/ui/StatusPage";
import { LinkButton } from "@/shared/ui/LinkButton";

interface NotFoundCopy {
  title: string;
  description: string;
  action?: { to: string; label: string };
}

/** 주소 첫 구간별로 무엇을 찾지 못했는지와 돌아갈 목록을 알려 준다. 주소 값으로 객체 속성을 읽지 않도록 Map을 쓴다. */
const NOT_FOUND_BY_SECTION = new Map<string, NotFoundCopy>(Object.entries({
  products: {
    title: "작품을 찾을 수 없습니다",
    description: "판매가 끝났거나 주소가 바뀌었을 수 있습니다. 지금 판매 중인 작품을 확인해 보세요.",
    action: { to: "/products", label: "작품 목록 보기" },
  },
  classes: {
    title: "클래스를 찾을 수 없습니다",
    description: "운영이 끝났거나 주소가 바뀌었을 수 있습니다. 지금 예약할 수 있는 클래스를 확인해 보세요.",
    action: { to: "/classes", label: "클래스 목록 보기" },
  },
  events: {
    title: "이벤트를 찾을 수 없습니다",
    description: "기간이 끝났거나 공개가 중지된 이벤트일 수 있습니다.",
    action: { to: "/events", label: "이벤트 목록 보기" },
  },
  notices: {
    title: "공지를 찾을 수 없습니다",
    description: "삭제됐거나 주소가 바뀐 공지일 수 있습니다. 최근 공지는 홈에서 확인할 수 있습니다.",
  },
  my: {
    title: "내역을 찾을 수 없습니다",
    description: "주소를 다시 확인하거나 내 정보에서 주문·예약 내역을 찾아 주세요.",
    action: { to: "/my", label: "내 정보로 가기" },
  },
  guest: {
    title: "내역을 찾을 수 없습니다",
    description: "비회원 조회에서 주문·예약 번호로 다시 찾아 주세요.",
    action: { to: "/guest", label: "비회원 조회" },
  },
}));

const DEFAULT_NOT_FOUND: NotFoundCopy = {
  title: "페이지를 찾을 수 없습니다",
  description: "주소가 바뀌었거나 없는 페이지입니다. 아래에서 원하는 화면을 찾아 주세요.",
};

export function NotFoundPage() {
  const { pathname } = useLocation();
  const copy = NOT_FOUND_BY_SECTION.get(pathname.split("/")[1] ?? "") ?? DEFAULT_NOT_FOUND;
  return (
    <StatusPage
      kicker="404"
      title={copy.title}
      description={copy.description}
      actions={(
        <>
          {copy.action && <LinkButton to={copy.action.to}>{copy.action.label}</LinkButton>}
          <LinkButton to="/" variant={copy.action ? "outline-secondary" : "primary"}>홈으로</LinkButton>
        </>
      )}
    />
  );
}
