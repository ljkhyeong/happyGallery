import { useState, type FormEvent } from "react";
import { Button, Form } from "react-bootstrap";
import { Link, useNavigate } from "react-router";
import { useCustomerAuth } from "./useCustomerAuth";
import { buildAuthPageHref } from "./navigation";
import { PolicyConsentFields } from "@/features/policy-consent/PolicyConsentFields";
import { usePolicyAcceptance } from "@/features/policy-consent/usePolicyAcceptance";
import { ApiError, CustomerSessionChangedError } from "@/shared/api";
import { ErrorAlert } from "@/shared/ui";
import { removeSessionValues } from "@/shared/storage/browserSessionStorage";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";

export function SocialSignupConsent({ attemptId, returnTo }: { attemptId: string; returnTo: string }) {
  const { completeSocialSignup } = useCustomerAuth();
  const consent = usePolicyAcceptance();
  const navigate = useNavigate();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const expired = error instanceof ApiError && error.code === "SOCIAL_LOGIN_FAILED";

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!consent.acceptance || pending) return;
    setPending(true);
    setError(null);
    try {
      await completeSocialSignup(attemptId, consent.acceptance);
      removeSessionValues(SESSION_KEYS.socialLoginReturnTo);
      navigate(returnTo, { replace: true });
    } catch (requestError) {
      if (requestError instanceof CustomerSessionChangedError) {
        navigate("/my", { replace: true });
        return;
      }
      setError(requestError);
      if (requestError instanceof ApiError && requestError.code === "POLICY_CONSENT_REQUIRED") {
        consent.setAccepted(false);
        void consent.policyQuery.refetch();
      }
    } finally {
      setPending(false);
    }
  }

  return (
    <section className="mt-5">
      <h1 className="h4">동의하고 가입 완료</h1>
      <p className="text-muted">소셜 계정 확인이 끝났습니다. 약관에 동의하면 가입이 완료됩니다.</p>
      <Form onSubmit={(event) => void submit(event)}>
        {expired ? <p role="alert">가입 시간이 만료되었거나 이미 처리되었습니다. 다시 로그인해 주세요.</p>
          : <ErrorAlert error={error} />}
        <PolicyConsentFields id="social-signup-consent" policy={consent.policyQuery.data}
          checked={consent.accepted} onChange={consent.setAccepted}
          isLoading={consent.policyQuery.isLoading || pending} error={consent.policyQuery.error} />
        {consent.policyQuery.isError && (
          <Button variant="link" onClick={() => void consent.policyQuery.refetch()}>약관 다시 불러오기</Button>
        )}
        <Button type="submit" className="w-100" disabled={!consent.ready || pending || expired}>
          {pending ? "가입 처리 중..." : "동의하고 시작하기"}
        </Button>
      </Form>
      <Link className="d-block mt-3" to={buildAuthPageHref("/login", { redirectTo: returnTo })}>
        {expired ? "다시 로그인하기" : "다른 계정으로 로그인"}
      </Link>
    </section>
  );
}
