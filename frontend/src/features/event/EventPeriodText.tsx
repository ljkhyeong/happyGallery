import type { EventResponse } from "./api";
import { eventPeriodLabel } from "./time";

interface Props {
  event: Pick<EventResponse, "startAt" | "endAt">;
  className?: string;
}

export function EventPeriodText({ event, className }: Props) {
  const { start, end } = eventPeriodLabel(event);
  return (
    <p className={className}>
      <time dateTime={event.startAt}>{start}</time>
      {end !== null && (
        <>
          {" ~ "}
          <time dateTime={event.endAt}>{end}</time>
        </>
      )}
    </p>
  );
}
