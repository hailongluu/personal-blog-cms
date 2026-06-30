import type { Metadata } from 'next';
import { searchPosts } from '@/lib/api';
import PostCard from '@/components/PostCard';
import SearchBox from '@/components/SearchBox';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Tìm kiếm',
  description: 'Tìm bài viết về AI, công nghệ và lập trình.',
  // Result pages shouldn't be indexed (thin/duplicate), but links are followable.
  robots: { index: false, follow: true },
  alternates: { canonical: '/search' },
};

export default async function SearchPage({ searchParams }: { searchParams: Promise<{ q?: string }> }) {
  const { q = '' } = await searchParams;
  const query = q.trim();
  const { data: posts, meta } = await searchPosts(query, 1, 20);

  return (
    <div className="max-w-5xl mx-auto px-4 py-12 md:py-16">
      <header className="mb-8">
        <h1 className="text-3xl md:text-4xl font-bold text-stone-900 mb-4">Tìm kiếm</h1>
        <div className="max-w-md">
          <SearchBox initial={query} autoFocus />
        </div>
      </header>

      {!query ? (
        <p className="text-stone-400">Nhập từ khoá để tìm bài viết.</p>
      ) : posts.length === 0 ? (
        <p className="text-stone-500">
          Không tìm thấy bài viết nào cho “<span className="font-medium text-stone-700">{query}</span>”.
        </p>
      ) : (
        <>
          <p className="text-sm text-stone-500 mb-8">
            {meta.totalItems} kết quả cho “<span className="font-medium text-stone-700">{query}</span>”
          </p>
          <div className="grid gap-10 sm:grid-cols-2">
            {posts.map((post) => (
              <PostCard key={post.id} post={post} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}
