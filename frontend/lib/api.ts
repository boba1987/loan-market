"use client";

import type { AuthState } from "@/lib/auth";

function parseJsonSafe(text: string): unknown | null {
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

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
  const data = parseJsonSafe(text);

  if (!response.ok) {
    const maybeError =
      data && typeof data === "object" && "error" in data
        ? String((data as { error?: unknown }).error ?? "")
        : "";
    const plainText = text.trim();
    const message =
      maybeError ||
      (plainText && plainText.length < 300 ? plainText : "") ||
      (response.status >= 500
        ? "Server error. Please try again in a moment."
        : `${response.status} ${response.statusText || "Request failed"}`);
    throw new Error(message);
  }

  if (!text) return null as T;
  if (data !== null) return data as T;
  throw new Error("Received an unexpected response format from server.");
}
