import { Alert, Button } from "react-bootstrap";

interface Props {
  /** 영역마다 다른 충돌 안내 문구 */
  message: string;
  onKeepDraft: () => void;
  onLoadLatest: () => void;
}

/** 버전 충돌 뒤 작성 중인 초안을 유지할지, 다른 관리자가 저장한 내용을 불러올지 고르게 한다. */
export function VersionConflictAlert({ message, onKeepDraft, onLoadLatest }: Props) {
  return (
    <Alert variant="warning">
      <p className="mb-2">{message}</p>
      <div className="d-flex flex-wrap gap-2">
        <Button type="button" size="sm" variant="outline-dark" onClick={onKeepDraft}>
          내 초안 유지
        </Button>
        <Button type="button" size="sm" variant="outline-secondary" onClick={onLoadLatest}>
          다른 관리자가 저장한 내용 불러오기
        </Button>
      </div>
    </Alert>
  );
}
