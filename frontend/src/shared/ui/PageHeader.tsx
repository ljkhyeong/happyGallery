import type { ReactNode } from "react";

interface Props {
  title: string;
  description?: ReactNode;
  actions?: ReactNode;
  /** 바로 아래 탭·칩 줄이 자체 선을 가지면 머리 아래 선을 뺀다(작품·클래스 목록). */
  divider?: boolean;
}

/** 목록·체크아웃 화면의 머리: 굵은 제목과 한 줄 설명을 같은 간격으로 보여 준다. */
export function PageHeader({ title, description, actions, divider = true }: Props) {
  return (
    <header className={`store-section-header page-header mb-4${divider ? "" : " is-plain"}`}>
      <div>
        <h1 className="store-section-title">{title}</h1>
        {description && <p className="store-section-desc mb-0">{description}</p>}
      </div>
      {actions && <div className="page-header-actions">{actions}</div>}
    </header>
  );
}
