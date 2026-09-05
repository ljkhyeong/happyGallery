import { useState } from "react";
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Card, Form } from "react-bootstrap";
import { Heart } from "lucide-react";
import { Link } from "react-router";
import { getMyFavoriteStatus, saveMyFavorite, removeMyFavorite, listMyFavorites,
  type FavoriteResponse } from "@/generated/api/customerStore";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { runForCurrentCustomer } from "@/shared/api";
import { ErrorAlert, LoadingSpinner, EmptyState, LinkButton, useToast } from "@/shared/ui";

type TargetType = FavoriteResponse["targetType"];
const favoritesKey = ["me", "favorites"] as const;
const targetHref = (type: TargetType, id: number) => `${type === "PRODUCT" ? "/products" : "/classes"}/${id}`;

export function FavoriteButton(props: { type: TargetType; targetId: number }) {
  const { sessionVersion } = useCustomerAuth();
  return <FavoriteButtonContent key={sessionVersion} {...props} />;
}

function FavoriteButtonContent({ type, targetId }: { type: TargetType; targetId: number }) {
  const { isAuthenticated, isLoading } = useCustomerAuth();
  const client = useQueryClient();
  const toast = useToast();
  const query = useQuery({ queryKey: [...favoritesKey, "status", type, targetId], enabled: isAuthenticated,
    queryFn: ({ signal }) => runForCurrentCustomer(() => getMyFavoriteStatus(type, targetId, { signal })) });
  const saved = query.data?.saved ?? false;
  const mutation = useMutation({
    mutationFn: () => runForCurrentCustomer(() => saved ? removeMyFavorite(type, targetId) : saveMyFavorite(type, targetId),
      async (_, requireCurrent) => {
        await client.invalidateQueries({ queryKey: favoritesKey });
        requireCurrent();
        toast.show(saved ? "찜을 해제했습니다." : "찜에 저장했습니다.");
      }),
  });
  if (isLoading) return null;
  if (!isAuthenticated) return <LinkButton size="sm" variant="outline-secondary"
    to={buildAuthPageHref("/login", { redirectTo: targetHref(type, targetId) })}>로그인하고 찜하기</LinkButton>;
  return <div className="my-2">
    <Button size="sm" variant={saved ? "dark" : "outline-dark"} aria-pressed={saved}
      disabled={query.isLoading || query.isError || mutation.isPending} onClick={() => mutation.mutate()}>
      <Heart size={16} fill={saved ? "currentColor" : "none"} aria-hidden className="me-1" />
      {type === "PRODUCT" ? "상품" : "클래스"} {saved ? "찜 해제" : "찜하기"}
    </Button>
    <Link to="/my/favorites" className="small ms-3">내 찜 보기</Link>
    <ErrorAlert error={query.error ?? mutation.error} onRetry={() => { void query.refetch(); }} />
  </div>;
}

export function MyFavoritesSection() {
  const [type, setType] = useState<TargetType | "">("");
  const { isAuthenticated } = useCustomerAuth();
  const client = useQueryClient();
  const query = useInfiniteQuery({
    queryKey: [...favoritesKey, "list", type], enabled: isAuthenticated,
    queryFn: ({ pageParam, signal }) => runForCurrentCustomer(() => listMyFavorites({ type: type || undefined, cursor: pageParam, size: 20 }, { signal })),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (page) => page.hasMore ? page.nextCursor ?? undefined : undefined,
  });
  const mutation = useMutation({
    mutationFn: (item: FavoriteResponse) => runForCurrentCustomer(() => removeMyFavorite(item.targetType, item.targetId),
      async (_, requireCurrent) => {
        await client.invalidateQueries({ queryKey: favoritesKey });
        requireCurrent();
      }),
  });
  const items = query.data?.pages.flatMap((page) => page.content) ?? [];
  return <Card id="my-favorites" className="mb-4"><Card.Body>
    <div className="d-flex align-items-center justify-content-between mb-2">
      <h6 className="mb-0">내 찜</h6>
      <Form.Select aria-label="찜 종류" size="sm" style={{ width: 140 }} value={type} onChange={(event) => setType(event.target.value as TargetType | "")}>
        <option value="">전체</option><option value="PRODUCT">상품</option><option value="CLASS">클래스</option>
      </Form.Select>
    </div>
    {query.isLoading && <LoadingSpinner />}
    <ErrorAlert error={query.error ?? mutation.error} onRetry={() => { void query.refetch(); }} />
    {query.data && items.length === 0 && <EmptyState message="찜한 항목이 없습니다." />}
    {items.map((item) => <div key={item.id} className="d-flex justify-content-between align-items-center gap-2 border-bottom py-2">
      <div><span className="small text-muted me-2">{item.targetType === "PRODUCT" ? "상품" : "클래스"}</span>
        {item.active ? <Link to={targetHref(item.targetType, item.targetId)}>{item.name}</Link> : <span>{item.name} · 현재 이용할 수 없음</span>}
      </div>
      <Button size="sm" variant="outline-secondary" disabled={mutation.isPending} onClick={() => mutation.mutate(item)} aria-label={`${item.name} 찜 해제`}>찜 해제</Button>
    </div>)}
    {query.hasNextPage && <Button className="mt-2" size="sm" variant="outline-primary" disabled={query.isFetchingNextPage}
      onClick={() => { void query.fetchNextPage(); }}>찜 더 보기</Button>}
  </Card.Body></Card>;
}
