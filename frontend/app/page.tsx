"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { readAuth } from "@/lib/auth";

export default function Home() {
  const router = useRouter();

  useEffect(() => {
    const auth = readAuth();
    router.replace(auth ? "/loan-applications" : "/login");
  }, [router]);

  return <div className="p-8 text-sm text-zinc-600">Redirecting...</div>;
}
