import { Link } from "react-router-dom";

interface Props {
  guestPhone?: string;
  guestName?: string;
  helperText: string;
  primaryTo?: string;
  primaryLabel?: string;
}

function buildClaimSignupLink(guestPhone?: string, guestName?: string) {
  const search = new URLSearchParams({
    claim: "1",
    redirect: "/my?claim=1",
  });
  if (guestPhone) {
    search.set("phone", guestPhone);
  }
  if (guestName) {
    search.set("name", guestName);
  }
  return `/signup?${search.toString()}`;
}

function buildClaimLoginLink() {
  const search = new URLSearchParams({
    claim: "1",
    redirect: "/my?claim=1",
  });
  return `/login?${search.toString()}`;
}

export function GuestClaimSuccessActions({
  guestPhone,
  guestName,
  helperText,
  primaryTo,
  primaryLabel,
}: Props) {
  return (
    <>
      <div className="d-flex flex-wrap gap-2 mb-2">
        {primaryTo && primaryLabel && (
          <Link to={primaryTo} className="btn btn-outline-dark btn-sm">
            {primaryLabel}
          </Link>
        )}
        <Link to={buildClaimSignupLink(guestPhone, guestName)} className="btn btn-primary btn-sm">
          회원가입하고 내 정보로 가져오기
        </Link>
      </div>
      <div className="small">
        <div className="mb-1">
          <Link to={buildClaimLoginLink()} className="text-decoration-none">
            이미 계정이 있나요? 로그인하고 가져오기
          </Link>
        </div>
        <small className="text-muted-soft">{helperText}</small>
      </div>
    </>
  );
}
