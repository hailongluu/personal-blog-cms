import type { Metadata } from 'next';
import { getPostsByTag } from '@/lib/api';
import { SITE_URL } from '@/lib/site';
import PostCard from '@/components/PostCard';
import Pagination from '@/components/Pagination';

export const dynamic = 'force-dynamic';

const PAGE_SIZE = 12;

type Params = { params: Promise<{ slug: string }>; searchParams: Promise<{ page?: string }> };

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { slug } = await params;
  return {
    title: `#${slug}`,
    description: `Bài viết gắn thẻ #${slug}.`,
    alternates: { canonical: `/tags/${slug}` },
    openGraph: { type: 'website', title: `#${slug}`, url: `${SITE_URL}/tags/${slug}` },
  };
}

export default async function TagPage({ params, searchParams }: Params) {
  const { slug } = await params;
  const { page: pageParam } = await searchParams;
  const page = Math.max(1, parseInt(pageParam ?? '1', 10) || 1);
  const { data: posts, meta } = await getPostsByTag(slug, page, PAGE_SIZE);

  return (
    <div className="max-w-5xl mx-auto px-4 py-12 md:py-16">
      <header className="mb-10">
        <p className="text-sm font-medium text-stone-400 uppercase tracking-wide">Thẻ</p>
        <h1 className="text-3xl md:text-4xl font-bold text-stone-900">#{slug}</h1>
      </header>

      {posts.length === 0 ? (
        <p className="text-stone-400">Chưa có bài viết nào với thẻ này.</p>
      ) : (
        <>
          <div className="grid gap-10 sm:grid-cols-2">
            {posts.map((post) => (
              <PostCard key={post.id} post={post} />
            ))}
          </div>
          <Pagination basePath={`/tags/${slug}`} page={page} totalPages={meta.totalPages} />
        </>
      )}
    </div>
  );
}
