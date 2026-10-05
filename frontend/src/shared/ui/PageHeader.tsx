import type { ReactNode } from "react";

interface Props {
  kicker: string;
  title: string;
  description?: ReactNode;
  actions?: ReactNode;
}

/** 목록·체크아웃 화면의 머리: 영문 kicker, 세리프 제목, 설명을 같은 간격으로 보여 준다. */
export function PageHeader({ kicker, title, description, actions }: Props) {
  return (
    <header className="store-section-header page-header mb-4">
      <div>
        <p className="store-section-kicker mb-2">{kicker}</p>
        <h1 className="store-section-title">{title}</h1>
        {description && <p className="store-section-desc mb-0">{description}</p>}
      </div>
      {actions && <div className="page-header-actions">{actions}</div>}
    </header>
  );
}
