import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Badge, Button, Spinner } from "react-bootstrap";
import { ApiError } from "@/shared/api";
import { getUserMessage } from "@/shared/lib";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import { useToast } from "@/shared/ui";
import {
  SOCIAL_PROVIDER_DETAILS,
  SOCIAL_PROVIDERS,
  type SocialProvider,
} from "@/features/customer-auth/socialAuth";
import {
  fetchLinkedSocialProviders,
  startSocialAccountLink,
  unlinkSocialAccount,
} from "@/features/customer-auth/socialAccountApi";

interface Props {
  localPasswordEnabled: boolean;
}

export function SocialAccountSection({ localPasswordEnabled }: Props) {
  const toast = useToast();
  const [startingProvider, setStartingProvider] = useState<SocialProvider | null>(null);
  const { data: linkedProviders = [], isLoading } = useQuery({
    queryKey: ["me", "social-accounts"],
    queryFn: fetchLinkedSocialProviders,
  });
  const unlinkMutation = useMutation({
    mutationFn: unlinkSocialAccount,
    onSuccess: () => {
      window.location.assign("/login?returnTo=/my");
    },
    onError: (error) => {
      const message = error instanceof ApiError
        ? getUserMessage(error.code) ?? error.message
        : "소셜 계정 연결을 해제하지 못했습니다.";
      toast.show(message, "danger");
    },
  });

  const handleLink = async (provider: SocialProvider) => {
    setStartingProvider(provider);
    try {
      const { authorizationUrl } = await startSocialAccountLink(provider);
      sessionStorage.setItem(SESSION_KEYS.socialAccountLink, provider);
      window.location.assign(authorizationUrl);
    } catch (error) {
      setStartingProvider(null);
      const message = error instanceof ApiError
        ? getUserMessage(error.code) ?? error.message
        : "소셜 계정 연결을 시작하지 못했습니다.";
      toast.show(message, "danger");
    }
  };

  const canUnlink = localPasswordEnabled || linkedProviders.length > 1;

  return (
    <div className="border-top mt-3 pt-3">
      <div className="d-flex justify-content-between align-items-center gap-3 mb-2">
        <div>
          <h6 className="mb-1">소셜 로그인</h6>
        </div>
        {isLoading && <Spinner animation="border" size="sm" />}
      </div>

      <div className="d-grid gap-2">
        {SOCIAL_PROVIDERS.map((provider) => {
          const linked = linkedProviders.includes(provider);
          const details = SOCIAL_PROVIDER_DETAILS[provider];
          return (
            <div key={provider} className="d-flex justify-content-between align-items-center gap-3">
              <div className="d-flex align-items-center gap-2">
                <span>{details.label}</span>
                {linked && <Badge bg="success">연결됨</Badge>}
              </div>
              {linked ? (
                <Button
                  type="button"
                  variant="outline-danger"
                  size="sm"
                  disabled={!canUnlink || unlinkMutation.isPending}
                  onClick={() => unlinkMutation.mutate(provider)}
                >
                  해제
                </Button>
              ) : (
                <Button
                  type="button"
                  variant="outline-primary"
                  size="sm"
                  disabled={startingProvider !== null}
                  onClick={() => void handleLink(provider)}
                >
                  {startingProvider === provider ? "연결 중..." : "연결"}
                </Button>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
