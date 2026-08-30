import { Container } from "react-bootstrap";
import { Outlet } from "react-router";
import { LoadingSpinner } from "@/shared/ui";

export function meta() {
  return [
    { title: "해피갤러리" },
    { name: "robots", content: "noindex,nofollow" },
  ];
}

export function clientLoader() {
  return null;
}

clientLoader.hydrate = true as const;

export function HydrateFallback() {
  return (
    <Container className="page-container">
      <LoadingSpinner />
    </Container>
  );
}

export default function ClientOnlyLayout() {
  return <Outlet />;
}
