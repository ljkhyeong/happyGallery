import type { ReactNode } from "react";

interface Props {
  title: string;
  description?: ReactNode;
  actions?: ReactNode;
}

/** 목록·체크아웃 화면의 머리: 굵은 제목과 한 줄 설명을 같은 간격으로 보여 준다. */
export function PageHeader({ title, description, actions }: Props) {
  return (
    <header className="store-section-header page-header mb-4">
      <div>
        <h1 className="store-section-title">{title}</h1>
        {description && <p className="store-section-desc mb-0">{description}</p>}
      </div>
      {actions && <div className="page-header-actions">{actions}</div>}
    </header>
  );
}
