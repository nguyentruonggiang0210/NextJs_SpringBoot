"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { API_BASE } from "../../lib/config";

const guestItems = [
  { label: "Trang chủ", href: "/" },
  { label: "Đăng nhập", href: "/login" },
];

const authItems = [
  { label: "Trang chủ", href: "/" },
  { label: "Người dùng", href: "/users" },
  { label: "Chat", href: "/chat" },
  { label: "Telemetry", href: "/telemetry" },
];

export default function Navbar() {
  const pathname = usePathname();
  const router = useRouter();
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  // Sync auth state from localStorage on every route change
  useEffect(() => {
    const token = localStorage.getItem("accessToken");
    const user = localStorage.getItem("user");
    setIsLoggedIn(!!token && !!user);
  }, [pathname]);

  /**
   * Calls the logout API to revoke the refresh token, then clears local
   * storage and redirects to the login page.
   */
  async function handleLogout() {
    const refreshToken = localStorage.getItem("refreshToken");
    if (refreshToken) {
      try {
        await fetch(`${API_BASE}/api/auth/logout`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken }),
        });
      } catch {
        // Proceed with local logout even if the request fails
      }
    }
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("user");
    setIsLoggedIn(false);
    router.push("/login");
  }

  const navItems = isLoggedIn ? authItems : guestItems;

  return (
    <nav className="w-full bg-white dark:bg-zinc-900 border-b border-zinc-200 dark:border-zinc-700 shadow-sm">
      <div className="max-w-5xl mx-auto px-6 flex items-center gap-1 h-14">
        <span className="text-base font-bold text-blue-600 mr-6 tracking-tight">
          MyApp
        </span>

        {navItems.map((item) => {
          const isActive = pathname === item.href;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`px-4 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                isActive
                  ? "bg-blue-600 text-white"
                  : "text-zinc-600 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800"
              }`}
            >
              {item.label}
            </Link>
          );
        })}

        {isLoggedIn && (
          <button
            onClick={handleLogout}
            className="ml-auto px-4 py-1.5 rounded-lg text-sm font-medium text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
          >
            Đăng xuất
          </button>
        )}
      </div>
    </nav>
  );
}
