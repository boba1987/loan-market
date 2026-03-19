"use client";

import { useEffect, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { apiFetch } from "@/lib/api";
import { useAuthState } from "@/lib/auth";

type Item = {
  id: number;
  name: string;
  email: string;
  amount: number;
  yearlyIncome: number;
  debt: number;
  dateOfBirth: string;
  married: boolean;
  yearsWorking: number;
  industry: string;
  offers?: Array<{
    bankName: string;
    bankEmail: string;
    interestRate: number;
    repaymentPeriod: number;
  }>;
};

export default function AdminCreditApplicationsPage() {
  const { auth, loading } = useAuthState(true);
  const [items, setItems] = useState<Item[]>([]);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    if (!auth) return;
    try {
      const result = await apiFetch<{ items: Item[] }>(
        "/api/admin/credit-applications?page=1&pageSize=100",
        {},
        auth,
      );
      setItems(result.items);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load applications");
    }
  };

  useEffect(() => {
    if (auth?.role === "admin") {
      void load();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [auth]);

  if (loading || !auth) return <div className="p-4">Loading...</div>;
  if (auth.role !== "admin") {
    return (
      <AppShell auth={auth}>
        <p className="rounded bg-yellow-50 p-3 text-sm">This page is available only to admin role.</p>
      </AppShell>
    );
  }

  const remove = async (id: number) => {
    setError(null);
    try {
      await apiFetch(`/api/admin/credit-applications/${id}`, { method: "DELETE" }, auth);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete application");
    }
  };

  return (
    <AppShell auth={auth}>
      <h1 className="mb-4 text-2xl font-semibold">Admin Credit Applications</h1>
      {error ? <p className="mb-3 text-sm text-red-600">{error}</p> : null}
      <div className="space-y-4">
        {items.map((item) => (
          <section key={item.id} className="rounded border bg-white p-4 text-sm">
            <div className="mb-2 grid grid-cols-1 gap-1 md:grid-cols-2">
              <p><strong>ID:</strong> {item.id}</p>
              <p><strong>Name:</strong> {item.name}</p>
              <p><strong>Email:</strong> {item.email}</p>
              <p><strong>Amount:</strong> {item.amount}</p>
            </div>
            {item.offers?.length ? (
              <ul className="mb-2 list-disc pl-5">
                {item.offers.map((o, idx) => (
                  <li key={`${item.id}-${idx}`}>
                    {o.bankName} ({o.bankEmail}) - {o.interestRate}% / {o.repaymentPeriod} months
                  </li>
                ))}
              </ul>
            ) : null}
            <button onClick={() => void remove(item.id)} className="rounded bg-red-600 px-3 py-1 text-white">
              Delete Application
            </button>
          </section>
        ))}
      </div>
    </AppShell>
  );
}
