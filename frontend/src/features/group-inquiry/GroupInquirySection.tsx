import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Modal } from "react-bootstrap";
import { Link } from "react-router";
import { createGuestGroupInquiry, createMyGroupInquiry, type GroupInquiryRequest } from "@/generated/api/customerStore";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { runForCurrentCustomer } from "@/shared/api";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { GroupInquiryForm } from "./GroupInquiryForm";
import { useBotProtection } from "@/features/bot-protection/useBotProtection";

export function GroupInquirySection() {
  const { sessionVersion } = useCustomerAuth();
  return <GroupInquirySectionContent key={sessionVersion} />;
}

function GroupInquirySectionContent() {
  const { user, isAuthenticated, isLoading, status, error, refresh } = useCustomerAuth();
  const client = useQueryClient();
  const [show, setShow] = useState(false);
  const [receiptId, setReceiptId] = useState<number | null>(null);
  const botProtection = useBotProtection("group_inquiry");
  const options = { headers: botProtection.token ? { "X-Bot-Token": botProtection.token } : undefined };
  const mutation = useMutation({
    mutationFn: (request: GroupInquiryRequest) => isAuthenticated
      ? runForCurrentCustomer(() => createMyGroupInquiry(request, options), (receipt) => {
        setReceiptId(receipt.id);
        void client.invalidateQueries({ queryKey: ["me", "group-inquiries"] });
      })
      : createGuestGroupInquiry(request, options).then((receipt) => { setReceiptId(receipt.id); }),
    onSettled: botProtection.reset,
  });
  return (
    <>
      <Button variant="dark" size="lg" onClick={() => setShow(true)}>단체 수업 문의 접수</Button>
      <Modal show={show} onHide={() => { if (!mutation.isPending) setShow(false); }}
        size="lg" centered scrollable backdrop={mutation.isPending ? "static" : true}
        keyboard={!mutation.isPending} aria-labelledby="group-inquiry-title">
        <Modal.Header closeButton={!mutation.isPending} closeLabel="문의 팝업 닫기">
          <Modal.Title as="h2" id="group-inquiry-title">단체 수업 문의 접수</Modal.Title>
        </Modal.Header>
        <Modal.Body id="group-inquiry-form">
          {receiptId !== null ? <Alert variant="success">
            <Alert.Heading>문의가 접수되었습니다.</Alert.Heading>
            <p className="mb-1">접수 번호 {receiptId} · 입력한 연락처로 답변드립니다.</p>
            {isAuthenticated && <Link to="/my/group-inquiries">내 문의 상태 확인</Link>}
          </Alert> : isLoading ? <LoadingSpinner /> : status === "error" ? <ErrorAlert error={error} onRetry={() => { void refresh(); }} /> : <>
            {botProtection.challenge}
            <GroupInquiryForm onSubmit={(request) => mutation.mutate(request)} submitDisabled={!botProtection.ready}
              pending={mutation.isPending} error={mutation.error} initialContact={user ?? undefined} />
          </>}
        </Modal.Body>
        {receiptId !== null && <Modal.Footer>
          <Button variant="secondary" onClick={() => setShow(false)}>닫기</Button>
        </Modal.Footer>}
      </Modal>
    </>
  );
}
