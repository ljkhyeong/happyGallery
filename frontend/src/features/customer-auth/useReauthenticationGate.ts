import { useEffect, useState } from "react";
import { isReauthenticationRequired } from "@/features/customer-auth/customerStepUp";

interface Options {
  show: boolean;
  /** 모달이 열릴 때 되돌릴 본인 확인 상태. 외부 인증을 마치고 돌아온 경우 true다. */
  initiallyReauthenticated: boolean;
}

/**
 * 민감한 변경 모달의 본인 확인(step-up) 게이트.
 * 요청이 REAUTHENTICATION_REQUIRED로 실패하면 CustomerStepUpPrompt로 전환하고, 확인을 마치면 원래 폼으로 돌아간다.
 */
export function useReauthenticationGate({ show, initiallyReauthenticated }: Options) {
  const [reauthenticated, setReauthenticated] = useState(initiallyReauthenticated);
  const [stepUpBusy, setStepUpBusy] = useState(false);

  useEffect(() => {
    if (show) setReauthenticated(initiallyReauthenticated);
  }, [show, initiallyReauthenticated]);

  return {
    reauthenticated,
    /** CustomerStepUpPrompt가 비밀번호·소셜 확인을 진행하는 동안 true. onBusyChange에 setter를 그대로 넘긴다. */
    stepUpBusy,
    setStepUpBusy,
    /** mutation onError에 넘긴다. 본인 확인을 다시 요구한 오류만 프롬프트로 전환한다. */
    requireStepUpOnError: (error: unknown) => {
      if (isReauthenticationRequired(error)) setReauthenticated(false);
    },
    markVerified: () => setReauthenticated(true),
    reset: () => setReauthenticated(initiallyReauthenticated),
  };
}
