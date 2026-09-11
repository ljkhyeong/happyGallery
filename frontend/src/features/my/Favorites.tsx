import { useInfiniteQuery, useMutation, useQuery, useQueryClient, type InfiniteData, type QueryClient } from "@tanstack/react-query";
import { Button, Card, Form } from "react-bootstrap";
import { Heart } from "lucide-react";
import { Link, useSearchParams } from "react-router";
import { getMyFavoriteStatus, saveMyFavorite, removeMyFavorite, listMyFavorites,
  type FavoriteResponse, type FavoritePageResponse } from "@/generated/api/customerStore";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { captureCustomerSession, requireCurrentCustomerSession, runForCurrentCustomer, runForCustomerSession, type CustomerSessionSnapshot } from "@/shared/api";
import { ErrorAlert, LoadingSpinner, EmptyState, LinkButton, useToast } from "@/shared/ui";

type TargetType = FavoriteResponse["targetType"];
const favoritesKey = ["me", "favorites"] as const;
const targetHref = (type: TargetType, id: number) => `${type === "PRODUCT" ? "/products" : "/classes"}/${id}`;

function refreshFavorites(client: QueryClient, type: TargetType, targetId: number, saved: boolean) {
  client.setQueryData([...favoritesKey, "status", type, targetId], { saved });
  if (!saved) {
    client.setQueriesData<InfiniteData<FavoritePageResponse>>(
      { queryKey: [...favoritesKey, "list"] },
      (data) => data && ({
        ...data,
        pages: data.pages.map((page) => ({
          ...page,
          content: page.content.filter((item) => item.targetType !== type || item.targetId !== targetId),
        })),
      }),
    );
  }
  return client.invalidateQueries({ queryKey: favoritesKey });
}

export function FavoriteButton(props: { type: TargetType; targetId: number }) {
  const { sessionVersion } = useCustomerAuth();
  return <FavoriteButtonContent key={`${sessionVersion}:${props.type}:${props.targetId}`} {...props} />;
}

function FavoriteButtonContent({ type, targetId }: { type: TargetType; targetId: number }) {
  const { isAuthenticated, isLoading } = useCustomerAuth();
  const client = useQueryClient();
  const toast = useToast();
  const query = useQuery({ queryKey: [...favoritesKey, "status", type, targetId], enabled: isAuthenticated,
    queryFn: ({ signal }) => runForCurrentCustomer(() => getMyFavoriteStatus(type, targetId, { signal })) });
  const saved = query.data?.saved ?? false;
  const mutation = useMutation({
    mutationFn: ({ shouldSave, customerSession }: { shouldSave: boolean; customerSession: CustomerSessionSnapshot }) => runForCustomerSession(
      customerSession,
      async () => {
        if (shouldSave) await saveMyFavorite(type, targetId);
        else await removeMyFavorite(type, targetId);
        requireCurrentCustomerSession(customerSession);
        await refreshFavorites(client, type, targetId, shouldSave);
        requireCurrentCustomerSession(customerSession);
        toast.show(shouldSave ? "찜에 저장했습니다." : "찜을 해제했습니다.");
      }),
  });
  if (isLoading) return null;
  if (!isAuthenticated) return <LinkButton size="sm" variant="outline-secondary"
    to={buildAuthPageHref("/login", { redirectTo: targetHref(type, targetId) })}>로그인하고 찜하기</LinkButton>;
  return <div className="my-2">
    <Button size="sm" variant={saved ? "dark" : "outline-dark"} aria-pressed={saved}
      disabled={query.isLoading || query.isError || mutation.isPending}
      onClick={() => mutation.mutate({ shouldSave: !saved, customerSession: captureCustomerSession() })}>
      <Heart size={16} fill={saved ? "currentColor" : "none"} aria-hidden className="me-1" />
      {type === "PRODUCT" ? "상품" : "클래스"} {saved ? "찜 해제" : "찜하기"}
    </Button>
    <Link to="/my/favorites" className="small ms-3">내 찜 보기</Link>
    <ErrorAlert error={query.error} onRetry={() => { void query.refetch(); }}
      retrying={query.isFetching || mutation.isPending} retryLabel="찜 상태 다시 확인" />
    <ErrorAlert error={mutation.error}
      onRetry={() => { if (mutation.variables) mutation.mutate(mutation.variables); }}
      retrying={mutation.isPending}
      retryLabel={mutation.variables?.shouldSave ? "찜 저장 다시 시도" : "찜 해제 다시 시도"} />
  </div>;
}

