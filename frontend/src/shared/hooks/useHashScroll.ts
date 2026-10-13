import { useEffect } from "react";
import { useLocation } from "react-router";

/**
 * 데이터를 받은 뒤에 그려지는 구역은 주소의 #id로 들어와도 브라우저가 처음에 찾지 못한다.
 * 구역이 그려진 뒤 주소의 해시가 같으면 이동마다 한 번 그 구역으로 스크롤한다.
 */
export function useHashScroll(id: string) {
  const { hash, key } = useLocation();
  useEffect(() => {
    if (hash !== `#${id}`) return;
    document.getElementById(id)?.scrollIntoView({ block: "start" });
  }, [hash, id, key]);
}
