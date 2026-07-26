import { Alert } from "react-bootstrap";
import { fetchOrderFulfillment } from "./api";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDate } from "@/shared/lib";
import type { AdminOrderFulfillmentResponse } from "@/shared/types";
import { ShipmentTrackingActions } from "@/features/order/ShipmentTrackingActions";

interface Props {
  orderId: number;
  adminKey: string;
  onAuthError: () => void;
}

export function OrderFulfillmentDetails({
  fulfillment,
}: {
  fulfillment: AdminOrderFulfillmentResponse;
}) {
  if (fulfillment.type === "PICKUP") {
    return (
      <Alert variant="light" className="border">
        <strong>매장 픽업</strong>
        {fulfillment.pickupDeadlineAt && <div>픽업 마감: {fulfillment.pickupDeadlineAt}</div>}
      </Alert>
    );
  }

  const address = fulfillment.shippingAddress;
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
      {fulfillment.expectedShipDate && (
        <div className="mt-2">예상 출고일: {formatDate(fulfillment.expectedShipDate)}</div>
      )}
      {fulfillment.carrier && <div>택배사: {fulfillment.carrier}</div>}
      {fulfillment.trackingNumber && (
        <>
          <div>운송장 번호: {fulfillment.trackingNumber}</div>
          {fulfillment.carrier && (
            <ShipmentTrackingActions
              carrier={fulfillment.carrier}
              trackingNumber={fulfillment.trackingNumber}
            />
          )}
        </>
      )}
    </Alert>
  );
}

export function OrderFulfillmentPanel({ orderId, adminKey, onAuthError }: Props) {
  const { data, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey: ["admin", "orders", orderId, "fulfillment"],
    queryFn: () => fetchOrderFulfillment(adminKey, orderId),
  });

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorAlert error={error} />;
  if (!data) return null;

  return <OrderFulfillmentDetails fulfillment={data} />;
}
