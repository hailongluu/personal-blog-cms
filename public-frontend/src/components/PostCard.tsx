import Link from 'next/link';
import { Clock, Calendar } from 'lucide-react';
import type { Post } from '@/types';

function formatDate(iso: string | null): string {
  if (!iso) return '';
  return new Date(iso).toLocaleDateString('vi-VN', { year: 'numeric', month: 'long', day: 'numeric' });
}

export default function PostCard({ post }: { post: Post }) {
  return (
    <article className="group">
      <Link href={`/blog/${post.slug}`} className="block">
        {post.coverImageUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={post.coverImageUrl}
            alt={post.title}
            className="w-full aspect-[16/9] object-cover rounded-xl mb-4 group-hover:opacity-90 transition-opacity"
          />
        )}
        {post.topic && (
          <span
            className="inline-block text-xs font-medium px-2.5 py-1 rounded-full mb-2"
            style={{ backgroundColor: (post.topic.color || '#78716c') + '20', color: post.topic.color || '#78716c' }}
          >
            {post.topic.name}
          </span>
        )}
        <h2 className="text-xl font-bold text-stone-900 leading-snug group-hover:text-stone-700 transition-colors">
          {post.title}
        </h2>
        {post.excerpt && <p className="mt-2 text-stone-500 line-clamp-2">{post.excerpt}</p>}
        <div className="mt-3 flex items-center gap-4 text-xs text-stone-400">
          {post.publishedAt && (
            <span className="flex items-center gap-1">
              <Calendar size={13} /> {formatDate(post.publishedAt)}
            </span>
          )}
          <span className="flex items-center gap-1">
            <Clock size={13} /> {post.readingTimeMin} min
          </span>
        </div>
      </Link>
    </article>
  );
}
