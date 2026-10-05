import { Component, type ErrorInfo, type ReactNode } from "react";
import { Alert, Button } from "react-bootstrap";
import * as Sentry from "@sentry/react";

interface Props {
  children: ReactNode;
  /** 다시 시도 전에 원인이 된 캐시 등을 비운다. 비우지 않으면 같은 데이터로 다시 멈출 수 있다. */
  onReset?: () => void;
}

interface State {
  hasError: boolean;
}

/**
 * 한 영역의 화면 오류가 페이지 전체를 오류 화면으로 바꾸지 않게 그 영역만 대신 안내한다.
 * 관리자처럼 여러 작업 영역이 한 화면에 모인 곳에서 쓴다.
 */
export class SectionErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    Sentry.captureException(error, { extra: { componentStack: info.componentStack } });
  }

  render() {
    if (this.state.hasError) {
      return (
        <Alert variant="danger" className="mb-0" role="alert">
          <div>이 영역을 불러오지 못했습니다. 다른 영역은 그대로 사용할 수 있습니다.</div>
          <Button
            type="button"
            variant="outline-danger"
            size="sm"
            className="mt-2"
            onClick={() => {
              this.props.onReset?.();
              this.setState({ hasError: false });
            }}
          >
            다시 시도
          </Button>
        </Alert>
      );
    }
    return this.props.children;
  }
}
