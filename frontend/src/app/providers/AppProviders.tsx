import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { type PropsWithChildren, useEffect, useState } from "react";
import { refreshAccessToken } from "../../lib/api/client";
import { authStore } from "../../lib/auth/authStore";

export function AppProviders({ children }: PropsWithChildren) {
  const [queryClient] = useState(() => new QueryClient({
    defaultOptions: {
      queries: { staleTime: 20_000, retry: 1, refetchOnWindowFocus: false },
      mutations: { retry: 0 },
    },
  }));

  useEffect(() => {
    authStore.initialize();
    void refreshAccessToken().catch(() => authStore.clear());
  }, []);

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

