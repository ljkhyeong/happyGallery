const GRID_MINUTES = 30;
const ROW_HEIGHT_PX = 42;

export interface HalfHourScheduleItem {
  id: string | number;
  start: string;
  end: string;
  title: string;
  detail?: string;
  tone?: "primary" | "success" | "warning" | "danger" | "muted";
}

interface Props {
  ariaLabel: string;
  date: string;
  startTime?: string;
  endTime?: string;
  items: HalfHourScheduleItem[];
  emptyMessage: string;
}

interface NormalizedItem extends HalfHourScheduleItem {
  startMinute: number;
  endMinute: number;
}

interface PositionedItem extends NormalizedItem {
  lane: number;
  laneCount: number;
}

function minuteOf(value: string): number | null {
  const match = value.match(/(?:^|T)(\d{2}):(\d{2})/);
  if (!match) return null;
  return Number(match[1]) * 60 + Number(match[2]);
}

function timeLabel(totalMinutes: number): string {
  const hour = totalMinutes === 24 * 60 ? 24 : Math.floor(totalMinutes / 60) % 24;
  const minute = totalMinutes % 60;
  return `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
}

function positionOverlaps(items: NormalizedItem[]): PositionedItem[] {
  const positioned: PositionedItem[] = [];
  let cluster: Array<NormalizedItem & { lane: number }> = [];
  let clusterEnd = -1;
  let laneEnds: number[] = [];

  const finishCluster = () => {
    const laneCount = Math.max(laneEnds.length, 1);
    positioned.push(...cluster.map((item) => ({ ...item, laneCount })));
    cluster = [];
    clusterEnd = -1;
    laneEnds = [];
  };

  for (const item of items) {
    if (cluster.length > 0 && item.startMinute >= clusterEnd) {
      finishCluster();
    }
    const reusableLane = laneEnds.findIndex((endMinute) => endMinute <= item.startMinute);
    const lane = reusableLane >= 0 ? reusableLane : laneEnds.length;
    laneEnds[lane] = item.endMinute;
    cluster.push({ ...item, lane });
    clusterEnd = Math.max(clusterEnd, item.endMinute);
  }
  if (cluster.length > 0) finishCluster();
  return positioned;
}

export function HalfHourDaySchedule({
  ariaLabel,
  date,
  startTime,
  endTime,
  items,
  emptyMessage,
}: Props) {
  const normalized = items
    .map((item): NormalizedItem | null => {
      const startMinute = minuteOf(item.start);
      const endMinute = minuteOf(item.end);
      if (startMinute === null || endMinute === null || endMinute <= startMinute) return null;
      return { ...item, startMinute, endMinute };
    })
    .filter((item): item is NormalizedItem => item !== null)
    .sort((left, right) => left.startMinute - right.startMinute || left.endMinute - right.endMinute);

  const earliestItem = normalized[0]?.startMinute;
  const latestItem = normalized.reduce<number | null>(
    (latest, item) => latest === null ? item.endMinute : Math.max(latest, item.endMinute),
    null,
  );
  const configuredStart = startTime ? minuteOf(startTime) : null;
  const configuredEnd = endTime ? minuteOf(endTime) : null;
  const rangeStart = configuredStart
    ?? Math.max((earliestItem ?? 10 * 60) - GRID_MINUTES, 0);
  const rangeEnd = configuredEnd
    ?? Math.min((latestItem ?? 19 * 60) + GRID_MINUTES, 24 * 60);
  const viewStart = Math.floor(
    Math.min(rangeStart, earliestItem ?? rangeStart) / GRID_MINUTES,
  ) * GRID_MINUTES;
  const viewEndCandidate = Math.ceil(
    Math.max(rangeEnd, latestItem ?? rangeEnd) / GRID_MINUTES,
  ) * GRID_MINUTES;
  const viewEnd = Math.max(viewEndCandidate, viewStart + GRID_MINUTES);
  const rowCount = (viewEnd - viewStart) / GRID_MINUTES;
  const height = rowCount * ROW_HEIGHT_PX;
  const markers = Array.from({ length: rowCount + 1 }, (_, index) => viewStart + index * GRID_MINUTES);
  const positioned = positionOverlaps(normalized);

  return (
    <section className="admin-day-schedule" aria-label={ariaLabel}>
      <div className="admin-day-schedule-heading">
        <strong>{date}</strong>
        <span>30분 눈금</span>
      </div>
      <div className="admin-day-schedule-scroll">
        <div className="admin-day-schedule-grid" style={{ height }}>
          <div className="admin-day-schedule-axis" aria-hidden="true">
            {markers.map((minute, index) => (
              <time
                key={minute}
                className={index === 0
                  ? "is-first"
                  : index === markers.length - 1
                    ? "is-last"
                    : undefined}
                style={{ top: index * ROW_HEIGHT_PX }}
              >
                {timeLabel(minute)}
              </time>
            ))}
          </div>
          <div className="admin-day-schedule-canvas" role="list">
            {markers.map((minute, index) => (
              <span
                key={minute}
                className="admin-day-schedule-line"
                style={{ top: index * ROW_HEIGHT_PX }}
                aria-hidden="true"
              />
            ))}
            {positioned.map((item) => {
              const clippedStart = Math.max(item.startMinute, viewStart);
              const clippedEnd = Math.min(item.endMinute, viewEnd);
              const left = (item.lane / item.laneCount) * 100;
              const width = 100 / item.laneCount;
              return (
                <div
                  key={item.id}
                  role="listitem"
                  className={`admin-day-schedule-item is-${item.tone ?? "primary"}`}
                  style={{
                    top: ((clippedStart - viewStart) / GRID_MINUTES) * ROW_HEIGHT_PX + 2,
                    height: Math.max(((clippedEnd - clippedStart) / GRID_MINUTES) * ROW_HEIGHT_PX - 4, 24),
                    left: `calc(${left}% + 3px)`,
                    width: `calc(${width}% - 6px)`,
                  }}
                  title={`${timeLabel(item.startMinute)}–${timeLabel(item.endMinute)} ${item.title}`}
                >
                  <strong>{item.title}</strong>
                  <small>{timeLabel(item.startMinute)}–{timeLabel(item.endMinute)}</small>
                  {item.detail && <small>{item.detail}</small>}
                </div>
              );
            })}
            {positioned.length === 0 && (
              <p className="admin-day-schedule-empty">{emptyMessage}</p>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
