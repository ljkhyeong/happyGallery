import type { QueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/shared/api";
import {
  type ClassSituationTagCode,
  isClassSituationTag,
  isPerfumeClassCategory,
} from "@/shared/lib";
import type { ClassResponse } from "@/shared/types";
import type { ClassCategoryValue } from "./classCategories";

/** 클래스 등록·수정 폼이 함께 쓰는 소개 입력값 */
export interface ClassDetailValues {
  imageUrl: string;
  description: string;
  preparationInfo: string;
  targetAudience: string;
  situationTags: ClassSituationTagCode[];
}

export const EMPTY_CLASS_DETAILS: ClassDetailValues = {
  imageUrl: "",
  description: "",
  preparationInfo: "",
  targetAudience: "",
  situationTags: [],
};

export function classDetailsFrom(bookingClass: ClassResponse): ClassDetailValues {
  return {
    imageUrl: bookingClass.imageUrl ?? "",
    description: bookingClass.description ?? "",
    preparationInfo: bookingClass.preparationInfo ?? "",
    targetAudience: bookingClass.targetAudience ?? "",
    situationTags: bookingClass.situationTags.filter(isClassSituationTag),
  };
}

/** 빈 소개 문구는 보내지 않는다. */
export function classDetailPayload(details: ClassDetailValues) {
  return {
    description: details.description.trim() || undefined,
    imageUrl: details.imageUrl.trim() || undefined,
    preparationInfo: details.preparationInfo.trim() || undefined,
    targetAudience: details.targetAudience.trim() || undefined,
    situationTags: details.situationTags,
  };
}

/** 향수 수업에는 이용권을 쓸 수 없어 분류를 향수로 바꾸면 이용권 사용을 끈다. */
export function classCategoryChangeHandler(
  setCategoryValue: (value: ClassCategoryValue) => void,
  setPassEligible: (value: boolean) => void,
) {
  return (nextCategoryValue: ClassCategoryValue) => {
    setCategoryValue(nextCategoryValue);
    if (isPerfumeClassCategory(nextCategoryValue.category)) {
      setPassEligible(false);
    }
  };
}

/** 클래스 등록·수정·운영 상태 변경 뒤 관리자 목록과 공개 클래스 목록을 다시 읽는다. */
export function invalidateClassQueries(queryClient: QueryClient): void {
  void queryClient.invalidateQueries({ queryKey: queryKeys.admin.classes });
  void queryClient.invalidateQueries({ queryKey: queryKeys.catalog.classes });
}
