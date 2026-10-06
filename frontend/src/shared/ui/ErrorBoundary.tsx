import { Component, type ErrorInfo, type ReactNode } from "react";
import { Button } from "react-bootstrap";
import * as Sentry from "@sentry/react";
import { StatusPage } from "./StatusPage";

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
      // 화면 전체를 대신하므로 머리글 없이 공방 이름과 바로가기를 함께 보여 준다.
      return (
        <StatusPage
          standalone
          kicker="오류"
          title="예기치 않은 오류가 발생했습니다"
          description="페이지를 새로고침해 주세요. 오류가 계속되면 공방에 문의해 주세요."
          actions={<Button onClick={() => window.location.reload()}>새로고침</Button>}
        />
      );
    }
    return this.props.children;
  }
}
