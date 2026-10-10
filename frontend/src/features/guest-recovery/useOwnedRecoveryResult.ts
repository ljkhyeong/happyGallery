import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import {
  captureCustomerSession,
  runForCurrentCustomer,
  type CustomerSessionSnapshot,
} from "@/shared/api";

export interface OwnedRecoveryView<TSession> {
  customerSession: CustomerSessionSnapshot;
  storage: TSession;
}

interface Options<TResult, TSession> {
  load: () => TSession | null;
  save: (result: TResult, customerSession: CustomerSessionSnapshot) => TSession | null;
  clear: (expected?: TSession) => void;
  request: (phone: string, code: string) => Promise<TResult>;
  /** 저장한 결과를 화면에 보이기 전에 할 일(이전 결과로 받은 조회 캐시 정리 등). */
  beforeShow?: () => Promise<void>;
  /** 결과를 지운 뒤 화면 상태를 비우기 전에 할 일. */
  afterClear?: () => void;
}

/**
 * 휴대폰 인증으로 받은 비회원 복구 결과를 현재 회원 세션 소유로 저장하고 화면에 보인다.
 * 세션이 바뀌어 저장 뒤 작업이 실패하면 이 흐름이 저장한 값만 지운다.
 */
export function useOwnedRecoveryResult<TResult, TSession>({
  load,
  save,
  clear,
  request,
  beforeShow,
  afterClear,
}: Options<TResult, TSession>) {
  const [view, setView] = useState<OwnedRecoveryView<TSession> | null>(() => {
    const storage = load();
    return storage ? { customerSession: captureCustomerSession(), storage } : null;
  });
  const mutation = useMutation({
    mutationFn: async ({ phone, code }: { phone: string; code: string }) => {
      let storedRecovery: TSession | null = null;
      try {
        return await runForCurrentCustomer(
          () => request(phone, code),
          async (result, requireCurrent) => {
            requireCurrent();
            const customerSession = captureCustomerSession();
            storedRecovery = save(result, customerSession);
            requireCurrent();
            if (storedRecovery) {
              if (beforeShow) {
                await beforeShow();
                requireCurrent();
              }
              setView({ customerSession, storage: storedRecovery });
            }
            return result;
          },
        );
      } catch (error) {
        if (storedRecovery) {
          const expectedRecovery = storedRecovery;
          clear(expectedRecovery);
          setView((current) => current?.storage === expectedRecovery ? null : current);
        }
        throw error;
      }
    },
  });

  function reset() {
    mutation.reset();
    clear(view?.storage);
    afterClear?.();
    setView(null);
  }

  return { view, mutation, reset };
}
