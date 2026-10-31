import { useEffect, useState } from "react";

/**
 * 화면 위에서 offset(px)을 지난 마지막 구역의 id. 첫 구역에 닿기 전이면 null이다.
 * 맨 아래까지 내려오면 화면 위까지 올라오지 못하는 짧은 마지막 구역을 현재 구역으로 본다.
 */
export function useScrollSpy(ids: readonly string[], offset: number): string | null {
  const [activeId, setActiveId] = useState<string | null>(null);
  const idsKey = ids.join(" ");

  useEffect(() => {
    const sectionIds = idsKey.split(" ").filter(Boolean);
    let frame = 0;
    const update = () => {
      frame = 0;
      let current: string | null = null;
      for (const id of sectionIds) {
        const top = document.getElementById(id)?.getBoundingClientRect().top;
        if (top !== undefined && top <= offset) current = id;
      }
      const root = document.documentElement;
      if (current && window.innerHeight + window.scrollY >= root.scrollHeight - 2) {
        current = sectionIds[sectionIds.length - 1] ?? current;
      }
      setActiveId(current);
    };
    const schedule = () => {
      if (!frame) frame = requestAnimationFrame(update);
    };
    update();
    window.addEventListener("scroll", schedule, { passive: true });
    window.addEventListener("resize", schedule);
    return () => {
      window.removeEventListener("scroll", schedule);
      window.removeEventListener("resize", schedule);
      if (frame) cancelAnimationFrame(frame);
    };
  }, [idsKey, offset]);

  return activeId;
}
