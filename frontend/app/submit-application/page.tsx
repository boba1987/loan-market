"use client";

import { FormEvent, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { apiFetch } from "@/lib/api";
import { useAuthState } from "@/lib/auth";

export default function SubmitApplicationPage() {
  const { auth, loading } = useAuthState(true);
  const [amount, setAmount] = useState("");
  const [income, setIncome] = useState("");
  const [debt, setDebt] = useState("");
  const [resultId, setResultId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  if (loading || !auth) return <div className="p-4">Loading...</div>;
  if (auth.role !== "user") {
    return (
      <AppShell auth={auth}>
        <p className="rounded bg-yellow-50 p-3 text-sm">This page is available only to user role.</p>
      </AppShell>
    );
  }

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setResultId(null);
    try {
      const result = await apiFetch<{ id: number }>(
        "/api/user/credit-applications",
        {
          method: "POST",
          body: JSON.stringify({
            amount: Number(amount),
            income: Number(income),
            debt: Number(debt),
          }),
        },
        auth,
      );
      setResultId(result.id);
      setAmount("");
      setIncome("");
      setDebt("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to submit application");
    }
  };

  return (
    <AppShell auth={auth}>
      <h1 className="mb-4 text-2xl font-semibold">Submit Application</h1>
      <form onSubmit={onSubmit} className="max-w-xl space-y-3 rounded border bg-white p-4">
        <div>
          <label className="mb-1 block text-sm font-medium">Amount</label>
          <input className="w-full rounded border px-3 py-2" value={amount} onChange={(e) => setAmount(e.target.value)} required />
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium">Income</label>
          <input className="w-full rounded border px-3 py-2" value={income} onChange={(e) => setIncome(e.target.value)} required />
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium">Debt</label>
          <input className="w-full rounded border px-3 py-2" value={debt} onChange={(e) => setDebt(e.target.value)} required />
        </div>
        {error ? <p className="text-sm text-red-600">{error}</p> : null}
        {resultId ? <p className="text-sm text-green-700">Application submitted with id {resultId}.</p> : null}
        <button className="rounded bg-blue-600 px-4 py-2 text-white" type="submit">
          Submit
        </button>
      </form>
    </AppShell>
  );
}
