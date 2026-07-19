import { useQuery } from "@tanstack/react-query";
import { Alert } from "react-bootstrap";
import { fetchOrderFulfillment } from "./api";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";

interface Props {
  orderId: number;
  adminKey: string;
}

export function OrderFulfillmentPanel({ orderId, adminKey }: Props) {
  const { data, isLoading, error } = useQuery({
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
    </Alert>
  );
}
