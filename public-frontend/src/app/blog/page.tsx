import type { Metadata } from 'next';
import { getPublishedPosts } from '@/lib/api';
import PostCard from '@/components/PostCard';
import Pagination from '@/components/Pagination';

export const dynamic = 'force-dynamic';

const PAGE_SIZE = 12;

export const metadata: Metadata = {
  title: 'Blog',
  description: 'Tất cả bài viết về AI, công nghệ và lập trình.',
  alternates: { canonical: '/blog' },
};

export default async function BlogListPage({ searchParams }: { searchParams: Promise<{ page?: string }> }) {
  const { page: pageParam } = await searchParams;
  const page = Math.max(1, parseInt(pageParam ?? '1', 10) || 1);
  const { data: posts, meta } = await getPublishedPosts(page, PAGE_SIZE);

  return (
    <div className="max-w-5xl mx-auto px-4 py-12 md:py-16">
      <header className="mb-10">
        <h1 className="text-3xl md:text-4xl font-bold text-stone-900">Blog</h1>
        <p className="mt-2 text-stone-500">Bài viết phân tích, hướng dẫn và đánh giá về AI &amp; công nghệ.</p>
      </header>

      {posts.length === 0 ? (
        <p className="text-stone-400">Chưa có bài viết nào.</p>
      ) : (
        <>
          <div className="grid gap-10 sm:grid-cols-2">
            {posts.map((post) => (
              <PostCard key={post.id} post={post} />
            ))}
          </div>
          <Pagination basePath="/blog" page={page} totalPages={meta.totalPages} />
        </>
      )}
    </div>
  );
}
