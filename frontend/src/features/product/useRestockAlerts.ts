import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  listMyRestockAlerts, registerMyRestockAlert, cancelMyRestockAlert,
  type RestockAlertRequest,
} from "@/generated/api/customerStore";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import {
  queryKeys, requireCurrentCustomerSession, runForCurrentCustomer, runForCustomerSession,
  type CustomerSessionSnapshot,
} from "@/shared/api";
import { useToast } from "@/shared/ui";

type RestockAlertChange = { customerSession: CustomerSessionSnapshot } & (
  | { action: "register"; request: RestockAlertRequest }
  | { action: "cancel"; id: number }
);

export function useRestockAlerts() {
  const { isAuthenticated } = useCustomerAuth();
  const client = useQueryClient();
  const toast = useToast();
  const query = useQuery({
    queryKey: queryKeys.member.restockAlerts,
    queryFn: ({ signal }) => runForCurrentCustomer(() => listMyRestockAlerts({ signal })),
    enabled: isAuthenticated,
  });
  const mutation = useMutation({
    mutationFn: (change: RestockAlertChange) => runForCustomerSession(change.customerSession, async () => {
      if (change.action === "register") await registerMyRestockAlert(change.request);
      else await cancelMyRestockAlert(change.id);
      requireCurrentCustomerSession(change.customerSession);
      // 변경 응답에는 최신 상태가 없으므로 조회 오류는 변경 오류와 따로 표시한다.
      await client.invalidateQueries({ queryKey: queryKeys.member.restockAlerts });
      requireCurrentCustomerSession(change.customerSession);
      // 버튼 이름만 바뀌면 처리 결과를 놓치므로 빈자리 알림처럼 토스트로 알린다(화면 낭독기에도 읽힌다).
      if (change.action === "register") toast.show("재입고 알림을 신청했습니다. 입고되면 한 번 알려 드립니다.");
      else toast.show("재입고 알림을 해지했습니다.", "info");
    }),
  });
  return { query, mutation };
}
