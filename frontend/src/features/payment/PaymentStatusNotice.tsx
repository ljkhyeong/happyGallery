import { Alert } from "react-bootstrap";
import { formatKRW } from "@/shared/lib";
import type { PaymentStatusResponse } from "./types";

function paymentContextCopy(context: PaymentStatusResponse["context"]) {
  switch (context) {
    case "ORDER":
      return {
        completedHeading: "결제가 완료되었습니다",
        creationFailed: "주문을 만들지 못해",
        notCreated: "주문이 생성되지 않았으며",
        restartLocation: "상품 화면",
      };
    case "BOOKING":
      return {
        completedHeading: "결제가 완료되었습니다",
        creationFailed: "예약을 만들지 못해",
        notCreated: "예약이 생성되지 않았으며",
        restartLocation: "예약 화면",
      };
    case "PASS":
      return {
        completedHeading: "8회권 결제가 완료되었습니다",
        creationFailed: "8회권을 발급하지 못해",
        notCreated: "8회권이 발급되지 않았으며",
        restartLocation: "8회권 구매 화면",
      };
  }
}

export function PaymentStatusNotice({ status }: { status: PaymentStatusResponse }) {
  const amount = formatKRW(status.amount);
  const contextCopy = paymentContextCopy(status.context);
  switch (status.status) {
    case "READY":
    case "RETRYABLE":
      return (
        <Alert variant="warning" className="mb-0">
          <Alert.Heading className="fs-5">결제 결과를 다시 확인해 주세요</Alert.Heading>
          <p className="mb-0">{amount} 결제 결과를 아직 확정하지 못했습니다. 같은 결제로 다시 확인할 수 있습니다.</p>
        </Alert>
      );
    case "CONFIRMING":
      return (
        <Alert variant="info" className="mb-0">
          <Alert.Heading className="fs-5">결제를 확인하고 있습니다</Alert.Heading>
          <p className="mb-0">새로 결제하지 마세요. 처리 결과를 자동으로 확인하고 있습니다.</p>
        </Alert>
      );
    case "REFUNDING":
      return (
        <Alert variant="info" className="mb-0">
          <Alert.Heading className="fs-5">자동 환불을 처리하고 있습니다</Alert.Heading>
          <p className="mb-0">결제는 승인됐지만 {contextCopy.creationFailed} {amount} 전액 환불을 요청했습니다.</p>
        </Alert>
      );
    case "REFUNDED":
      return (
        <Alert variant="success" className="mb-0">
          <Alert.Heading className="fs-5">환불이 완료되었습니다</Alert.Heading>
          <p className="mb-0">{contextCopy.notCreated} {amount} 전액 환불이 완료됐습니다.</p>
        </Alert>
      );
    case "REVIEW_REQUIRED":
      return (
        <Alert variant="warning" className="mb-0">
          <Alert.Heading className="fs-5">결제사 결과를 확인하고 있습니다</Alert.Heading>
          <p className="mb-0">중복 결제를 피하기 위해 새로 결제하지 마세요. 공방에 결제 결과를 문의한 뒤 새로고침해 주세요.</p>
        </Alert>
      );
    case "SUPPORT_REQUIRED":
      return (
        <Alert variant="warning" className="mb-0">
          <Alert.Heading className="fs-5">환불 확인이 필요합니다</Alert.Heading>
          <p className="mb-0">자동 환불을 완료하지 못해 공방에서 확인 중입니다. 새로 결제하지 말고 해피갤러리로 문의해 주세요.</p>
        </Alert>
      );
    case "FAILED":
      return (
        <Alert variant="danger" className="mb-0">
          <Alert.Heading className="fs-5">결제가 승인되지 않았습니다</Alert.Heading>
          <p className="mb-0">결제가 완료되지 않았습니다. 결제 수단을 확인한 뒤 다시 시작해 주세요.</p>
        </Alert>
      );
    case "EXPIRED":
      return (
        <Alert variant="secondary" className="mb-0">
          <Alert.Heading className="fs-5">결제 준비가 종료되었습니다</Alert.Heading>
          <p className="mb-0">승인된 결제가 없습니다. {contextCopy.restartLocation}에서 다시 시작해 주세요.</p>
        </Alert>
      );
    case "COMPLETED":
      return (
        <Alert variant="success" className="mb-0">
          <Alert.Heading className="fs-5">{contextCopy.completedHeading}</Alert.Heading>
          <p className="mb-0">{amount} 결제가 정상 처리되었습니다.</p>
        </Alert>
      );
  }
}
