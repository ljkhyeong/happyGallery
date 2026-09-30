import { LinkButton } from "@/shared/ui/LinkButton";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { trackGuestMemberCta } from "@/features/monitoring/api";

export function GuestMemberGuide({ source }: { source: string }) {
  const options = { redirectTo: "/my?claim=1", claim: true };
  return (
    <aside className="guest-member-guide" aria-label="회원 이용 안내">
      <h2>회원이신가요?</h2>
      <p>로그인하면 내 정보에서 주문과 예약을 한 번에 관리할 수 있습니다.
        비회원 내역도 같은 휴대폰 번호로 가져올 수 있어요.</p>
      <div className="d-flex flex-wrap gap-2">
        <LinkButton to={buildAuthPageHref("/login", options)} variant="outline-dark" size="sm"
          onClick={() => trackGuestMemberCta(source, "login")}>
          로그인하고 가져오기
        </LinkButton>
        <LinkButton to={buildAuthPageHref("/signup", options)} variant="link" size="sm"
          onClick={() => trackGuestMemberCta(source, "signup")}>
          회원가입
        </LinkButton>
      </div>
    </aside>
  );
}
