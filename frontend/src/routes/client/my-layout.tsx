import { Outlet } from "react-router";
import { MyShell } from "@/features/my/MyShell";

export default function MyLayoutRoute() {
  return (
    <MyShell>
      <Outlet />
    </MyShell>
  );
}
