import { useEffect, useRef, useState } from "react";
import { Alert, Container } from "react-bootstrap";
import { useNavigate, useSearchParams } from "react-router-dom";
import { resolveSafeReturnTo } from "@/features/customer-auth/navigation";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { getUserMessage } from "@/shared/lib";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import { LoadingSpinner } from "@/shared/ui";

export function SocialCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { refresh } = useCustomerAuth();
  const [error, setError] = useState("");
  const handled = useRef(false);

  useEffect(() => {
    if (handled.current) {
      return;
    }
    handled.current = true;

    const errorCode = searchParams.get("error");
    if (errorCode) {
      sessionStorage.removeItem(SESSION_KEYS.socialLoginReturnTo);
      setError(getUserMessage(errorCode) ?? "소셜 로그인에 실패했습니다. 다시 시도해 주세요.");
      return;
    }

    void (async () => {
      await refresh();

      const returnTo = resolveSafeReturnTo(sessionStorage.getItem(SESSION_KEYS.socialLoginReturnTo));
      sessionStorage.removeItem(SESSION_KEYS.socialLoginReturnTo);

      if (searchParams.get("newUser") === "true") {
        navigate("/my", { replace: true, state: { phoneOnboarding: true } });
      } else {
        navigate(returnTo, { replace: true });
      }
    })();
  }, [navigate, refresh, searchParams]);

  if (error) {
    return (
      <Container className="page-container" style={{ maxWidth: 480 }}>
        <Alert variant="danger" className="mt-5">{error}</Alert>
        <a href="/login">로그인 페이지로 돌아가기</a>
      </Container>
    );
  }

  return (
    <Container className="page-container d-flex justify-content-center align-items-center" style={{ minHeight: 300 }}>
      <LoadingSpinner />
    </Container>
  );
}
