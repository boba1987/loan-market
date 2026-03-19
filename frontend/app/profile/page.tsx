"use client";

import { useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { apiFetch } from "@/lib/api";
import { useAuthState } from "@/lib/auth";

type Profile = Record<string, unknown>;

export default function ProfilePage() {
  const { auth, loading } = useAuthState(true);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      if (!auth) return;
      setError(null);
      try {
        if (auth.role === "user") {
          setProfile(await apiFetch<Profile>("/api/user/me", {}, auth));
          return;
        }
        if (auth.role === "bank") {
          setProfile(await apiFetch<Profile>("/api/bank/me", {}, auth));
          return;
        }
        const users = await apiFetch<{ users: Array<Record<string, unknown>> }>(
          "/api/admin/users?role=admin",
          {},
          auth,
        );
        const me = users.users.find((u) => u.email === auth.email) ?? null;
        setProfile((me as Profile) ?? { role: auth.role, email: auth.email, name: auth.name });
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load profile");
      }
    };
    void load();
  }, [auth]);

  if (loading || !auth) return <div className="p-4">Loading...</div>;

  return (
    <AppShell auth={auth}>
      <h1 className="mb-4 text-2xl font-semibold">Profile</h1>
      {error ? <p className="mb-3 text-sm text-red-600">{error}</p> : null}
      <div className="rounded border bg-white p-4">
        <pre className="overflow-auto text-sm">{JSON.stringify(profile, null, 2)}</pre>
      </div>
    </AppShell>
  );
}
