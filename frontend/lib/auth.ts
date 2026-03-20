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
const AUTH_UPDATED_EVENT = "loan-market-auth-updated";

export function saveAuth(auth: AuthState): void {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
  window.dispatchEvent(new CustomEvent(AUTH_UPDATED_EVENT));
}

export function clearAuth(): void {
  localStorage.removeItem(AUTH_STORAGE_KEY);
  window.dispatchEvent(new CustomEvent(AUTH_UPDATED_EVENT));
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
    const refresh = () => {
      const nextAuth = readAuth();
      setAuth(nextAuth);
      setLoading(false);
      if (redirectToLogin && !nextAuth) {
        router.replace("/login");
      }
    };

    window.addEventListener(AUTH_UPDATED_EVENT, refresh);
    void Promise.resolve().then(refresh);

    return () => window.removeEventListener(AUTH_UPDATED_EVENT, refresh);
  }, [redirectToLogin, router]);

  return { auth, loading };
}
