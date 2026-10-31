import { useEffect, type RefObject } from "react";

const ACTIVE_ITEM = ".active, [aria-current='page'], [aria-pressed='true']";

/**
 * 가로로 밀어 보는 메뉴·탭·칩 줄에서 현재 항목을 줄 가운데로 옮긴다.
 * 줄이 다 보이면 그대로 두고 세로 스크롤은 건드리지 않는다. key가 바뀔 때마다(이동·선택) 다시 맞춘다.
 */
export function useCenterActiveItem(ref: RefObject<HTMLElement | null>, key: unknown) {
  useEffect(() => {
    const row = ref.current;
    if (!row || row.scrollWidth <= row.clientWidth) return;
    const active = row.querySelector<HTMLElement>(ACTIVE_ITEM);
    // 화면을 옮겨도 남는 머리글 줄은 현재 항목이 없는 화면(예: 홈)에서 처음으로 돌린다.
    if (!active) {
      row.scrollLeft = 0;
      return;
    }
    // offsetLeft는 가장 가까운 positioned 조상 기준이라 줄 안의 위치를 화면 좌표 차이로 구한다.
    const left = active.getBoundingClientRect().left - row.getBoundingClientRect().left + row.scrollLeft;
    row.scrollLeft = left - (row.clientWidth - active.offsetWidth) / 2;
  }, [ref, key]);
}
