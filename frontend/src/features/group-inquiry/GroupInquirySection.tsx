import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Alert } from "react-bootstrap";
import { Link } from "react-router";
import { createGuestGroupInquiry, createMyGroupInquiry, type GroupInquiryRequest } from "@/generated/api/customerStore";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { runForCurrentCustomer } from "@/shared/api";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { GroupInquiryForm } from "./GroupInquiryForm";

export function GroupInquirySection() {
  const { sessionVersion } = useCustomerAuth();
  return <GroupInquirySectionContent key={sessionVersion} />;
}

function GroupInquirySectionContent() {
  const { user, isAuthenticated, isLoading, status, error, refresh } = useCustomerAuth();
  const client = useQueryClient();
  const [receiptId, setReceiptId] = useState<number | null>(null);
  const mutation = useMutation({
    mutationFn: (request: GroupInquiryRequest) => isAuthenticated
      ? runForCurrentCustomer(() => createMyGroupInquiry(request), (receipt) => {
        setReceiptId(receipt.id);
        void client.invalidateQueries({ queryKey: ["me", "group-inquiries"] });
      })
      : createGuestGroupInquiry(request).then((receipt) => { setReceiptId(receipt.id); }),
  });
  return (
    <section id="group-inquiry-form" className="my-5">
      <h2>단체 수업 문의 접수</h2>
      {receiptId !== null ? <Alert variant="success">
        <Alert.Heading>문의가 접수되었습니다.</Alert.Heading>
        <p className="mb-1">접수 번호 {receiptId} · 입력한 연락처로 상담을 안내합니다.</p>
        {isAuthenticated && <Link to="/my/group-inquiries">내 문의 상태 확인</Link>}
      </Alert> : isLoading ? <LoadingSpinner /> : status === "error" ? <ErrorAlert error={error} onRetry={() => { void refresh(); }} /> : <GroupInquiryForm onSubmit={(request) => mutation.mutate(request)}
        pending={mutation.isPending} error={mutation.error} initialContact={user ?? undefined} />}
    </section>
  );
}
