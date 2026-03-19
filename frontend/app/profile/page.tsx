"use client";

import { FormEvent, useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { apiFetch } from "@/lib/api";
import { useAuthState } from "@/lib/auth";

type UserProfile = {
  role: string;
  name: string;
  email: string;
  dateOfBirth?: string | null;
  married?: boolean | null;
  yearsWorking?: number | null;
  industry?: string | null;
};

type Profile = Record<string, unknown> | UserProfile;

export default function ProfilePage() {
  const { auth, loading } = useAuthState(true);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    name: "",
    dateOfBirth: "",
    married: "false",
    yearsWorking: "",
    industry: "",
  });

  useEffect(() => {
    const load = async () => {
      if (!auth) return;
      setError(null);
      try {
        if (auth.role === "user") {
          const p = await apiFetch<UserProfile>("/api/user/me", {}, auth);
          setProfile(p);
          setForm({
            name: p.name ?? "",
            dateOfBirth: p.dateOfBirth ?? "",
            married: String(Boolean(p.married)),
            yearsWorking: p.yearsWorking != null ? String(p.yearsWorking) : "",
            industry: p.industry ?? "",
          });
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

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (auth.role !== "user") return;
    setSaveError(null);
    setSaving(true);
    try {
      const updated = await apiFetch<UserProfile>(
        "/api/user/me",
        {
          method: "PUT",
          body: JSON.stringify({
            name: form.name || undefined,
            dateOfBirth: form.dateOfBirth || undefined,
            married: form.married === "true",
            yearsWorking: form.yearsWorking ? Number(form.yearsWorking) : undefined,
            industry: form.industry || undefined,
          }),
        },
        auth,
      );
      setProfile(updated);
    } catch (err) {
      setSaveError(err instanceof Error ? err.message : "Failed to save profile");
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppShell auth={auth}>
      <h1 className="mb-4 text-2xl font-semibold">Profile</h1>
      {error ? <p className="mb-3 text-sm text-red-600">{error}</p> : null}
      {auth.role === "user" ? (
        <form onSubmit={onSubmit} className="max-w-xl space-y-3 rounded border bg-white p-4">
          <div>
            <label className="mb-1 block text-sm font-medium">Name</label>
            <input
              className="w-full rounded border px-3 py-2"
              value={form.name}
              onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Email</label>
            <input className="w-full rounded border bg-zinc-100 px-3 py-2" value={auth.email} disabled />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Date of birth</label>
            <input
              className="w-full rounded border px-3 py-2"
              placeholder="YYYY-MM-DD"
              value={form.dateOfBirth}
              onChange={(e) => setForm((p) => ({ ...p, dateOfBirth: e.target.value }))}
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Married</label>
            <select
              className="w-full rounded border px-3 py-2"
              value={form.married}
              onChange={(e) => setForm((p) => ({ ...p, married: e.target.value }))}
            >
              <option value="true">true</option>
              <option value="false">false</option>
            </select>
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Years working</label>
            <input
              className="w-full rounded border px-3 py-2"
              value={form.yearsWorking}
              onChange={(e) => setForm((p) => ({ ...p, yearsWorking: e.target.value }))}
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Industry</label>
            <input
              className="w-full rounded border px-3 py-2"
              value={form.industry}
              onChange={(e) => setForm((p) => ({ ...p, industry: e.target.value }))}
            />
          </div>
          {saveError ? <p className="text-sm text-red-600">{saveError}</p> : null}
          <button
            type="submit"
            disabled={saving}
            className="rounded bg-blue-600 px-4 py-2 text-white transition-colors hover:bg-blue-700 disabled:opacity-50"
          >
            {saving ? "Saving..." : "Save profile"}
          </button>
        </form>
      ) : (
        <div className="rounded border bg-white p-4">
          <pre className="overflow-auto text-sm">{JSON.stringify(profile, null, 2)}</pre>
        </div>
      )}
    </AppShell>
  );
}
