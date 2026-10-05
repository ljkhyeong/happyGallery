import type { ReactNode } from "react";
import { PageHeader } from "@/shared/ui";
import { myNavLabel } from "./myNavigation";

/** 로그인 확인과 메뉴는 MyShell이 맡고, 이 컴포넌트는 메뉴와 같은 제목의 머리만 붙인다. */
export function MySectionPage({ path, description, children }: {
  path: string;
  description?: ReactNode;
  children: ReactNode;
}) {
  return (
    <>
      <PageHeader kicker="My page" title={myNavLabel(path)} description={description} />
      {children}
    </>
  );
}
