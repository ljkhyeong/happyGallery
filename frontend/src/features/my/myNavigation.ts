/**
 * 내 정보 메뉴와 각 화면 제목의 단일 원본.
 * 메뉴 이름과 화면 h1을 같게 두고, 로그인 안내 문구도 여기서 만든다.
 */
export interface MyNavItem {
  to: string;
  label: string;
  /** 로그인하지 않은 고객에게 보이는 안내 */
  gateDescription: string;
}

export interface MyNavGroup {
  title: string;
  items: MyNavItem[];
}

export const MY_HOME_PATH = "/my";

export const MY_NAV_GROUPS: MyNavGroup[] = [
  {
    title: "예약·주문",
    items: [
      { to: "/my/bookings", label: "예약 내역", gateDescription: "로그인하면 내 예약을 확인할 수 있습니다." },
      { to: "/my/orders", label: "주문 내역", gateDescription: "로그인하면 내 주문을 확인할 수 있습니다." },
      { to: "/my/passes", label: "이용권", gateDescription: "로그인하면 내 이용권을 확인할 수 있습니다." },
    ],
  },
  {
    title: "혜택",
    items: [
      { to: "/my/benefits", label: "쿠폰·적립금", gateDescription: "로그인하면 내 쿠폰과 적립금을 확인할 수 있습니다." },
      { to: "/my/favorites", label: "내 찜", gateDescription: "로그인하면 찜한 상품과 클래스를 확인할 수 있습니다." },
    ],
  },
  {
    title: "문의·후기",
    items: [
      { to: "/my/inquiries", label: "1:1 문의", gateDescription: "로그인하면 1:1 문의를 작성하고 답변을 확인할 수 있습니다." },
      { to: "/my/reviews", label: "내 후기", gateDescription: "내가 작성한 상품·클래스 후기는 로그인 후 확인하고 수정할 수 있습니다." },
      { to: "/my/group-inquiries", label: "단체 수업 문의", gateDescription: "로그인하면 단체 수업 문의 상태를 확인할 수 있습니다." },
      { to: "/my/notifications", label: "알림함", gateDescription: "로그인하면 받은 알림을 확인할 수 있습니다." },
    ],
  },
  {
    title: "알림 신청·설정",
    items: [
      { to: "/my/vacancy-alerts", label: "빈자리 알림", gateDescription: "로그인하면 신청한 빈자리 알림을 확인할 수 있습니다." },
      { to: "/my/restock-alerts", label: "재입고 알림", gateDescription: "로그인하면 신청한 재입고 알림을 확인할 수 있습니다." },
      { to: "/my/shipping-address", label: "기본 배송지", gateDescription: "로그인하면 기본 배송지를 저장하고 수정할 수 있습니다." },
    ],
  },
];

const ITEMS = MY_NAV_GROUPS.flatMap((group) => group.items);

/** 상세 화면(`/my/bookings/12`)은 상위 목록 메뉴를 활성으로 본다. */
export function findMyNavItem(pathname: string): MyNavItem | undefined {
  return ITEMS.find((item) => pathname === item.to || pathname.startsWith(`${item.to}/`));
}

export function myNavLabel(path: string): string {
  const item = ITEMS.find((candidate) => candidate.to === path);
  if (!item) throw new Error(`내 정보 메뉴에 없는 경로입니다: ${path}`);
  return item.label;
}
