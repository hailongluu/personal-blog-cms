import type { Metadata } from 'next';
import Link from 'next/link';
import { CheckCircle2, XCircle } from 'lucide-react';
import { confirmNewsletter } from '@/lib/api';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Xác nhận đăng ký',
  robots: { index: false, follow: false },
};

export default async function ConfirmPage({ searchParams }: { searchParams: Promise<{ token?: string }> }) {
  const { token = '' } = await searchParams;
  const res = token ? await confirmNewsletter(token) : null;
  const ok = !!res && !res.error;

  return (
    <div className="max-w-md mx-auto px-4 py-20 text-center">
      {ok ? (
        <>
          <CheckCircle2 size={56} className="text-green-500 mx-auto mb-5" />
          <h1 className="text-2xl font-bold text-stone-900 dark:text-stone-100 mb-2">Đăng ký đã được xác nhận!</h1>
          <p className="text-stone-500">Cảm ơn bạn. Bạn sẽ nhận được bản tin mới nhất qua email.</p>
        </>
      ) : (
        <>
          <XCircle size={56} className="text-red-400 mx-auto mb-5" />
          <h1 className="text-2xl font-bold text-stone-900 dark:text-stone-100 mb-2">Không thể xác nhận</h1>
          <p className="text-stone-500">{res?.error?.message || 'Liên kết không hợp lệ hoặc đã được sử dụng.'}</p>
        </>
      )}
      <Link href="/" className="inline-block mt-8 text-sm font-medium text-stone-700 dark:text-stone-300 hover:underline">
        ← Về trang chủ
      </Link>
    </div>
  );
}
