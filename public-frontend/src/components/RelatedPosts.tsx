import { getRelatedPosts } from '@/lib/api';
import PostCard from './PostCard';

export default async function RelatedPosts({ slug }: { slug: string }) {
  const posts = await getRelatedPosts(slug, 3);
  if (posts.length === 0) return null;

  return (
    <section className="max-w-5xl mx-auto px-4 pb-16 pt-10 border-t border-stone-200">
      <h2 className="text-xl font-bold text-stone-900 mb-6">Bài liên quan</h2>
      <div className="grid gap-10 sm:grid-cols-2 lg:grid-cols-3">
        {posts.map((p) => (
          <PostCard key={p.id} post={p} />
        ))}
      </div>
    </section>
  );
}
