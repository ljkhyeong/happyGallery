import { Store } from "lucide-react";
import { ReviewDate } from "./ReviewDisplay";

interface Props {
  reply: {
    content: string;
    createdAt: string;
    edited: boolean;
    editedAt: string | null;
  } | null;
}

export function ReviewOfficialReply({ reply }: Props) {
  if (!reply) return null;

  return (
    <aside className="review-official-reply" aria-label="공방 공식 답글">
      <div className="review-official-reply-heading">
        <Store size={16} aria-hidden="true" />
        <strong>해피갤러리 공방</strong>
        {reply.edited && <span>수정됨</span>}
        <small><ReviewDate value={reply.editedAt ?? reply.createdAt} /></small>
      </div>
      <p className="mb-0">{reply.content}</p>
    </aside>
  );
}
