"use client";

import { FormEvent, useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { apiFetch } from "@/lib/api";
import { useAuthState } from "@/lib/auth";
import { emailError, formatIsoDateInput, isoDateError, numberError, required } from "@/lib/validation";

type UserRecord = {
  id: number;
  email: string;
  name: string;
  role: "user" | "bank" | "admin";
  dateOfBirth?: string | null;
  maritalStatus?: "not married" | "married" | "divorced" | "other" | null;
  yearsWorking?: number | null;
  industry?: string | null;
};

export default function UsersPage() {
  const { auth, loading } = useAuthState(true);
  const [users, setUsers] = useState<UserRecord[]>([]);
  const [editForms, setEditForms] = useState<
    Record<
      number,
      {
        email: string;
        name: string;
        role: "user" | "bank" | "admin";
        dateOfBirth: string;
        maritalStatus: "" | "not married" | "married" | "divorced" | "other";
        yearsWorking: string;
        industry: string;
      }
    >
  >({});
  const [roleFilter, setRoleFilter] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [createErrors, setCreateErrors] = useState<Record<string, string | undefined>>({});
  const [editErrors, setEditErrors] = useState<Record<number, Record<string, string | undefined>>>({});
  const [pendingDeleteUserId, setPendingDeleteUserId] = useState<number | null>(null);

  const [createForm, setCreateForm] = useState({
    email: "",
    password: "",
    confirmPassword: "",
    role: "user",
    name: "",
    dateOfBirth: "",
    maritalStatus: "",
    yearsWorking: "",
    industry: "",
  });

  const load = async () => {
    if (!auth) return;
    setError(null);
    try {
      const qp = roleFilter ? `?role=${encodeURIComponent(roleFilter)}` : "";
      const result = await apiFetch<{ users: UserRecord[] }>(`/api/admin/users${qp}`, {}, auth);
      const filtered = result.users.filter((u) => u.email !== auth.email);
      setUsers(filtered);
      setEditForms(
        Object.fromEntries(
          filtered.map((u) => [
            u.id,
            {
              email: u.email,
              name: u.name ?? "",
              role: u.role,
              dateOfBirth: u.dateOfBirth ?? "",
              maritalStatus: (u.maritalStatus ?? "") as "" | "not married" | "married" | "divorced" | "other",
              yearsWorking: u.yearsWorking != null ? String(u.yearsWorking) : "",
              industry: u.industry ?? "",
            },
          ]),
        ),
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load users");
    }
  };

  useEffect(() => {
    if (auth?.role === "admin" && auth?.token) {
      void load();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [auth?.role, auth?.token, auth?.email, roleFilter]);

  if (loading || !auth) return <div className="p-4">Loading...</div>;
  if (auth.role !== "admin") {
    return (
      <AppShell auth={auth}>
        <p className="rounded bg-yellow-50 p-3 text-sm">This page is available only to admin role.</p>
      </AppShell>
    );
  }

  const submitCreate = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    const nextErrors = {
      email: emailError(createForm.email) ?? undefined,
      password: required(createForm.password, "Password") ?? undefined,
      confirmPassword:
        required(createForm.confirmPassword, "Confirm password") ??
        (createForm.password !== createForm.confirmPassword ? "Passwords do not match" : undefined),
      name: required(createForm.name, "Name") ?? undefined,
      dateOfBirth: createForm.dateOfBirth ? isoDateError(createForm.dateOfBirth, "Date of birth") ?? undefined : undefined,
      yearsWorking: createForm.yearsWorking ? numberError(createForm.yearsWorking, "Years working", { min: 0 }) ?? undefined : undefined,
      industry: createForm.industry ? undefined : undefined,
    };
    setCreateErrors(nextErrors);
    if (Object.values(nextErrors).some(Boolean)) return;
    try {
      await apiFetch("/api/admin/users", {
        method: "POST",
        body: JSON.stringify({
          email: createForm.email,
          password: createForm.password,
          role: createForm.role,
          name: createForm.name || undefined,
          dateOfBirth: createForm.dateOfBirth || undefined,
          maritalStatus: createForm.maritalStatus || undefined,
          yearsWorking: createForm.yearsWorking ? Number(createForm.yearsWorking) : undefined,
          industry: createForm.industry || undefined,
        }),
      }, auth);
      setCreateForm({
        email: "",
        password: "",
        confirmPassword: "",
        role: "user",
        name: "",
        dateOfBirth: "",
        maritalStatus: "",
        yearsWorking: "",
        industry: "",
      });
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create user");
    }
  };

  const deleteUser = async (id: number) => {
    setError(null);
    try {
      await apiFetch(`/api/admin/users/${id}`, { method: "DELETE" }, auth);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete user");
    }
  };

  const updateUser = async (id: number) => {
    if (!auth) return;
    const form = editForms[id];
    if (!form) return;
    const rowErrors = {
      email: emailError(form.email) ?? undefined,
      name: required(form.name, "Name") ?? undefined,
      dateOfBirth: form.dateOfBirth ? isoDateError(form.dateOfBirth, "Date of birth") ?? undefined : undefined,
      yearsWorking: form.yearsWorking ? numberError(form.yearsWorking, "Years working", { min: 0 }) ?? undefined : undefined,
    };
    setEditErrors((prev) => ({ ...prev, [id]: rowErrors }));
    if (Object.values(rowErrors).some(Boolean)) return;
    setError(null);
    try {
      await apiFetch(
        `/api/admin/users/${id}`,
        {
          method: "PUT",
          body: JSON.stringify({
            email: form.email,
            name: form.name,
            role: form.role,
            dateOfBirth: form.dateOfBirth || undefined,
            maritalStatus: form.maritalStatus || undefined,
            yearsWorking: form.yearsWorking ? Number(form.yearsWorking) : undefined,
            industry: form.industry || undefined,
          }),
        },
        auth,
      );
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update user");
    }
  };

  return (
    <AppShell auth={auth}>
      <h1 className="mb-4 text-2xl font-semibold">Users (Admin)</h1>
      {error ? <p className="mb-3 text-sm text-red-600">{error}</p> : null}

      <section className="mb-6 rounded border bg-white p-4">
        <h2 className="mb-3 text-lg font-medium">Create User</h2>
        <form onSubmit={submitCreate} className="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-3">
          <div>
            <label className="text-xs font-medium">
              Email
              <input className="mt-1 w-full rounded border px-2 py-2" placeholder="Email" value={createForm.email} onChange={(e) => setCreateForm((p) => ({ ...p, email: e.target.value }))} />
            </label>
            {createErrors.email ? <p className="mt-1 text-xs text-red-600">{createErrors.email}</p> : null}
          </div>
          <div>
            <label className="text-xs font-medium">
              Password
              <input className="mt-1 w-full rounded border px-2 py-2" placeholder="Password" type="password" value={createForm.password} onChange={(e) => setCreateForm((p) => ({ ...p, password: e.target.value }))} />
            </label>
            {createErrors.password ? <p className="mt-1 text-xs text-red-600">{createErrors.password}</p> : null}
          </div>
          <div>
            <label className="text-xs font-medium">
              Confirm password
              <input className="mt-1 w-full rounded border px-2 py-2" placeholder="Confirm password" type="password" value={createForm.confirmPassword} onChange={(e) => setCreateForm((p) => ({ ...p, confirmPassword: e.target.value }))} />
            </label>
            {createErrors.confirmPassword ? <p className="mt-1 text-xs text-red-600">{createErrors.confirmPassword}</p> : null}
          </div>
          <div>
            <label className="text-xs font-medium">
              Role
              <select className="mt-1 w-full rounded border px-2 py-2" value={createForm.role} onChange={(e) => setCreateForm((p) => ({ ...p, role: e.target.value }))}>
                <option value="user">user</option>
                <option value="bank">bank</option>
                <option value="admin">admin</option>
              </select>
            </label>
          </div>
          <div>
            <label className="text-xs font-medium">
              Name
              <input className="mt-1 w-full rounded border px-2 py-2" placeholder="Name" value={createForm.name} onChange={(e) => setCreateForm((p) => ({ ...p, name: e.target.value }))} />
            </label>
            {createErrors.name ? <p className="mt-1 text-xs text-red-600">{createErrors.name}</p> : null}
          </div>
          <div>
            <label className="text-xs font-medium">
              Date of birth
              <input className="mt-1 w-full rounded border px-2 py-2" placeholder="Date of birth YYYY-MM-DD" value={createForm.dateOfBirth} onChange={(e) => setCreateForm((p) => ({ ...p, dateOfBirth: formatIsoDateInput(e.target.value) }))} />
            </label>
            {createErrors.dateOfBirth ? <p className="mt-1 text-xs text-red-600">{createErrors.dateOfBirth}</p> : null}
          </div>
          <div>
            <label className="text-xs font-medium">
              Marital status
              <select className="mt-1 w-full rounded border px-2 py-2" value={createForm.maritalStatus} onChange={(e) => setCreateForm((p) => ({ ...p, maritalStatus: e.target.value }))}>
                <option value="" disabled>
                  Select...
                </option>
                <option value="not married">not married</option>
                <option value="married">married</option>
                <option value="divorced">divorced</option>
                <option value="other">other</option>
              </select>
            </label>
          </div>
          <div>
            <label className="text-xs font-medium">
              Years working
              <input type="number" min={0} className="mt-1 w-full rounded border px-2 py-2" placeholder="Years working" value={createForm.yearsWorking} onChange={(e) => setCreateForm((p) => ({ ...p, yearsWorking: e.target.value }))} />
            </label>
            {createErrors.yearsWorking ? <p className="mt-1 text-xs text-red-600">{createErrors.yearsWorking}</p> : null}
          </div>
          <div>
            <label className="text-xs font-medium">
              Industry
              <input className="mt-1 w-full rounded border px-2 py-2" placeholder="Industry" value={createForm.industry} onChange={(e) => setCreateForm((p) => ({ ...p, industry: e.target.value }))} />
            </label>
          </div>
          <div className="md:col-span-2 lg:col-span-3">
            <button type="submit" className="rounded bg-blue-600 px-3 py-2 text-white">Create</button>
          </div>
        </form>
      </section>

      <section className="rounded border bg-white p-4">
        <div className="mb-3 flex items-center gap-2">
          <label className="text-sm">Filter role:</label>
          <select className="rounded border px-2 py-1" value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}>
            <option value="">all</option>
            <option value="user">user</option>
            <option value="bank">bank</option>
            <option value="admin">admin</option>
          </select>
        </div>
        <div className="space-y-3">
          {users.map((u) => (
            <div key={u.id} className="rounded border p-3 text-sm">
              <p className="mb-2"><strong>ID:</strong> {u.id}</p>
              <div className="mb-2 grid grid-cols-1 gap-2 md:grid-cols-2">
                <label className="text-xs font-medium">
                  Email
                  <input
                    className="mt-1 w-full rounded border px-2 py-1"
                    value={editForms[u.id]?.email ?? ""}
                    onChange={(e) =>
                      setEditForms((prev) => ({
                        ...prev,
                        [u.id]: { ...prev[u.id], email: e.target.value },
                      }))
                    }
                  />
                </label>
                {editErrors[u.id]?.email ? <p className="text-xs text-red-600 md:col-span-2">{editErrors[u.id]?.email}</p> : null}
                <label className="text-xs font-medium">
                  Name
                  <input
                    className="mt-1 w-full rounded border px-2 py-1"
                    value={editForms[u.id]?.name ?? ""}
                    onChange={(e) =>
                      setEditForms((prev) => ({
                        ...prev,
                        [u.id]: { ...prev[u.id], name: e.target.value },
                      }))
                    }
                  />
                </label>
                {editErrors[u.id]?.name ? <p className="text-xs text-red-600 md:col-span-2">{editErrors[u.id]?.name}</p> : null}
                <label className="text-xs font-medium">
                  Role
                  <select
                    className="mt-1 w-full rounded border px-2 py-1"
                    value={editForms[u.id]?.role ?? "user"}
                    onChange={(e) =>
                      setEditForms((prev) => ({
                        ...prev,
                        [u.id]: { ...prev[u.id], role: e.target.value as UserRecord["role"] },
                      }))
                    }
                  >
                    <option value="user">user</option>
                    <option value="bank">bank</option>
                    <option value="admin">admin</option>
                  </select>
                </label>
                <label className="text-xs font-medium">
                  Date of birth
                  <input
                    className="mt-1 w-full rounded border px-2 py-1"
                    placeholder="YYYY-MM-DD"
                    value={editForms[u.id]?.dateOfBirth ?? ""}
                    onChange={(e) =>
                      setEditForms((prev) => ({
                        ...prev,
                        [u.id]: { ...prev[u.id], dateOfBirth: formatIsoDateInput(e.target.value) },
                      }))
                    }
                  />
                </label>
                {editErrors[u.id]?.dateOfBirth ? <p className="text-xs text-red-600 md:col-span-2">{editErrors[u.id]?.dateOfBirth}</p> : null}
                <label className="text-xs font-medium">
                  Marital status
                  <select
                    className="mt-1 w-full rounded border px-2 py-1"
                    value={editForms[u.id]?.maritalStatus ?? ""}
                    onChange={(e) =>
                      setEditForms((prev) => ({
                        ...prev,
                        [u.id]: { ...prev[u.id], maritalStatus: e.target.value as "" | "not married" | "married" | "divorced" | "other" },
                      }))
                    }
                  >
                    <option value="">Select...</option>
                    <option value="not married">not married</option>
                    <option value="married">married</option>
                    <option value="divorced">divorced</option>
                    <option value="other">other</option>
                  </select>
                </label>
                <label className="text-xs font-medium">
                  Years working
                  <input
                    className="mt-1 w-full rounded border px-2 py-1"
                    type="number"
                    min={0}
                    placeholder="Years working"
                    value={editForms[u.id]?.yearsWorking ?? ""}
                    onChange={(e) =>
                      setEditForms((prev) => ({
                        ...prev,
                        [u.id]: { ...prev[u.id], yearsWorking: e.target.value },
                      }))
                    }
                  />
                </label>
                {editErrors[u.id]?.yearsWorking ? <p className="text-xs text-red-600 md:col-span-2">{editErrors[u.id]?.yearsWorking}</p> : null}
                <label className="text-xs font-medium">
                  Industry
                  <input
                    className="mt-1 w-full rounded border px-2 py-1"
                    placeholder="Industry"
                    value={editForms[u.id]?.industry ?? ""}
                    onChange={(e) =>
                      setEditForms((prev) => ({
                        ...prev,
                        [u.id]: { ...prev[u.id], industry: e.target.value },
                      }))
                    }
                  />
                </label>
              </div>
              <div className="flex gap-2">
                <button onClick={() => void updateUser(u.id)} className="rounded bg-blue-600 px-3 py-1 text-white">
                  Save
                </button>
                <button onClick={() => setPendingDeleteUserId(u.id)} className="rounded bg-red-600 px-3 py-1 text-white">
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>

      {pendingDeleteUserId != null ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="w-full max-w-md rounded bg-white p-5 shadow-lg">
            <h2 className="mb-2 text-lg font-semibold">Confirm delete</h2>
            <p className="mb-4 text-sm text-zinc-700">
              Delete user ID {pendingDeleteUserId}? This action cannot be undone.
            </p>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setPendingDeleteUserId(null)}
                className="rounded border px-3 py-2 text-sm"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  void deleteUser(pendingDeleteUserId);
                  setPendingDeleteUserId(null);
                }}
                className="rounded bg-red-600 px-3 py-2 text-sm text-white hover:bg-red-700"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </AppShell>
  );
}
