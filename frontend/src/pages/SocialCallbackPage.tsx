import { useEffect, useRef, useState } from "react";
import { Alert, Container } from "react-bootstrap";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import {
  buildSocialRedirectUri,
  isSocialProvider,
  SOCIAL_PROVIDER_DETAILS,
} from "@/features/customer-auth/socialAuth";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import { LoadingSpinner } from "@/shared/ui";

export function SocialCallbackPage() {
  const { provider: providerParam } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { socialLogin, refresh } = useCustomerAuth();
  const [error, setError] = useState("");
  const handled = useRef(false);

  useEffect(() => {
    if (handled.current) {
      return;
    }
    handled.current = true;

    if (!isSocialProvider(providerParam)) {
      setError("지원하지 않는 소셜 로그인 요청입니다.");
      return;
    }

    const provider = providerParam;
    const details = SOCIAL_PROVIDER_DETAILS[provider];
    const code = searchParams.get("code");
    const returnedState = searchParams.get("state");
    const savedState = sessionStorage.getItem(details.stateStorageKey);

    sessionStorage.removeItem(details.stateStorageKey);

    if (!code) {
      setError(`${details.label} 인증이 완료되지 않았습니다. 다시 시도해주세요.`);
      return;
    }

    if (!savedState || !returnedState || returnedState !== savedState) {
      setError("잘못된 소셜 로그인 요청입니다. 다시 시도해주세요.");
      return;
    }

    const redirectUri = buildSocialRedirectUri(provider);

    void socialLogin(provider, code, redirectUri, returnedState).then(async (result) => {
      if (!result.ok) {
        if (result.errorCode === "SOCIAL_ACCOUNT_LINK_REQUIRED") {
          setError("같은 이메일로 가입된 계정이 있습니다. 기존 로그인 수단을 이용해주세요.");
          return;
        }
        setError(`${details.label} 로그인에 실패했습니다. 다시 시도해주세요.`);
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
  }, [navigate, providerParam, refresh, searchParams, socialLogin]);

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
