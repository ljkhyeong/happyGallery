import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { fetchAdminInquiries, replyInquiry } from "./api";
import type { AdminInquiryResponse } from "./api";
import { ErrorAlert, LoadingSpinner, EmptyState, useToast } from "@/shared/ui";
import { CursorPager } from "@/shared/ui/CursorPager";
import { formatDateTime } from "@/shared/lib";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { useCursorHistory } from "@/shared/hooks/useCursorHistory";
import { AdminReplyCard } from "./AdminReplyCard";
import { queryKeys } from "@/shared/api";

interface Props {
  token: string;
  onAuthError: () => void;
}

export function AdminInquirySection({ token, onAuthError }: Props) {
  const {
    cursor,
    hasPreviousPage,
    showNextPage,
    showPreviousPage,
  } = useCursorHistory();
  const { data: page, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.inquiries.page(cursor),
    queryFn: () => fetchAdminInquiries(token, cursor),
  });
  const inquiries = page?.content;

  return (
    <div>
      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} />
      {inquiries && inquiries.length === 0 && <EmptyState message="문의가 없습니다." />}
      {inquiries?.map((inq) => (
        <AdminInquiryItem
          key={inq.id}
          inquiry={inq}
          token={token}
          onAuthError={onAuthError}
        />
      ))}
      <CursorPager
        hasPreviousPage={hasPreviousPage}
        page={page}
        busy={isLoading}
        onPrevious={showPreviousPage}
        onNext={showNextPage}
      />
    </div>
  );
}

function AdminInquiryItem({
  inquiry,
  token,
  onAuthError,
}: {
  inquiry: AdminInquiryResponse;
  token: string;
  onAuthError: () => void;
}) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [replyText, setReplyText] = useState("");

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => replyInquiry(inquiry.id, replyText, token),
    onSuccess: () => {
      toast.show("답변이 등록되었습니다.");
      setReplyText("");
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.inquiries.all });
    },
  });

  return (
    <AdminReplyCard
      title={inquiry.title}
      content={inquiry.content}
      replyContent={inquiry.replyContent}
      meta={`${inquiry.userName} (회원 번호: ${inquiry.userId}) | ${formatDateTime(inquiry.createdAt)}`}
      replyLabel="문의 답변"
      replyControlId={`admin-inquiry-reply-${inquiry.id}`}
      replyText={replyText}
      onReplyTextChange={setReplyText}
      onSubmit={() => mutation.mutate()}
      pending={mutation.isPending}
      error={mutation.error}
    />
  );
}
