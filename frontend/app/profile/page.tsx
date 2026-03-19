"use client";

import { FormEvent, useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { apiFetch } from "@/lib/api";
import { useAuthState } from "@/lib/auth";
import { formatIsoDateInput, isoDateError, numberError, required } from "@/lib/validation";

type UserProfile = {
  role: string;
  name: string;
  email: string;
  dateOfBirth?: string | null;
  maritalStatus?: "not married" | "married" | "divorced" | "other" | null;
  yearsWorking?: number | null;
  industry?: string | null;
};

type Profile = Record<string, unknown> | UserProfile;

export default function ProfilePage() {
  const { auth, loading } = useAuthState(true);
  const role = auth?.role;
  const token = auth?.token;
  const email = auth?.email;
  const name = auth?.name;
  const [profile, setProfile] = useState<Profile | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string | undefined>>({});
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    name: "",
    dateOfBirth: "",
    maritalStatus: "",
    yearsWorking: "",
    industry: "",
  });

  useEffect(() => {
    const authForApi = token && role && email ? { token, role, email, name } : null;
    const load = async () => {
      if (!authForApi) return;
      setError(null);
      try {
        if (role === "user") {
          const p = await apiFetch<UserProfile>("/api/user/me", {}, authForApi);
          setProfile(p);
          setForm({
            name: p.name ?? "",
            dateOfBirth: p.dateOfBirth ?? "",
            maritalStatus: p.maritalStatus ?? "",
            yearsWorking: p.yearsWorking != null ? String(p.yearsWorking) : "",
            industry: p.industry ?? "",
          });
          return;
        }
        if (role === "bank") {
          setProfile(await apiFetch<Profile>("/api/bank/me", {}, authForApi));
          return;
        }
        const users = await apiFetch<{ users: Array<Record<string, unknown>> }>(
          "/api/admin/users?role=admin",
          {},
          authForApi,
        );
        const me = users.users.find((u) => u.email === email) ?? null;
        setProfile((me as Profile) ?? { role, email, name });
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load profile");
      }
    };
    void load();
  }, [email, name, role, token]);

  if (loading || !auth) return <div className="p-4">Loading...</div>;

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (auth.role !== "user") return;
    setSaveError(null);
    const nextErrors = {
      name: required(form.name, "Name") ?? undefined,
      dateOfBirth: isoDateError(form.dateOfBirth, "Date of birth") ?? undefined,
      yearsWorking: numberError(form.yearsWorking, "Years working", { min: 0 }) ?? undefined,
      industry: required(form.industry, "Industry") ?? undefined,
    };
    setFieldErrors(nextErrors);
    if (Object.values(nextErrors).some(Boolean)) return;
    setSaving(true);
    try {
      const updated = await apiFetch<UserProfile>(
        "/api/user/me",
        {
          method: "PUT",
          body: JSON.stringify({
            name: form.name || undefined,
            dateOfBirth: form.dateOfBirth || undefined,
            maritalStatus: form.maritalStatus || undefined,
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
            {fieldErrors.name ? <p className="mt-1 text-xs text-red-600">{fieldErrors.name}</p> : null}
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
              onChange={(e) => setForm((p) => ({ ...p, dateOfBirth: formatIsoDateInput(e.target.value) }))}
            />
            {fieldErrors.dateOfBirth ? <p className="mt-1 text-xs text-red-600">{fieldErrors.dateOfBirth}</p> : null}
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Marital status</label>
            <select
              className="w-full rounded border px-3 py-2"
              value={form.maritalStatus}
              onChange={(e) => setForm((p) => ({ ...p, maritalStatus: e.target.value }))}
            >
              <option value="">Select...</option>
              <option value="not married">not married</option>
              <option value="married">married</option>
              <option value="divorced">divorced</option>
              <option value="other">other</option>
            </select>
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Years working</label>
            <input
              type="number"
              min={0}
              className="w-full rounded border px-3 py-2"
              value={form.yearsWorking}
              onChange={(e) => setForm((p) => ({ ...p, yearsWorking: e.target.value }))}
            />
            {fieldErrors.yearsWorking ? <p className="mt-1 text-xs text-red-600">{fieldErrors.yearsWorking}</p> : null}
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Industry</label>
            <input
              className="w-full rounded border px-3 py-2"
              value={form.industry}
              onChange={(e) => setForm((p) => ({ ...p, industry: e.target.value }))}
            />
            {fieldErrors.industry ? <p className="mt-1 text-xs text-red-600">{fieldErrors.industry}</p> : null}
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
