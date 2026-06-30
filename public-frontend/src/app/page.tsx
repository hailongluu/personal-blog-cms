import Link from 'next/link';
import { ArrowRight } from 'lucide-react';
import { getFeaturedPosts, getPublishedPosts } from '@/lib/api';
import { SITE_TAGLINE } from '@/lib/site';
import PostCard from '@/components/PostCard';

// Always SSR with live data — the backend isn't reachable at build time.
export const dynamic = 'force-dynamic';

export default async function HomePage() {
  const [featured, latest] = await Promise.all([
    getFeaturedPosts(3),
    getPublishedPosts(1, 6),
  ]);
  const featuredIds = new Set(featured.map((p) => p.id));
  const recent = latest.data.filter((p) => !featuredIds.has(p.id)).slice(0, 6);

  return (
    <div className="max-w-5xl mx-auto px-4 py-12 md:py-20">
      <section className="mb-16">
        <h1 className="text-4xl md:text-5xl font-bold text-stone-900 leading-tight max-w-3xl">
          Lưu Hải Long — {SITE_TAGLINE}
        </h1>
        <p className="mt-4 text-lg text-stone-500 max-w-2xl">
          Chia sẻ kiến thức chuyên sâu về AI, công nghệ và lập trình. Bài viết phân tích, hướng dẫn và đánh giá công cụ mới nhất.
        </p>
        <Link href="/blog" className="mt-6 inline-flex items-center gap-1.5 text-sm font-medium text-stone-700 hover:text-stone-900">
          Đọc blog <ArrowRight size={16} />
        </Link>
      </section>

      {featured.length > 0 && (
        <section className="mb-16">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-stone-400 mb-6">Nổi bật</h2>
          <div className="grid gap-10 sm:grid-cols-2 lg:grid-cols-3">
            {featured.map((post) => (
              <PostCard key={post.id} post={post} />
            ))}
          </div>
        </section>
      )}

      {recent.length > 0 && (
        <section>
          <h2 className="text-sm font-semibold uppercase tracking-wide text-stone-400 mb-6">Mới nhất</h2>
          <div className="grid gap-10 sm:grid-cols-2 lg:grid-cols-3">
            {recent.map((post) => (
              <PostCard key={post.id} post={post} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
