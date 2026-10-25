import { useState } from "react";
import { useToast } from "@/shared/ui/ToastContainer";
import { useAdminMutation } from "./useAdminMutation";
import { isVersionConflict, useVersionedEdit } from "./useVersionedEdit";

interface ListItem {
  id: number;
  version: number;
}

interface Versioned {
  version: number;
}

interface VersionedCrudFormOptions<TItem extends ListItem, TDetail extends Versioned, TForm> {
  onAuthError: () => void;
  /** 저장 실패가 다른 관리자의 선행 수정인지 판정한다. 기본은 HTTP 409 전체다. */
  isConflict?: (error: unknown) => boolean;
  emptyForm: () => TForm;
  /** 수정을 시작할 때 목록 값으로 먼저 채우는 초안. 최신 상세를 읽으면 formFrom으로 덮어쓴다. */
  draftFrom: (item: TItem) => TForm;
  formFrom: (detail: TDetail) => TForm;
  fetchLatest: (id: number) => Promise<TDetail>;
  create: (form: TForm) => Promise<unknown>;
  update: (id: number, form: TForm, expectedVersion: number) => Promise<unknown>;
  remove: (id: number, version: number) => Promise<unknown>;
  /** 등록·수정·삭제 성공 뒤 다시 읽을 캐시를 무효화한다. */
  onSaved: () => void;
  /** 저장·삭제가 다른 관리자의 변경과 충돌했을 때 목록을 다시 읽게 한다. */
  onConflict: () => void;
  messages: { created: string; updated: string; deleted: string };
}

/**
 * 버전 편집 목록 화면의 폼 열림·수정 대상·초안·작업 오류 상태와 등록·수정·삭제 mutation을 한곳에서 관리한다.
 * 렌더(토글 버튼·오류 표시·충돌 안내·제출 조건)와 캐시 무효화 대상은 각 영역이 갖는다.
 */
export function useVersionedCrudForm<TItem extends ListItem, TDetail extends Versioned, TForm>({
  onAuthError,
  isConflict = isVersionConflict,
  emptyForm,
  draftFrom,
  formFrom,
  fetchLatest,
  create,
  update,
  remove,
  onSaved,
  onConflict,
  messages,
}: VersionedCrudFormOptions<TItem, TDetail, TForm>) {
  const toast = useToast();
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [form, setForm] = useState<TForm>(emptyForm);
  const [actionError, setActionError] = useState<Error | null>(null);
  const edit = useVersionedEdit<TDetail>({
    onAuthError,
    isConflict,
    applyToForm: (latest) => setForm(formFrom(latest)),
    setError: setActionError,
    onConflict,
  });

  const resetForm = () => {
    edit.reset();
    setShowForm(false);
    setEditId(null);
    setForm(emptyForm());
    setActionError(null);
  };

  const createMutation = useAdminMutation(onAuthError, {
    mutationFn: () => create(form),
    onMutate: () => setActionError(null),
    onSuccess: () => {
      toast.show(messages.created);
      resetForm();
      onSaved();
    },
    onError: setActionError,
  });

  const updateMutation = useAdminMutation(onAuthError, {
    mutationFn: () => update(editId!, form, edit.version!),
    onMutate: () => setActionError(null),
    onSuccess: () => {
      toast.show(messages.updated);
      resetForm();
      onSaved();
    },
    onError: (error) => edit.handleSaveError(error, () => fetchLatest(editId!)),
  });

  const deleteMutation = useAdminMutation(onAuthError, {
    mutationFn: ({ id, version }: ListItem) => remove(id, version),
    onMutate: () => setActionError(null),
    onSuccess: () => {
      toast.show(messages.deleted);
      onSaved();
    },
    onError: edit.handleDeleteError,
  });

  /** 목록 값으로 폼을 먼저 채우고 최신 상세를 읽는다. 상세를 읽기 전에는 저장 버전이 비어 저장이 막힌다. */
  const startEdit = (item: TItem) => {
    setEditId(item.id);
    setForm(draftFrom(item));
    setShowForm(true);
    void edit.startEdit(() => fetchLatest(item.id));
  };

  /** 열려 있으면 닫고, 닫혀 있으면 초안을 비운 새 작성 폼을 연다. */
  const toggleForm = () => {
    resetForm();
    if (!showForm) setShowForm(true);
  };

  /** 입력 검증은 호출하는 영역이 먼저 한다. 수정 중이면 최신 버전을 읽은 뒤에만 저장한다. */
  const submit = () => {
    if (editId === null) createMutation.mutate();
    else if (edit.version !== null) updateMutation.mutate();
  };

  return {
    edit,
    form,
    setForm,
    showForm,
    editId,
    actionError,
    saving: createMutation.isPending || updateMutation.isPending,
    deleteMutation,
    startEdit,
    toggleForm,
    submit,
  };
}
