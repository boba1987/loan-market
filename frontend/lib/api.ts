"use client";

import type { AuthState } from "@/lib/auth";

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
  auth?: AuthState | null,
): Promise<T> {
  const headers = new Headers(options.headers);

  if (!headers.has("Content-Type") && options.body) {
    headers.set("Content-Type", "application/json");
  }
  if (auth?.token) {
    headers.set("Authorization", `Bearer ${auth.token}`);
  }

  const response = await fetch(`/backend-api${path}`, {
    ...options,
    headers,
  });

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const message =
      data?.error ?? `${response.status} ${response.statusText || "Request failed"}`;
    throw new Error(message);
  }

  return data as T;
}
