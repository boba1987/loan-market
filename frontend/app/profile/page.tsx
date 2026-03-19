"use client";

import { FormEvent, useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { apiFetch } from "@/lib/api";
import { saveAuth, useAuthState } from "@/lib/auth";
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

export default function ProfilePage() {
  const { auth, loading } = useAuthState(true);
  const role = auth?.role;
  const token = auth?.token;
  const email = auth?.email;
  const name = auth?.name;
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
        const endpoint = role === "user" ? "/api/user/me" : role === "bank" ? "/api/bank/me" : "/api/admin/me";
        const p = await apiFetch<UserProfile>(endpoint, {}, authForApi);
        setForm({
          name: p.name ?? "",
          dateOfBirth: p.dateOfBirth ?? "",
          maritalStatus: p.maritalStatus ?? "",
          yearsWorking: p.yearsWorking != null ? String(p.yearsWorking) : "",
          industry: p.industry ?? "",
        });
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load profile");
      }
    };
    void load();
  }, [email, name, role, token]);

  if (loading || !auth) return <div className="p-4">Loading...</div>;

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSaveError(null);
    const isUser = auth.role === "user";
    const nextErrors = {
      name: isUser ? required(form.name, "Name") ?? undefined : undefined,
      dateOfBirth: form.dateOfBirth ? isoDateError(form.dateOfBirth, "Date of birth") ?? undefined : undefined,
      yearsWorking: form.yearsWorking ? numberError(form.yearsWorking, "Years working", { min: 0 }) ?? undefined : undefined,
      industry: isUser ? required(form.industry, "Industry") ?? undefined : undefined,
    };
    setFieldErrors(nextErrors);
    if (Object.values(nextErrors).some(Boolean)) return;
    setSaving(true);
    try {
      const endpoint = auth.role === "user" ? "/api/user/me" : auth.role === "bank" ? "/api/bank/me" : "/api/admin/me";
      const payload =
        auth.role === "user"
          ? {
              name: form.name || undefined,
              dateOfBirth: form.dateOfBirth || undefined,
              maritalStatus: form.maritalStatus || undefined,
              yearsWorking: form.yearsWorking ? Number(form.yearsWorking) : undefined,
              industry: form.industry || undefined,
            }
          : {
              name: form.name || undefined,
            };
      const updated = await apiFetch<UserProfile>(
        endpoint,
        {
          method: "PUT",
          body: JSON.stringify(payload),
        },
        auth,
      );
      const nextName = updated.name || form.name || auth.name;
      if (nextName && nextName !== auth.name) {
        saveAuth({ ...auth, name: nextName });
      }
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
          {auth.role === "user" ? (
            <>
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
                  <option value="not married">Not married</option>
                  <option value="married">Married</option>
                  <option value="divorced">Divorced</option>
                  <option value="other">Other</option>
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
            </>
          ) : null}
          {saveError ? <p className="text-sm text-red-600">{saveError}</p> : null}
          <button
            type="submit"
            disabled={saving}
            className="rounded bg-blue-600 px-4 py-2 text-white transition-colors hover:bg-blue-700 disabled:opacity-50"
          >
            {saving ? "Saving..." : "Save profile"}
          </button>
        </form>
    </AppShell>
  );
}
