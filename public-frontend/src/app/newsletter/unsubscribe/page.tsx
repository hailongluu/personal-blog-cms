import type { Metadata } from 'next';
import Link from 'next/link';
import { CheckCircle2 } from 'lucide-react';
import { unsubscribeNewsletter } from '@/lib/api';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Hủy đăng ký',
  robots: { index: false, follow: false },
};

export default async function UnsubscribePage({ searchParams }: { searchParams: Promise<{ email?: string }> }) {
  const { email = '' } = await searchParams;
  if (email) await unsubscribeNewsletter(email);

  return (
    <div className="max-w-md mx-auto px-4 py-20 text-center">
      <CheckCircle2 size={56} className="text-stone-400 mx-auto mb-5" />
      <h1 className="text-2xl font-bold text-stone-900 dark:text-stone-100 mb-2">Bạn đã hủy đăng ký</h1>
      <p className="text-stone-500">
        {email ? `${email} sẽ không nhận bản tin nữa.` : 'Yêu cầu đã được xử lý.'} Bạn có thể đăng ký lại bất cứ lúc nào.
      </p>
      <Link href="/newsletter" className="inline-block mt-8 text-sm font-medium text-stone-700 dark:text-stone-300 hover:underline">
        Đăng ký lại
      </Link>
    </div>
  );
}
