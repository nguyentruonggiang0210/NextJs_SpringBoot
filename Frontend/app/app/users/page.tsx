"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { API_BASE } from "../../lib/config";

interface User {
  id: number;
  name: string;
  email: string;
  phone: string;
  password: string | null;
  code: string;
  permissionName: string | null;
}

type UserFormData = Omit<User, "id" | "code" | "permissionName">

const emptyForm: UserFormData = { name: "", email: "", phone: "", password: "" };

export default function UsersPage() {
  const router = useRouter();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Modal state
  const [showModal, setShowModal] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [form, setForm] = useState<UserFormData>(emptyForm);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState("");

  function getToken() {
    return localStorage.getItem("accessToken") ?? "";
  }

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const res = await fetch(`${API_BASE}/api/users`, {
        headers: { Authorization: `Bearer ${getToken()}` },
      });
      if (res.status === 401 || res.status === 403) {
        localStorage.clear();
        router.push(res.status === 403 ? "/forbidden" : "/login");
        return;
      }
      const data: User[] = await res.json();
      setUsers(data);
    } catch {
      setError("Không thể tải danh sách người dùng.");
    } finally {
      setLoading(false);
    }
  }, [router]);

  useEffect(() => {
    if (!localStorage.getItem("accessToken")) {
      router.push("/login");
      return;
    }
    // Kiểm tra quyền từ localStorage trước khi gọi API
    const stored = localStorage.getItem("user");
    if (stored) {
      const u = JSON.parse(stored) as { permissionName?: string };
      if (u.permissionName !== "member" && u.permissionName !== "admin") {
        router.push("/forbidden");
        return;
      }
    }
    fetchUsers();
  }, [fetchUsers, router]);

  function openCreate() {
    setEditingUser(null);
    setForm(emptyForm);
    setFormError("");
    setShowModal(true);
  }

  function openEdit(user: User) {
    setEditingUser(user);
    setForm({ name: user.name, email: user.email, phone: user.phone, password: "" });
    setFormError("");
    setShowModal(true);
  }

  async function handleSave() {
    setSaving(true);
    setFormError("");
    try {
      if (editingUser) {
        // PUT /api/users/{id}
        const res = await fetch(`${API_BASE}/api/users/${editingUser.id}`, {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${getToken()}`,
          },
          body: JSON.stringify(form),
        });
        if (!res.ok) throw new Error("Cập nhật thất bại.");
      } else {
        // POST /api/users
        const res = await fetch(`${API_BASE}/api/users`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${getToken()}`,
          },
          body: JSON.stringify(form),
        });
        if (!res.ok) throw new Error("Tạo người dùng thất bại.");
      }
      setShowModal(false);
      await fetchUsers();
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "Có lỗi xảy ra.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id: number) {
    if (!confirm("Bạn có chắc muốn xóa người dùng này không?")) return;
    try {
      await fetch(`${API_BASE}/api/users/${id}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${getToken()}` },
      });
      await fetchUsers();
    } catch {
      alert("Xóa thất bại.");
    }
  }

  function handleLogout() {
    localStorage.clear();
    router.push("/login");
  }

  return (
    <div className="min-h-screen bg-zinc-100 dark:bg-zinc-900 p-6">
      {/* Header */}
      <div className="max-w-5xl mx-auto flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-zinc-800 dark:text-zinc-100">
          Quản lý người dùng
        </h1>
        <div className="flex gap-3">
          <button
            onClick={openCreate}
            className="rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-semibold px-4 py-2 text-sm transition-colors"
          >
            + Thêm mới
          </button>
          <button
            onClick={handleLogout}
            className="rounded-lg bg-zinc-200 hover:bg-zinc-300 dark:bg-zinc-700 dark:hover:bg-zinc-600 text-zinc-700 dark:text-zinc-200 font-semibold px-4 py-2 text-sm transition-colors"
          >
            Đăng xuất
          </button>
        </div>
      </div>

      {/* Table */}
      <div className="max-w-5xl mx-auto bg-white dark:bg-zinc-800 rounded-2xl shadow overflow-hidden">
        {loading ? (
          <p className="text-center py-12 text-zinc-500">Đang tải...</p>
        ) : error ? (
          <p className="text-center py-12 text-red-500">{error}</p>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-zinc-50 dark:bg-zinc-700 text-zinc-600 dark:text-zinc-300 uppercase text-xs tracking-wide">
              <tr>
                <th className="px-4 py-3 text-left">ID</th>
                <th className="px-4 py-3 text-left">Tên</th>
                <th className="px-4 py-3 text-left">Email</th>
                <th className="px-4 py-3 text-left">Số điện thoại</th>
                <th className="px-4 py-3 text-left">Code</th>
                <th className="px-4 py-3 text-center">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100 dark:divide-zinc-700">
              {users.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-10 text-zinc-400">
                    Không có người dùng nào.
                  </td>
                </tr>
              ) : (
                users.map((user) => (
                  <tr
                    key={user.id}
                    className="hover:bg-zinc-50 dark:hover:bg-zinc-700/50 transition-colors"
                  >
                    <td className="px-4 py-3 text-zinc-800 dark:text-zinc-100">{user.id}</td>
                    <td className="px-4 py-3 text-zinc-800 dark:text-zinc-100">{user.name}</td>
                    <td className="px-4 py-3 text-zinc-600 dark:text-zinc-300">{user.email}</td>
                    <td className="px-4 py-3 text-zinc-600 dark:text-zinc-300">{user.phone}</td>
                    <td className="px-4 py-3 text-zinc-400 text-xs font-mono">{user.code}</td>
                    <td className="px-4 py-3 text-center">
                      <button
                        onClick={() => openEdit(user)}
                        className="text-blue-600 hover:underline font-medium mr-3"
                      >
                        Sửa
                      </button>
                      <button
                        onClick={() => handleDelete(user.id)}
                        className="text-red-500 hover:underline font-medium"
                      >
                        Xóa
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}
      </div>

      {/* Modal Create / Edit */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="w-full max-w-md bg-white dark:bg-zinc-800 rounded-2xl shadow-xl p-6">
            <h2 className="text-lg font-bold text-zinc-800 dark:text-zinc-100 mb-4">
              {editingUser ? "Cập nhật người dùng" : "Thêm người dùng mới"}
            </h2>

            <div className="flex flex-col gap-3">
              {(["name", "email", "phone", "password"] as (keyof UserFormData)[]).map((field) => (
                <div key={field}>
                  <label className="block text-xs font-medium text-zinc-500 dark:text-zinc-400 mb-1 capitalize">
                    {field === "name"
                      ? "Tên"
                      : field === "email"
                      ? "Email"
                      : field === "phone"
                      ? "Số điện thoại"
                      : "Mật khẩu"}
                  </label>
                  <input
                    type={field === "password" ? "password" : "text"}
                    value={form[field] ?? ""}
                    onChange={(e) => setForm({ ...form, [field]: e.target.value })}
                    placeholder={
                      field === "name"
                        ? "Nhập tên"
                        : field === "email"
                        ? "Nhập email"
                        : field === "phone"
                        ? "Nhập số điện thoại"
                        : editingUser
                        ? "Để trống nếu không đổi"
                        : "Nhập mật khẩu"
                    }
                    className="w-full rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-700 px-3 py-2 text-sm text-zinc-800 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              ))}

              {formError && (
                <p className="text-sm text-red-500">{formError}</p>
              )}
            </div>

            <div className="flex justify-end gap-3 mt-5">
              <button
                onClick={() => setShowModal(false)}
                className="px-4 py-2 rounded-lg text-sm font-medium bg-zinc-100 hover:bg-zinc-200 dark:bg-zinc-700 dark:hover:bg-zinc-600 text-zinc-700 dark:text-zinc-200 transition-colors"
              >
                Hủy
              </button>
              <button
                onClick={handleSave}
                disabled={saving}
                className="px-4 py-2 rounded-lg text-sm font-semibold bg-blue-600 hover:bg-blue-700 disabled:opacity-60 text-white transition-colors"
              >
                {saving ? "Đang lưu..." : "Lưu"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
