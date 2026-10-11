import {
  type ClassSituationTagCode,
  getClassSituationTagLabel,
  isClassSituationTag,
} from "@/shared/lib";
import { isSlotWhen, type SlotWhen } from "./upcomingSlots";

export interface ClassListFilters {
  passEligibleOnly: boolean;
  tag: ClassSituationTagCode | null;
  when: SlotWhen | null;
}

export function readClassListFilters(params: URLSearchParams): ClassListFilters {
  const tag = params.get("tag");
  const when = params.get("when");
  return {
    passEligibleOnly: params.get("passEligible") === "true",
    tag: isClassSituationTag(tag) ? tag : null,
    when: isSlotWhen(when) ? when : null,
  };
}

const WHEN_TITLE: Record<SlotWhen, string> = {
  weekend: "이번 주말에 들을 수 있는 수업",
  today: "오늘 들을 수 있는 수업",
};

/**
 * 일정·상황·4회권 조건으로 들어온 클래스 목록의 제목. 화면 제목과 문서 제목이 같은 값을 쓴다.
 * 조건이 없으면 documentName이 없다.
 */
export function classListHeading({ passEligibleOnly, tag, when }: ClassListFilters): {
  title: string;
  documentName?: string;
} {
  if (when) return { title: WHEN_TITLE[when], documentName: WHEN_TITLE[when] };
  if (tag) {
    const label = getClassSituationTagLabel(tag);
    return { title: label, documentName: `${label} 클래스` };
  }
  if (passEligibleOnly) return { title: "4회권으로 들을 수 있는 수업", documentName: "4회권으로 들을 수 있는 수업" };
  return { title: "공예 클래스" };
}
