import { Link } from "react-router";
import { formatDate } from "@/shared/lib";
import type { MyBookingSummary, MyOrderSummary } from "./api";

interface Props {
  orderCount: number;
  bookingCount: number;
  remainingCredits: number;
  activePassCount: number;
  latestOrder: MyOrderSummary | undefined;
  nextBooking: MyBookingSummary | undefined;
}

/** 각 숫자는 해당 내역 화면으로 이어진다. 숫자는 최근 조회분 기준이라 '최근'으로 표시한다. */
export function MyStatsRow({
  orderCount,
  bookingCount,
  remainingCredits,
  activePassCount,
  latestOrder,
  nextBooking,
}: Props) {
  const tiles = [
    {
      to: "/my/bookings",
      label: "최근 예약",
      value: `${bookingCount}건`,
      hint: nextBooking ? `다음 수업 ${formatDate(nextBooking.startAt)}` : "예정된 수업 없음",
    },
    {
      to: "/my/orders",
      label: "최근 주문",
      value: `${orderCount}건`,
      hint: latestOrder ? `최근 주문 ${formatDate(latestOrder.createdAt)}` : "주문 내역 없음",
    },
    {
      to: "/my/passes",
      label: "이용권 잔여",
      value: `${remainingCredits}회`,
      hint: activePassCount > 0 ? `사용 가능한 이용권 ${activePassCount}건` : "사용 가능한 이용권 없음",
    },
  ];

  return (
    <div className="my-stat-row">
      {tiles.map((tile) => (
        <Link key={tile.to} to={tile.to} className="my-stat-tile">
          <span className="my-stat-label">{tile.label}</span>
          <strong className="my-stat-value">{tile.value}</strong>
          <span className="my-stat-hint">{tile.hint}</span>
        </Link>
      ))}
    </div>
  );
}
