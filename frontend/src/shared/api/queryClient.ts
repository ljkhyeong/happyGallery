import { QueryClient } from "@tanstack/react-query";
import { CustomerSessionChangedError } from "./customerSession";

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) =>
        !(error instanceof CustomerSessionChangedError) && failureCount < 1,
      staleTime: 30_000,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
    },
  },
});
