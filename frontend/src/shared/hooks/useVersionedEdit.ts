import { useRef, useState } from "react";
import { ApiError } from "@/shared/api";
import { useToast } from "@/shared/ui/ToastContainer";
import { isAdminSessionUnauthorized } from "./adminSessionUnauthorized";

interface Versioned {
  version: number;
}

interface VersionedEditOptions<T extends Versioned> {
  onAuthError: () => void;
  /** 저장 실패가 다른 관리자의 선행 수정인지 판정한다. 서버 오류 코드가 영역마다 달라 각 영역에서 정한다. */
  isConflict: (error: unknown) => boolean;
  /** 서버의 최신 내용을 폼 상태에 채운다. 저장 버전은 훅이 관리한다. */
  applyToForm: (latest: T) => void;
  /** 영역 오류 표시를 바꾼다. null이면 지운다. */
  setError: (error: Error | null) => void;
  /**
   * 다른 관리자의 변경을 확인했을 때 캐시를 맞춘다.
   * 저장 충돌이면 다시 읽은 최신 내용을, 삭제 충돌이면 null을 받는다.
   */
  onConflict?: (latest: T | null) => void;
}

/** 버전을 함께 보낸 저장·삭제가 다른 관리자의 변경과 충돌했는지 HTTP 상태로 판정한다. */
export function isVersionConflict(error: unknown): error is ApiError {
  return error instanceof ApiError && error.status === 409;
}

/**
 * 버전 기반 편집의 저장 버전·최신 내용 조회·충돌 선택지를 관리한다.
 * 폼 상태와 API 호출은 각 영역이 갖고, 조회 함수는 호출할 때 넘긴다.
 */
export function useVersionedEdit<T extends Versioned>({
  onAuthError,
  isConflict,
  applyToForm,
  setError,
  onConflict,
}: VersionedEditOptions<T>) {
  const toast = useToast();
  // 닫거나 다른 항목으로 바꾼 편집에 늦게 도착한 조회 결과를 적용하지 않도록 요청 순번을 둔다.
  const requestSequence = useRef(0);
  const [version, setVersion] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [conflict, setConflict] = useState<T | null>(null);

  const applyLatest = (latest: T) => {
    applyToForm(latest);
    setVersion(latest.version);
    setConflict(null);
  };

  const load = async (
    request: number,
    fetchLatest: () => Promise<T>,
    onLoaded: (latest: T) => void,
  ) => {
    setLoading(true);
    try {
      const latest = await fetchLatest();
      if (request !== requestSequence.current) return;
      onLoaded(latest);
    } catch (error) {
      if (request !== requestSequence.current) return;
      if (isAdminSessionUnauthorized(error)) onAuthError();
      setError(error instanceof Error ? error : new Error("최신 내용을 불러오지 못했습니다."));
    } finally {
      if (request === requestSequence.current) setLoading(false);
    }
  };

  /** 목록 값으로 폼을 먼저 채운 뒤 호출한다. 최신 상세를 읽기 전에는 저장 버전을 비워 저장을 막는다. */
  const startEdit = (fetchLatest: () => Promise<T>) => {
    const request = ++requestSequence.current;
    setVersion(null);
    setConflict(null);
    setError(null);
    return load(request, fetchLatest, applyLatest);
  };

  /** 버전 충돌이면 작성 중인 초안을 그대로 두고 최신 내용을 읽어 선택지를 보여 준다. */
  const handleSaveError = (error: Error, fetchLatest: () => Promise<T>) => {
    if (!isConflict(error)) {
      setError(error);
      return Promise.resolve();
    }
    return load(requestSequence.current, fetchLatest, (latest) => {
      setConflict(latest);
      setError(null);
      onConflict?.(latest);
    });
  };

  /** 삭제 실패를 보여 주고, 버전 충돌이면 목록을 다시 읽게 한다. */
  const handleDeleteError = (error: Error) => {
    setError(error);
    if (isVersionConflict(error)) onConflict?.(null);
  };

  const keepDraft = () => {
    if (!conflict) return;
    setVersion(conflict.version);
    setConflict(null);
    toast.show("입력 내용은 유지했습니다. 최신 내용을 확인한 뒤 다시 저장해 주세요.");
  };

  const loadLatest = () => {
    if (conflict) applyLatest(conflict);
  };

  const reset = () => {
    requestSequence.current += 1;
    setVersion(null);
    setLoading(false);
    setConflict(null);
  };

  return {
    version,
    loading,
    conflict,
    startEdit,
    handleSaveError,
    handleDeleteError,
    keepDraft,
    loadLatest,
    applyLatest,
    reset,
  };
}
