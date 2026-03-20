"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

export type Role = "user" | "bank" | "admin";

export type AuthState = {
  token: string;
  role: Role;
  email: string;
  name?: string;
};

const AUTH_STORAGE_KEY = "loan-market-auth";

export function saveAuth(auth: AuthState): void {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
}

export function clearAuth(): void {
  localStorage.removeItem(AUTH_STORAGE_KEY);
}

export function readAuth(): AuthState | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthState;
  } catch {
    return null;
  }
}

export function useAuthState(redirectToLogin = false): {
  auth: AuthState | null;
  loading: boolean;
} {
  const router = useRouter();
  const [auth, setAuth] = useState<AuthState | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const nextAuth = readAuth();
    // Defer state updates to avoid render-cascade warnings.
    void Promise.resolve().then(() => {
      setAuth(nextAuth);
      setLoading(false);

      if (redirectToLogin && !nextAuth) {
        router.replace("/login");
      }
    });
  }, [redirectToLogin, router]);

  return { auth, loading };
}
