"use client";

import { FormEvent, useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { apiFetch } from "@/lib/api";
import { useAuthState } from "@/lib/auth";

type UserRecord = {
  id: number;
  email: string;
  name: string;
  role: "user" | "bank" | "admin";
  dateOfBirth?: string | null;
  married?: boolean | null;
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
        married: "true" | "false";
        yearsWorking: string;
        industry: string;
      }
    >
  >({});
  const [roleFilter, setRoleFilter] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pendingDeleteUserId, setPendingDeleteUserId] = useState<number | null>(null);

  const [createForm, setCreateForm] = useState({
    email: "",
    password: "",
    role: "user",
    name: "",
    dateOfBirth: "",
    married: "true",
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
              married: String(Boolean(u.married)) as "true" | "false",
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
    if (auth?.role === "admin") {
      void load();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [auth, roleFilter]);

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
    try {
      await apiFetch("/api/admin/users", {
        method: "POST",
        body: JSON.stringify({
          email: createForm.email,
          password: createForm.password,
          role: createForm.role,
          name: createForm.name || undefined,
          dateOfBirth: createForm.dateOfBirth || undefined,
          married: createForm.married === "true",
          yearsWorking: createForm.yearsWorking ? Number(createForm.yearsWorking) : undefined,
          industry: createForm.industry || undefined,
        }),
      }, auth);
      setCreateForm({
        email: "",
        password: "",
        role: "user",
        name: "",
        dateOfBirth: "",
        married: "true",
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
            married: form.married === "true",
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
        <form onSubmit={submitCreate} className="grid grid-cols-1 gap-2 md:grid-cols-3">
          <input className="rounded border px-2 py-2" placeholder="Email" value={createForm.email} onChange={(e) => setCreateForm((p) => ({ ...p, email: e.target.value }))} />
          <input className="rounded border px-2 py-2" placeholder="Password" type="password" value={createForm.password} onChange={(e) => setCreateForm((p) => ({ ...p, password: e.target.value }))} />
          <select className="rounded border px-2 py-2" value={createForm.role} onChange={(e) => setCreateForm((p) => ({ ...p, role: e.target.value }))}>
            <option value="user">user</option>
            <option value="bank">bank</option>
            <option value="admin">admin</option>
          </select>
          <input className="rounded border px-2 py-2" placeholder="Name" value={createForm.name} onChange={(e) => setCreateForm((p) => ({ ...p, name: e.target.value }))} />
          <input className="rounded border px-2 py-2" placeholder="Date of birth YYYY-MM-DD" value={createForm.dateOfBirth} onChange={(e) => setCreateForm((p) => ({ ...p, dateOfBirth: e.target.value }))} />
          <select className="rounded border px-2 py-2" value={createForm.married} onChange={(e) => setCreateForm((p) => ({ ...p, married: e.target.value }))}>
            <option value="true">Married true</option>
            <option value="false">Married false</option>
          </select>
          <input className="rounded border px-2 py-2" placeholder="Years working" value={createForm.yearsWorking} onChange={(e) => setCreateForm((p) => ({ ...p, yearsWorking: e.target.value }))} />
          <input className="rounded border px-2 py-2 md:col-span-2" placeholder="Industry" value={createForm.industry} onChange={(e) => setCreateForm((p) => ({ ...p, industry: e.target.value }))} />
          <button type="submit" className="rounded bg-blue-600 px-3 py-2 text-white">Create</button>
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
                <input
                  className="rounded border px-2 py-1"
                  value={editForms[u.id]?.email ?? ""}
                  onChange={(e) =>
                    setEditForms((prev) => ({
                      ...prev,
                      [u.id]: { ...prev[u.id], email: e.target.value },
                    }))
                  }
                />
                <input
                  className="rounded border px-2 py-1"
                  value={editForms[u.id]?.name ?? ""}
                  onChange={(e) =>
                    setEditForms((prev) => ({
                      ...prev,
                      [u.id]: { ...prev[u.id], name: e.target.value },
                    }))
                  }
                />
                <select
                  className="rounded border px-2 py-1"
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
                <input
                  className="rounded border px-2 py-1"
                  placeholder="YYYY-MM-DD"
                  value={editForms[u.id]?.dateOfBirth ?? ""}
                  onChange={(e) =>
                    setEditForms((prev) => ({
                      ...prev,
                      [u.id]: { ...prev[u.id], dateOfBirth: e.target.value },
                    }))
                  }
                />
                <select
                  className="rounded border px-2 py-1"
                  value={editForms[u.id]?.married ?? "false"}
                  onChange={(e) =>
                    setEditForms((prev) => ({
                      ...prev,
                      [u.id]: { ...prev[u.id], married: e.target.value as "true" | "false" },
                    }))
                  }
                >
                  <option value="true">married true</option>
                  <option value="false">married false</option>
                </select>
                <input
                  className="rounded border px-2 py-1"
                  placeholder="Years working"
                  value={editForms[u.id]?.yearsWorking ?? ""}
                  onChange={(e) =>
                    setEditForms((prev) => ({
                      ...prev,
                      [u.id]: { ...prev[u.id], yearsWorking: e.target.value },
                    }))
                  }
                />
                <input
                  className="rounded border px-2 py-1 md:col-span-2"
                  placeholder="Industry"
                  value={editForms[u.id]?.industry ?? ""}
                  onChange={(e) =>
                    setEditForms((prev) => ({
                      ...prev,
                      [u.id]: { ...prev[u.id], industry: e.target.value },
                    }))
                  }
                />
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
