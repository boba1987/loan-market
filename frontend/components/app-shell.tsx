"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { clearAuth, type AuthState } from "@/lib/auth";

function navItem(href: string, label: string, pathname: string) {
  const active = pathname === href;
  return (
    <Link
      key={href}
      href={href}
      className={`rounded px-3 py-2 text-sm ${
        active ? "bg-blue-600 text-white" : "bg-zinc-100 hover:bg-zinc-200"
      }`}
    >
      {label}
    </Link>
  );
}

export function AppShell({
  auth,
  children,
}: {
  auth: AuthState;
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const router = useRouter();

  const links = [
    { href: "/loan-applications", label: "Loan Applications", roles: ["user", "bank", "admin"] },
    { href: "/profile", label: "Profile", roles: ["user", "bank", "admin"] },
    { href: "/users", label: "Users", roles: ["admin"] },
    { href: "/submit-application", label: "Submit Application", roles: ["user"] },
    { href: "/admin-credit-applications", label: "Admin Applications", roles: ["admin"] },
  ].filter((l) => l.roles.includes(auth.role));

  return (
    <div className="min-h-screen bg-zinc-50 text-zinc-900">
      <header className="border-b bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-3 px-4 py-3">
          <div className="text-sm">
            <strong>{auth.name || auth.email}</strong>
            <span className="ml-2 rounded bg-zinc-100 px-2 py-1 text-xs uppercase">{auth.role}</span>
          </div>
          <button
            type="button"
            onClick={() => {
              clearAuth();
              router.replace("/login");
            }}
            className="rounded bg-red-600 px-3 py-2 text-sm text-white hover:bg-red-700"
          >
            Logout
          </button>
        </div>
        <nav className="mx-auto flex max-w-6xl flex-wrap gap-2 px-4 pb-3">
          {links.map((l) => navItem(l.href, l.label, pathname))}
        </nav>
      </header>
      <main className="mx-auto max-w-6xl p-4">{children}</main>
    </div>
  );
}
