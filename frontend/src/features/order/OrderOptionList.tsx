import type { OrderOptionSnapshotResponse } from "@/generated/api/order";
import { formatKRW } from "@/shared/lib";

export type OrderOptionDisplay = Pick<OrderOptionSnapshotResponse, "groupName" | "value" | "priceAdjustment">;

export function OrderOptionList({ options }: { options: readonly OrderOptionDisplay[] }) {
  if (options.length === 0) return null;
  return (
    <div className="small text-muted mt-1" style={{ whiteSpace: "pre-wrap", overflowWrap: "anywhere" }}>
      {options.map((option, index) => (
        <div key={`${index}-${option.groupName}`}>
          {option.groupName}: {option.value}
          {option.priceAdjustment > 0 ? ` (+${formatKRW(option.priceAdjustment)})` : ""}
        </div>
      ))}
    </div>
  );
}
