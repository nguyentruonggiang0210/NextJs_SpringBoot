import Link from "next/link";

export default function NotFound() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-zinc-100 dark:bg-zinc-900">
      <div className="text-center px-6">
        <p className="text-8xl font-extrabold text-blue-600 mb-4">404</p>
        <h1 className="text-2xl font-bold text-zinc-800 dark:text-zinc-100 mb-2">
          Trang không tồn tại
        </h1>
        <p className="text-zinc-500 dark:text-zinc-400 mb-8">
          Đường dẫn bạn truy cập không hợp lệ hoặc đã bị xóa.
        </p>
        <Link
          href="/"
          className="inline-block rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-semibold px-6 py-2.5 text-sm transition-colors"
        >
          Về trang chủ
        </Link>
      </div>
    </div>
  );
}
