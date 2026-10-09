import { useCallback, useEffect, useEffectEvent, useState } from "react";

/** 보호 이미지 응답이 이미지인지 확인한다. 오류 문서 같은 다른 형식을 이미지로 보여 주지 않는다. */
export function assertImageBlob(blob: Blob, message: string): Blob {
  if (!blob.type.startsWith("image/")) {
    throw new Error(message);
  }
  return blob;
}

/**
 * 인증이 필요한 이미지를 Blob으로 받아 이 화면에서만 쓰는 object URL로 바꾼다.
 * 원본이 바뀌거나 화면에서 사라지면 요청을 취소하고 URL을 해제한다.
 * 개인 이미지가 남지 않게 React Query 캐시에 넣지 않으므로 조회 함수는 `cache: "no-store"`로 받는다.
 *
 * `load`는 원본이 바뀔 때만 새로 만들어 넘긴다(useCallback·useMemo). `null`이면 아무것도 읽지 않는다.
 */
export function useBlobObjectUrl(
  load: ((signal: AbortSignal) => Promise<Blob>) | null,
  onError?: (error: unknown) => void,
) {
  const [url, setUrl] = useState<string>();
  const [error, setError] = useState<unknown>();
  const [retryVersion, setRetryVersion] = useState(0);
  const reportError = useEffectEvent((nextError: unknown) => onError?.(nextError));

  useEffect(() => {
    setUrl(undefined);
    setError(undefined);
    if (!load) return;

    const controller = new AbortController();
    let disposed = false;
    let createdUrl: string | undefined;
    // 조회 함수가 바로 던진 오류도 실패 상태로 보이게 Promise 안에서 부른다.
    void new Promise<Blob>((resolve) => resolve(load(controller.signal)))
      .then((blob) => {
        if (disposed) return;
        createdUrl = URL.createObjectURL(blob);
        setUrl(createdUrl);
      })
      .catch((nextError: unknown) => {
        if (disposed || controller.signal.aborted) return;
        reportError(nextError);
        setError(nextError);
      });

    return () => {
      disposed = true;
      controller.abort();
      if (createdUrl) URL.revokeObjectURL(createdUrl);
    };
  }, [load, retryVersion]);

  const retry = useCallback(() => setRetryVersion((current) => current + 1), []);

  return { url, error, retry };
}
