import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  listMyRestockAlerts, registerMyRestockAlert, cancelMyRestockAlert,
  type RestockAlertRequest,
} from "@/generated/api/customerStore";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import {
  requireCurrentCustomerSession, runForCurrentCustomer, runForCustomerSession,
  type CustomerSessionSnapshot,
} from "@/shared/api";

const restockAlertsKey = ["me", "restock-alerts"] as const;
type RestockAlertChange = { customerSession: CustomerSessionSnapshot } & (
  | { action: "register"; request: RestockAlertRequest }
  | { action: "cancel"; id: number }
);

export function useRestockAlerts() {
  const { isAuthenticated } = useCustomerAuth();
  const client = useQueryClient();
  const query = useQuery({
    queryKey: restockAlertsKey,
    queryFn: ({ signal }) => runForCurrentCustomer(() => listMyRestockAlerts({ signal })),
    enabled: isAuthenticated,
  });
  const mutation = useMutation({
    mutationFn: (change: RestockAlertChange) => runForCustomerSession(change.customerSession, async () => {
      if (change.action === "register") await registerMyRestockAlert(change.request);
      else await cancelMyRestockAlert(change.id);
      requireCurrentCustomerSession(change.customerSession);
      // 변경 응답에는 최신 상태가 없으므로 조회 오류는 변경 오류와 따로 표시한다.
      await client.invalidateQueries({ queryKey: restockAlertsKey });
      requireCurrentCustomerSession(change.customerSession);
    }),
  });
  return { query, mutation };
}