export function MyFavoritesSection() {
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedType = searchParams.get("type");
  const type = requestedType === "PRODUCT" || requestedType === "CLASS" ? requestedType : "";
  const { isAuthenticated } = useCustomerAuth();
  const client = useQueryClient();
  const query = useInfiniteQuery({
    queryKey: [...favoritesKey, "list", type], enabled: isAuthenticated,
    queryFn: ({ pageParam, signal }) => runForCurrentCustomer(() => listMyFavorites({ type: type || undefined, cursor: pageParam, size: 20 }, { signal })),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (page) => page.hasMore ? page.nextCursor ?? undefined : undefined,
  });
  const mutation = useMutation({
    mutationFn: ({ item, customerSession }: { item: FavoriteResponse; customerSession: CustomerSessionSnapshot }) => runForCustomerSession(
      customerSession,
      async () => {
        await removeMyFavorite(item.targetType, item.targetId);
        requireCurrentCustomerSession(customerSession);
        await refreshFavorites(client, item.targetType, item.targetId, false);
      }),
  });
  const items = query.data?.pages.flatMap((page) => page.content) ?? [];
  return <Card id="my-favorites" className="mb-4"><Card.Body>
    <div className="d-flex align-items-center justify-content-between mb-2">
      <h6 className="mb-0">내 찜</h6>
      <Form.Select aria-label="찜 종류" size="sm" style={{ width: 140 }} value={type} onChange={(event) => {
        const next = new URLSearchParams(searchParams);
        if (event.target.value) next.set("type", event.target.value);
        else next.delete("type");
        setSearchParams(next, { replace: true });
      }}>
        <option value="">전체</option><option value="PRODUCT">상품</option><option value="CLASS">클래스</option>
      </Form.Select>
    </div>
    {query.isLoading && <LoadingSpinner />}
    <ErrorAlert error={query.error}
      onRetry={() => { void (query.isFetchNextPageError ? query.fetchNextPage() : query.refetch()); }}
      retrying={query.isFetching || mutation.isPending}
      retryLabel={query.isFetchNextPageError ? "이어서 불러오기" : "목록 다시 불러오기"} />
    <ErrorAlert error={mutation.error}
      onRetry={() => { if (mutation.variables) mutation.mutate(mutation.variables); }}
      retrying={mutation.isPending} retryLabel="찜 해제 다시 시도" />
    {query.data && items.length === 0 && <EmptyState message="찜한 항목이 없습니다." />}
    {items.map((item) => <div key={item.id} className="d-flex justify-content-between align-items-center gap-2 border-bottom py-2">
      <div><span className="small text-muted me-2">{item.targetType === "PRODUCT" ? "상품" : "클래스"}</span>
        {item.active ? <Link to={targetHref(item.targetType, item.targetId)}>{item.name}</Link> : <span>{item.name} · 현재 이용할 수 없음</span>}
      </div>
      <Button size="sm" variant="outline-secondary" disabled={mutation.isPending}
        onClick={() => mutation.mutate({ item, customerSession: captureCustomerSession() })}
        aria-label={`${item.name} 찜 해제`}>찜 해제</Button>
    </div>)}
    {query.hasNextPage && !query.isFetchNextPageError && <Button className="mt-2" size="sm" variant="outline-primary"
      disabled={query.isFetching || mutation.isPending}
      onClick={() => { void query.fetchNextPage(); }}>{query.isFetchingNextPage ? "불러오는 중..." : "찜 더 보기"}</Button>}
  </Card.Body></Card>;
}
