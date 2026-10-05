import { useEffect, useRef, useState, type ReactNode } from "react";
import { Badge, Container } from "react-bootstrap";
import { Navigate, NavLink, useLocation, useNavigate } from "react-router";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { CustomerSessionChangedError } from "@/shared/api";
import { LoadingSpinner, useToast } from "@/shared/ui";
import { MyAuthGateCard } from "./MyAuthGateCard";
import { findMyNavItem, MY_HOME_PATH, MY_NAV_GROUPS } from "./myNavigation";

/** 예전 `/my#...` 링크는 별도 관리 화면으로 옮긴다. */
const MOVED_SECTIONS: Record<string, string> = {
  "#my-favorites": "/my/favorites",
  "#my-default-shipping-address": "/my/shipping-address",
  "#my-restock-alerts": "/my/restock-alerts",
  "#my-vacancy-alerts": "/my/vacancy-alerts",
  "#my-group-inquiries": "/my/group-inquiries",
};

/**
 * 내 정보 화면 공통 틀. 로그인 확인·안내와 메뉴를 한곳에서 처리한다.
 * 데스크톱은 왼쪽 메뉴, 모바일은 홈에서 묶음 목록·하위 화면에서 가로 메뉴로 보여 준다.
 */
export function MyShell({ children }: { children: ReactNode }) {
  const { user, isAuthenticated, isLoading, sessionVersion } = useCustomerAuth();
  const { pathname, hash } = useLocation();
  const isHome = pathname === MY_HOME_PATH;
  const movedSection = isHome ? MOVED_SECTIONS[hash] : undefined;

  if (movedSection) return <Navigate to={movedSection} replace />;

  if (isLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }

  if (!isAuthenticated || !user) {
    const item = findMyNavItem(pathname);
    return (
      <Container className="page-container my-gate-page">
        <Badge bg="light" text="dark" className="mb-3">내 정보</Badge>
        {isHome || !item ? (
          <MyAuthGateCard
            title="로그인하고 주문, 예약, 이용권을 한 곳에서 관리하세요"
            description="로그인하면 추가 휴대폰 인증 없이 주문·예약·이용권과 쿠폰·적립금을 확인할 수 있습니다."
            showGuestLinks
          />
        ) : (
          <MyAuthGateCard title="로그인이 필요합니다" description={item.gateDescription} />
        )}
      </Container>
    );
  }

  return (
    <Container className="page-container">
      <div className={`my-shell${isHome ? " is-home" : ""}`}>
        <aside className="my-shell-side">
          <p className="my-shell-hello">{user.name}님</p>
          <MyShellNav pathname={pathname} />
          <MyLogoutButton />
        </aside>
        <div className="my-shell-main" key={sessionVersion}>{children}</div>
      </div>
    </Container>
  );
}

function MyShellNav({ pathname }: { pathname: string }) {
  const navRef = useRef<HTMLElement>(null);

  // 모바일 가로 메뉴에서 현재 화면 메뉴가 보이도록 가운데로 옮긴다. 세로 스크롤은 건드리지 않는다.
  useEffect(() => {
    const nav = navRef.current;
    const active = nav?.querySelector<HTMLElement>("a.active");
    if (!nav || !active || nav.scrollWidth <= nav.clientWidth) return;
    nav.scrollLeft = active.offsetLeft - (nav.clientWidth - active.offsetWidth) / 2;
  }, [pathname]);

  return (
    <nav ref={navRef} className="my-shell-nav" aria-label="내 정보 관리 메뉴">
      <NavLink to={MY_HOME_PATH} end className="my-shell-nav-home">내 정보 홈</NavLink>
      {MY_NAV_GROUPS.map((group) => (
        <div key={group.title} className="my-shell-nav-group">
          <p>{group.title}</p>
          <ul>
            {group.items.map((item) => (
              <li key={item.to}>
                <NavLink to={item.to}>{item.label}</NavLink>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </nav>
  );
}

function MyLogoutButton() {
  const { logout } = useCustomerAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [loggingOut, setLoggingOut] = useState(false);

  const handleLogout = async () => {
    setLoggingOut(true);
    try {
      await logout();
      navigate("/");
    } catch (error) {
      if (error instanceof CustomerSessionChangedError) return;
      toast.show("로그아웃 여부를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.", "danger");
    } finally {
      setLoggingOut(false);
    }
  };

  return (
    <button type="button" className="my-shell-logout" onClick={() => void handleLogout()} disabled={loggingOut}>
      {loggingOut ? "로그아웃 중..." : "로그아웃"}
    </button>
  );
}
