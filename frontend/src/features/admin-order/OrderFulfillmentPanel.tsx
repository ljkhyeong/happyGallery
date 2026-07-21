import { Alert } from "react-bootstrap";
import { fetchOrderFulfillment } from "./api";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDate } from "@/shared/lib";

interface Props {
  orderId: number;
  adminKey: string;
  onAuthError: () => void;
}

export function OrderFulfillmentPanel({ orderId, adminKey, onAuthError }: Props) {
  const { data, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey: ["admin", "orders", orderId, "fulfillment"],
    queryFn: () => fetchOrderFulfillment(adminKey, orderId),
  });

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorAlert error={error} />;
  if (!data) return null;

  if (data.type === "PICKUP") {
    return (
      <Alert variant="light" className="border">
        <strong>매장 픽업</strong>
        {data.pickupDeadlineAt && <div>픽업 마감: {data.pickupDeadlineAt}</div>}
      </Alert>
    );
  }

  const address = data.shippingAddress;
  return (
    <Alert variant="light" className="border">
      <strong>택배 배송</strong>
      {address && (
        <div className="mt-2">
          <div>{address.recipientName} · {address.phone}</div>
          <div>[{address.postalCode}] {address.addressLine1}</div>
          {address.addressLine2 && <div>{address.addressLine2}</div>}
        </div>
      )}
      {data.expectedShipDate && <div className="mt-2">예상 출고일: {formatDate(data.expectedShipDate)}</div>}
      {data.carrier && <div>택배사: {data.carrier}</div>}
      {data.trackingNumber && <div>운송장 번호: {data.trackingNumber}</div>}
    </Alert>
  );
}
