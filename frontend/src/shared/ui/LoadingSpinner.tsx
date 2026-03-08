import { Spinner } from "react-bootstrap";

interface Props {
  text?: string;
}

export function LoadingSpinner({ text = "불러오는 중..." }: Props) {
  return (
    <div className="d-flex flex-column align-items-center justify-content-center py-5 text-muted-soft">
      <Spinner animation="border" size="sm" className="mb-2" />
      <small>{text}</small>
    </div>
  );
}
