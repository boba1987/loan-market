"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import { saveAuth, type AuthState } from "@/lib/auth";
import { emailError, required } from "@/lib/validation";

type LoginResponse = {
  token: string;
  role: "user" | "bank" | "admin";
  email: string;
  name?: string;
};

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({});

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    const nextErrors = {
      email: emailError(email) ?? undefined,
      password: required(password, "Password") ?? undefined,
    };
    setFieldErrors(nextErrors);
    if (nextErrors.email || nextErrors.password) return;
    setLoading(true);
    try {
      const result = await apiFetch<LoginResponse>("/api/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      const auth: AuthState = {
        token: result.token,
        role: result.role,
        email: result.email,
        name: result.name,
      };
      saveAuth(auth);
      router.replace("/loan-applications");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-zinc-50">
      <div className="mx-auto max-w-md px-4 py-16">
        <h1 className="mb-6 text-2xl font-semibold">Loan Market Login</h1>
        <form onSubmit={onSubmit} className="space-y-4 rounded bg-white p-5 shadow">
          <div>
            <label className="mb-1 block text-sm font-medium">Email</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded border px-3 py-2"
              placeholder="example@email.com"
            />
            {fieldErrors.email ? <p className="mt-1 text-xs text-red-600">{fieldErrors.email}</p> : null}
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Password</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded border px-3 py-2"
            />
            {fieldErrors.password ? <p className="mt-1 text-xs text-red-600">{fieldErrors.password}</p> : null}
          </div>
          {error ? <p className="text-sm text-red-600">{error}</p> : null}
          <button
            type="submit"
            disabled={loading}
            className="w-full rounded bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {loading ? "Signing in..." : "Sign in"}
          </button>
        </form>
      </div>
    </div>
  );
}
