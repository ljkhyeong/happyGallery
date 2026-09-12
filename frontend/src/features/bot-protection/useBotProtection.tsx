import { useCallback, useContext, useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { DEFAULT_SCRIPT_ID, Turnstile } from "@marsidev/react-turnstile";
import { getBotProtection } from "@/generated/api/customerAuth";
import { CspNonceContext } from "@/shared/seo/CspJsonLd";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";

type Action = "phone_verification" | "group_inquiry";

function BotChallenge({ siteKey, action, onToken, onRetry }: {
  siteKey: string;
  action: Action;
  onToken: (token: string | undefined) => void;
  onRetry: () => void;
}) {
  const nonce = useContext(CspNonceContext);
  const loaded = useRef(false);
  const active = useRef(true);
  const [error, setError] = useState<Error>();
  const fail = () => {
    if (!active.current) return;
    onToken(undefined);
    setError(new Error("자동 입력 방지를 확인하지 못했습니다. 다시 확인해 주세요."));
  };

  useEffect(() => {
    active.current = true;
    const timer = window.setTimeout(() => {
      if (!loaded.current) setError(new Error("자동 입력 방지를 불러오지 못했습니다. 다시 시도해 주세요."));
    }, 15_000);
    return () => {
      active.current = false;
      window.clearTimeout(timer);
      onToken(undefined);
    };
  }, [onToken]);

  return (
    <div className="my-3" aria-label="자동 입력 방지">
      <Turnstile siteKey={siteKey} scriptOptions={{ nonce }}
        options={{ action, language: "ko", size: "flexible", retry: "never", responseField: false }}
        onWidgetLoad={() => { loaded.current = true; }}
        onSuccess={(token) => {
          if (!active.current) return;
          setError(undefined);
          onToken(token);
        }}
        onExpire={() => { if (active.current) onToken(undefined); }}
        onError={fail} onTimeout={fail} onUnsupported={fail} />
      <ErrorAlert error={error} retryLabel="자동 입력 방지 다시 확인" onRetry={() => {
        if (!loaded.current) document.getElementById(DEFAULT_SCRIPT_ID)?.remove();
        onRetry();
      }} />
    </div>
  );
}

export function useBotProtection(action: Action) {
  const configuration = useQuery({
    queryKey: ["bot-protection"],
    queryFn: () => getBotProtection(),
    staleTime: 60_000,
    retry: false,
  });
  const [verification, setVerification] = useState<{ key: string; token: string }>();
  const [attempt, setAttempt] = useState(0);
  const reset = useCallback(() => {
    setVerification(undefined);
    setAttempt((value) => value + 1);
  }, []);
  const siteKey = configuration.data?.siteKey;
  const key = `${siteKey}:${action}:${attempt}`;
  const token = configuration.isSuccess && verification?.key === key ? verification.token : undefined;
  const setToken = useCallback((value: string | undefined) => {
    setVerification((current) => value ? { key, token: value } : current?.key === key ? undefined : current);
  }, [key]);
  return {
    token,
    ready: configuration.isSuccess && (!siteKey || Boolean(token)),
    reset,
    challenge: configuration.isPending ? <LoadingSpinner text="자동 입력 방지를 확인하는 중입니다" />
      : configuration.isError ? <ErrorAlert error={configuration.error} onRetry={() => { void configuration.refetch(); }} />
        : siteKey ? <BotChallenge key={key} siteKey={siteKey} action={action}
          onToken={setToken} onRetry={reset} /> : null,
  };
}
