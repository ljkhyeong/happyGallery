import { Component, type ErrorInfo, type ReactNode } from "react";
import { Alert, Button } from "react-bootstrap";
import * as Sentry from "@sentry/react";

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
}

export class ErrorBoundary extends Component<Props, State> {
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
        <div className="d-flex justify-content-center align-items-center" style={{ minHeight: "50vh" }}>
          <Alert variant="danger" className="text-center">
            <Alert.Heading>예기치 않은 오류가 발생했습니다.</Alert.Heading>
            <p className="mb-3">페이지를 새로고침해 주세요. 오류가 계속되면 공방에 문의해 주세요.</p>
            <Button variant="outline-danger" onClick={() => window.location.reload()}>
              새로고침
            </Button>
          </Alert>
        </div>
      );
    }
    return this.props.children;
  }
}
