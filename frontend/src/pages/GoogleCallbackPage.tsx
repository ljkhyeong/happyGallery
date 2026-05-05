import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Container, Alert } from "react-bootstrap";
import { LoadingSpinner } from "@/shared/ui";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";

export function GoogleCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { socialLogin, refresh } = useCustomerAuth();
  const [error, setError] = useState("");

  useEffect(() => {
    const code = searchParams.get("code");
    const returnedState = searchParams.get("state");
    const savedState = sessionStorage.getItem(SESSION_KEYS.googleOauthState);

    if (!code) {
      setError("인가 코드가 없습니다. 다시 시도해주세요.");
      return;
    }

    if (savedState && returnedState !== savedState) {
      setError("잘못된 요청입니다. 다시 시도해주세요.");
      return;
    }

    const redirectUri = window.location.origin + "/auth/callback/google";

    socialLogin(code, redirectUri).then(async (result) => {
      sessionStorage.removeItem(SESSION_KEYS.googleOauthState);

      if (!result.ok) {
        setError("소셜 로그인에 실패했습니다. 다시 시도해주세요.");
        return;
      }

      await refresh();

      const returnTo = sessionStorage.getItem(SESSION_KEYS.socialLoginReturnTo) ?? "/";
      sessionStorage.removeItem(SESSION_KEYS.socialLoginReturnTo);

      if (result.newUser) {
        navigate("/my", { state: { phoneOnboarding: true } });
      } else {
        navigate(returnTo);
      }
    });
  }, [searchParams, socialLogin, refresh, navigate]);

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
