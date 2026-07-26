import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Copy } from "lucide-react";
import { Alert, Button, Form } from "react-bootstrap";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import {
  confirmAdminMfaEnrollment,
  disableAdminMfa,
  getAdminMfaStatus,
  startAdminMfaEnrollment,
  type AdminMfaEnrollmentResponse,
  type AdminMfaStatusResponse,
} from "./api";

const MFA_STATUS_QUERY_KEY = ["admin", "auth", "mfa"] as const;

interface Props {
  adminKey: string;
  onAuthError: () => void;
  onCredentialChanged: (message: string) => void;
}

export function AdminMfaSettings({
  adminKey,
  onAuthError,
  onCredentialChanged,
}: Props) {
  const queryClient = useQueryClient();
  const [enrollment, setEnrollment] = useState<AdminMfaEnrollmentResponse>();
  const [confirmationCode, setConfirmationCode] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [disableCode, setDisableCode] = useState("");
  const [recoveryCodes, setRecoveryCodes] = useState<string[]>();
  const [copied, setCopied] = useState(false);

  const statusQuery = useAdminQuery(onAuthError, {
    queryKey: MFA_STATUS_QUERY_KEY,
    queryFn: () => getAdminMfaStatus(adminKey),
  });

  const startEnrollment = useAdminMutation(onAuthError, {
    mutationFn: () => startAdminMfaEnrollment(adminKey),
    onSuccess: (result) => {
      setEnrollment(result);
      setConfirmationCode("");
      setRecoveryCodes(undefined);
      setCopied(false);
    },
  });

  const confirmEnrollment = useAdminMutation(onAuthError, {
    mutationFn: () => confirmAdminMfaEnrollment(adminKey, confirmationCode.trim()),
    onSuccess: (result) => {
      setRecoveryCodes(result.recoveryCodes);
      setEnrollment(undefined);
      setConfirmationCode("");
      queryClient.setQueryData<AdminMfaStatusResponse>(MFA_STATUS_QUERY_KEY, {
        enabled: true,
        enrollmentPending: false,
        recoveryCodesRemaining: result.recoveryCodes.length,
      });
    },
  });

  const disableMfa = useAdminMutation(onAuthError, {
    mutationFn: () => disableAdminMfa(
      adminKey,
      currentPassword,
      disableCode.trim(),
    ),
    onSuccess: () => {
      onCredentialChanged(
        "2단계 인증이 해제되었습니다. 관리자 계정을 다시 인증해 주세요.",
      );
    },
  });

  const actionError = startEnrollment.error
    ?? confirmEnrollment.error
    ?? disableMfa.error;

  if (statusQuery.isLoading) {
    return <LoadingSpinner text="2단계 인증 상태 확인 중..." />;
  }

  if (recoveryCodes) {
    const displayedRecoveryCodes = recoveryCodes;

    async function copyRecoveryCodes() {
      await navigator.clipboard.writeText(displayedRecoveryCodes.join("\n"));
      setCopied(true);
    }

    return (
      <div>
        <Alert variant="warning">
          복구 코드는 다시 표시되지 않습니다. 안전한 곳에 보관해 주세요.
        </Alert>
        <div className="border rounded p-3 mb-3">
          <div className="d-flex align-items-center justify-content-between gap-3 mb-2">
            <strong>일회용 복구 코드</strong>
            <Button
              type="button"
              size="sm"
              variant="outline-secondary"
              onClick={() => void copyRecoveryCodes()}
              title="복구 코드 모두 복사"
            >
              <Copy size={16} aria-hidden="true" className="me-1" />
              {copied ? "복사됨" : "모두 복사"}
            </Button>
          </div>
          <div className="row g-2">
            {displayedRecoveryCodes.map((code) => (
              <code key={code} className="col-sm-6 text-dark">{code}</code>
            ))}
          </div>
        </div>
        <Button
          type="button"
          variant="primary"
          onClick={() => onCredentialChanged(
            "2단계 인증이 설정되었습니다. 다시 로그인해 주세요.",
          )}
        >
          보관 완료, 다시 로그인
        </Button>
      </div>
    );
  }

  if (statusQuery.error) {
    return <ErrorAlert error={statusQuery.error} />;
  }

  const status = statusQuery.data;
  if (!status) return null;

  if (status.enabled) {
    const canDisable = currentPassword.length > 0 && disableCode.trim().length > 0;

    return (
      <div>
        <p className="mb-3">
          2단계 인증 사용 중 · 남은 복구 코드 {status.recoveryCodesRemaining}개
        </p>
        <Form
          onSubmit={(event) => {
            event.preventDefault();
            if (canDisable && !disableMfa.isPending) disableMfa.mutate();
          }}
        >
          <div className="row g-3 align-items-end">
            <div className="col-md-5">
              <Form.Group controlId="admin-mfa-disable-password">
                <Form.Label>현재 비밀번호</Form.Label>
                <Form.Control
                  type="password"
                  autoComplete="current-password"
                  maxLength={100}
                  value={currentPassword}
                  disabled={disableMfa.isPending}
                  onChange={(event) => setCurrentPassword(event.target.value)}
                />
              </Form.Group>
            </div>
            <div className="col-md-5">
              <Form.Group controlId="admin-mfa-disable-code">
                <Form.Label>인증 코드 또는 복구 코드</Form.Label>
                <Form.Control
                  type="text"
                  autoComplete="one-time-code"
                  maxLength={32}
                  value={disableCode}
                  disabled={disableMfa.isPending}
                  onChange={(event) => setDisableCode(event.target.value)}
                />
              </Form.Group>
            </div>
            <div className="col-md-2">
              <Button
                type="submit"
                variant="outline-danger"
                className="w-100"
                disabled={!canDisable || disableMfa.isPending}
              >
                {disableMfa.isPending ? "해제 중..." : "해제"}
              </Button>
            </div>
          </div>
          <ErrorAlert error={actionError} />
        </Form>
      </div>
    );
  }

  const canConfirm = confirmationCode.trim().length === 6;

  return (
    <div>
      <p className="mb-3">
        {status.enrollmentPending
          ? "완료되지 않은 설정이 있습니다."
          : "관리자 로그인에 인증 앱 코드를 추가할 수 있습니다."}
      </p>

      {!enrollment && (
        <Button
          type="button"
          variant="outline-dark"
          disabled={startEnrollment.isPending}
          onClick={() => startEnrollment.mutate()}
        >
          {startEnrollment.isPending
            ? "등록 준비 중..."
            : status.enrollmentPending
              ? "설정 다시 시작"
              : "2단계 인증 설정"}
        </Button>
      )}

      {enrollment && (
        <>
          <div className="border rounded p-3 mb-3">
            <div className="mb-3">
              <div className="small text-secondary mb-1">인증 앱 등록 키</div>
              <code className="text-dark text-break user-select-all">{enrollment.secret}</code>
            </div>
            <Button
              as="a"
              href={enrollment.provisioningUri}
              size="sm"
              variant="outline-secondary"
            >
              인증 앱에서 열기
            </Button>
          </div>
          <Form
            onSubmit={(event) => {
              event.preventDefault();
              if (canConfirm && !confirmEnrollment.isPending) {
                confirmEnrollment.mutate();
              }
            }}
          >
            <div className="row g-3 align-items-end">
              <div className="col-sm-8">
                <Form.Group controlId="admin-mfa-enrollment-code">
                  <Form.Label>인증 앱의 6자리 코드</Form.Label>
                  <Form.Control
                    type="text"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    pattern="[0-9]{6}"
                    minLength={6}
                    maxLength={6}
                    value={confirmationCode}
                    disabled={confirmEnrollment.isPending}
                    onChange={(event) => setConfirmationCode(event.target.value)}
                  />
                </Form.Group>
              </div>
              <div className="col-sm-4">
                <Button
                  type="submit"
                  variant="primary"
                  className="w-100"
                  disabled={!canConfirm || confirmEnrollment.isPending}
                >
                  {confirmEnrollment.isPending ? "확인 중..." : "등록 확인"}
                </Button>
              </div>
            </div>
          </Form>
        </>
      )}

      <ErrorAlert error={actionError} />
    </div>
  );
}
