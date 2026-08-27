import { useSyncExternalStore } from "react";
import type { User } from "../../types/api";

export type AuthStatus = "initializing" | "authenticated" | "unauthenticated";

export interface AuthState {
  accessToken: string | null;
  user: User | null;
  status: AuthStatus;
}

let state: AuthState = {
  accessToken: null,
  user: null,
  status: "initializing",
};

const listeners = new Set<() => void>();

function update(next: AuthState) {
  state = next;
  listeners.forEach((listener) => listener());
}

export const authStore = {
  getSnapshot: () => state,
  subscribe: (listener: () => void) => {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
  setAuthenticated: (accessToken: string, user: User) =>
    update({ accessToken, user, status: "authenticated" }),
  setUser: (user: User) => {
    if (state.accessToken) update({ ...state, user, status: "authenticated" });
  },
  setAccessToken: (accessToken: string) =>
    update({ ...state, accessToken, status: state.user ? "authenticated" : "initializing" }),
  clear: () => update({ accessToken: null, user: null, status: "unauthenticated" }),
  initialize: () => update({ ...state, status: "initializing" }),
};

export function useAuthState() {
  return useSyncExternalStore(authStore.subscribe, authStore.getSnapshot, authStore.getSnapshot);
}
