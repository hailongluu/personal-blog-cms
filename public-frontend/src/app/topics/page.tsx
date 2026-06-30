import type { Metadata } from 'next';
import Link from 'next/link';
import { FolderTree } from 'lucide-react';
import { getTopics } from '@/lib/api';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Chủ đề',
  description: 'Tất cả chủ đề trên blog — AI, công nghệ, lập trình và hơn thế.',
  alternates: { canonical: '/topics' },
};

export default async function TopicsPage() {
  const topics = await getTopics();

  return (
    <div className="max-w-5xl mx-auto px-4 py-12 md:py-16">
      <header className="mb-10">
        <h1 className="text-3xl md:text-4xl font-bold text-stone-900">Chủ đề</h1>
        <p className="mt-2 text-stone-500">Khám phá bài viết theo chủ đề.</p>
      </header>

      {topics.length === 0 ? (
        <p className="text-stone-400">Chưa có chủ đề nào.</p>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {topics.map((t) => (
            <Link
              key={t.id}
              href={`/topics/${t.slug}`}
              className="group flex items-start gap-3 rounded-xl border border-stone-200 bg-white p-5 hover:border-stone-400 hover:shadow-sm transition-all"
            >
              <span
                className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg text-lg"
                style={{ backgroundColor: (t.color || '#78716c') + '20', color: t.color || '#78716c' }}
              >
                {t.icon || <FolderTree size={18} />}
              </span>
              <span>
                <span className="block font-semibold text-stone-900 group-hover:text-stone-700">{t.name}</span>
                {t.description && <span className="mt-1 block text-sm text-stone-500 line-clamp-2">{t.description}</span>}
              </span>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
