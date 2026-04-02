import Link from "next/link";

export default function ForbiddenPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-zinc-100 dark:bg-zinc-900">
      <div className="text-center px-6">
        <p className="text-8xl font-extrabold text-red-500 mb-4">403</p>
        <h1 className="text-2xl font-bold text-zinc-800 dark:text-zinc-100 mb-2">
          Không có quyền truy cập
        </h1>
        <p className="text-zinc-500 dark:text-zinc-400 mb-8">
          Tài khoản của bạn không có quyền xem trang này.
          <br />
          Vui lòng liên hệ admin để được cấp quyền.
        </p>
        <Link
          href="/login"
          className="inline-block rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-semibold px-6 py-2.5 text-sm transition-colors"
        >
          Quay lại đăng nhập
        </Link>
      </div>
    </div>
  );
}
