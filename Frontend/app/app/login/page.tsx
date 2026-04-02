"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { API_BASE } from "../../lib/config";

interface LoginResponse {
  success: boolean;
  message: string;
  accessToken: string;
  refreshToken: string;
  user: {
    id: number;
    name: string;
    email: string;
    phone: string;
    code: string;
    permissionName: string | null;
  };
}

export default function LoginPage() {
  const router = useRouter();
  const [id, setId] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  // Redirect already-authenticated users away from the login page
  useEffect(() => {
    const token = localStorage.getItem("accessToken");
    const raw = localStorage.getItem("user");
    if (token && raw) {
      try {
        const user = JSON.parse(raw);
        const role = user?.permissionName;
        router.replace(role === "member" || role === "admin" ? "/users" : "/forbidden");
      } catch {
        // Corrupted data — let the user log in again
      }
    }
  }, [router]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const res = await fetch(`${API_BASE}/api/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ id: Number(id), password }),
      });

      const data: LoginResponse = await res.json();

      if (data.success) {
        localStorage.setItem("accessToken", data.accessToken);
        localStorage.setItem("refreshToken", data.refreshToken);
        localStorage.setItem("user", JSON.stringify(data.user));

        const role = data.user?.permissionName;
        if (role === "member" || role === "admin") {
          router.push("/users");
        } else {
          router.push("/forbidden");
        }
      } else {
        setError(data.message || "Đăng nhập thất bại.");
      }
    } catch {
      setError("Không thể kết nối tới server.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-zinc-100 dark:bg-zinc-900">
      <div className="w-full max-w-sm bg-white dark:bg-zinc-800 rounded-2xl shadow-lg p-8">
        <h1 className="text-2xl font-bold text-center text-zinc-800 dark:text-zinc-100 mb-6">
          Đăng nhập
        </h1>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label
              htmlFor="userId"
              className="block text-sm font-medium text-zinc-600 dark:text-zinc-300 mb-1"
            >
              ID người dùng
            </label>
            <input
              id="userId"
              type="number"
              value={id}
              onChange={(e) => setId(e.target.value)}
              required
              placeholder="Nhập ID"
              className="w-full rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-700 px-3 py-2 text-sm text-zinc-800 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label
              htmlFor="password"
              className="block text-sm font-medium text-zinc-600 dark:text-zinc-300 mb-1"
            >
              Mật khẩu
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              placeholder="Nhập mật khẩu"
              className="w-full rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-700 px-3 py-2 text-sm text-zinc-800 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {error && (
            <p className="text-sm text-red-500 text-center">{error}</p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="mt-2 w-full rounded-lg bg-blue-600 hover:bg-blue-700 disabled:opacity-60 text-white font-semibold py-2 transition-colors"
          >
            {loading ? "Đang đăng nhập..." : "Đăng nhập"}
          </button>
        </form>
      </div>
    </div>
  );
}
