"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { useAuthState } from "@/lib/auth";
import { apiFetch } from "@/lib/api";

type Offer = {
  bankName?: string;
  bankEmail?: string;
  interestRate: number;
  repaymentPeriod: number;
};

type LoanApplication = {
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
  createdAt: number;
  interestRate?: number;
  repaymentPeriod?: number;
  offers?: Offer[];
};

type ApplicationResponse = {
  items: LoanApplication[];
  page: number;
  pageSize: number;
  total: number;
};

const numberFormatter = new Intl.NumberFormat("en-US", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

function formatAmount(value: number): string {
  return numberFormatter.format(value);
}

export default function LoanApplicationsPage() {
  const { auth, loading } = useAuthState(true);
  const [data, setData] = useState<ApplicationResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submittingFor, setSubmittingFor] = useState<number | null>(null);
  const [pendingDeleteApplicationId, setPendingDeleteApplicationId] = useState<number | null>(null);
  const [offerForm, setOfferForm] = useState<Record<number, { interestRate: string; repaymentPeriod: string }>>({});

  const endpoint = useMemo(() => {
    if (!auth) return null;
    if (auth.role === "user") return "/api/user/credit-applications?page=1&pageSize=50";
    if (auth.role === "bank") return "/api/bank/credit-applications?page=1&pageSize=50";
    return "/api/admin/credit-applications?page=1&pageSize=50";
  }, [auth]);

  const load = async () => {
    if (!auth || !endpoint) return;
    setError(null);
    try {
      const response = await apiFetch<ApplicationResponse>(endpoint, {}, auth);
      setData(response);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load applications");
    }
  };

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [endpoint, auth?.token]);

  const submitOffer = async (applicationId: number, e: FormEvent) => {
    e.preventDefault();
    if (!auth || auth.role !== "bank") return;
    const values = offerForm[applicationId];
    if (!values?.interestRate || !values?.repaymentPeriod) return;
    setSubmittingFor(applicationId);
    setError(null);
    try {
      await apiFetch(`/api/bank/credit-applications/${applicationId}/offer`, {
        method: "POST",
        body: JSON.stringify({
          interestRate: Number(values.interestRate),
          repaymentPeriod: Number(values.repaymentPeriod),
        }),
      }, auth);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to submit offer");
    } finally {
      setSubmittingFor(null);
    }
  };

  const deleteApplication = async (applicationId: number) => {
    if (!auth || auth.role !== "admin") return;
    setError(null);
    try {
      await apiFetch(`/api/admin/credit-applications/${applicationId}`, { method: "DELETE" }, auth);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete application");
    }
  };

  if (loading || !auth) return <div className="p-4">Loading...</div>;

  return (
    <AppShell auth={auth}>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Loan Applications</h1>
        <button
          onClick={() => void load()}
          className="rounded bg-zinc-800 px-3 py-2 text-sm text-white transition-colors hover:bg-zinc-700"
        >
          Refresh
        </button>
      </div>
      {error ? <p className="mb-3 text-sm text-red-600">{error}</p> : null}
      <div className="space-y-4">
        {data?.items?.map((app) => (
          <section key={app.id} className="rounded border bg-white p-4">
            <div className="grid grid-cols-1 gap-2 text-sm md:grid-cols-2">
              <p><strong>ID:</strong> {app.id}</p>
              <p><strong>Name:</strong> {app.name}</p>
              <p><strong>Email:</strong> {app.email}</p>
              <p><strong>Amount:</strong> {formatAmount(app.amount)}</p>
              <p><strong>Income:</strong> {formatAmount(app.yearlyIncome)}</p>
              <p><strong>Debt:</strong> {formatAmount(app.debt)}</p>
              <p><strong>Date of birth:</strong> {app.dateOfBirth}</p>
              <p><strong>Married:</strong> {String(app.married)}</p>
              <p><strong>Years working:</strong> {app.yearsWorking}</p>
              <p><strong>Industry:</strong> {app.industry}</p>
            </div>

            {auth.role === "bank" ? (
              <div className="mt-3">
                <p className="mb-2 text-sm">
                  <strong>Your offer:</strong>{" "}
                  {app.interestRate != null
                    ? `${app.interestRate}% / ${app.repaymentPeriod} months`
                    : "No offer yet"}
                </p>
                <form className="flex flex-wrap items-end gap-2" onSubmit={(e) => void submitOffer(app.id, e)}>
                  <div>
                    <label className="mb-1 block text-xs">Interest rate</label>
                    <input
                      type="number"
                      step="0.01"
                      className="rounded border px-2 py-1"
                      value={offerForm[app.id]?.interestRate ?? ""}
                      onChange={(e) =>
                        setOfferForm((prev) => ({
                          ...prev,
                          [app.id]: { ...prev[app.id], interestRate: e.target.value },
                        }))
                      }
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs">Repayment period (months)</label>
                    <input
                      type="number"
                      className="rounded border px-2 py-1"
                      value={offerForm[app.id]?.repaymentPeriod ?? ""}
                      onChange={(e) =>
                        setOfferForm((prev) => ({
                          ...prev,
                          [app.id]: { ...prev[app.id], repaymentPeriod: e.target.value },
                        }))
                      }
                    />
                  </div>
                  <button
                    type="submit"
                    disabled={submittingFor === app.id}
                    className="rounded bg-blue-600 px-3 py-2 text-sm text-white"
                  >
                    {submittingFor === app.id ? "Saving..." : "Submit / Update Offer"}
                  </button>
                </form>
              </div>
            ) : null}

            {auth.role === "admin" ? (
              <div className="mt-3">
                <p className="mb-2 text-sm font-semibold">Offers</p>
                {app.offers?.length ? (
                  <ul className="list-disc pl-6 text-sm">
                    {app.offers.map((offer, i) => (
                      <li key={`${app.id}-${i}`}>
                        {offer.bankName} ({offer.bankEmail}) - {offer.interestRate}% / {offer.repaymentPeriod} months
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="text-sm text-zinc-600">No offers yet.</p>
                )}
                <button
                  type="button"
                  onClick={() => setPendingDeleteApplicationId(app.id)}
                  className="mt-3 rounded bg-red-600 px-3 py-2 text-sm text-white transition-colors hover:bg-red-700"
                >
                  Delete Application
                </button>
              </div>
            ) : null}

            {auth.role === "user" ? (
              <div className="mt-3">
                <p className="mb-2 text-sm font-semibold">Offers</p>
                {app.offers?.length ? (
                  <ul className="list-disc pl-6 text-sm">
                    {app.offers.map((offer, i) => (
                      <li key={`${app.id}-user-${i}`}>
                        {offer.bankName} ({offer.bankEmail}) - {offer.interestRate}% / {offer.repaymentPeriod} months
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="text-sm text-zinc-600">No offers yet.</p>
                )}
              </div>
            ) : null}
          </section>
        ))}
      </div>
      {pendingDeleteApplicationId != null ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="w-full max-w-md rounded bg-white p-5 shadow-lg">
            <h2 className="mb-2 text-lg font-semibold">Confirm delete</h2>
            <p className="mb-4 text-sm text-zinc-700">
              Delete application ID {pendingDeleteApplicationId}? This action cannot be undone.
            </p>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setPendingDeleteApplicationId(null)}
                className="rounded border px-3 py-2 text-sm"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  void deleteApplication(pendingDeleteApplicationId);
                  setPendingDeleteApplicationId(null);
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
