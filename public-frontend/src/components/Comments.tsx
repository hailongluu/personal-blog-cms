import { getComments } from '@/lib/api';
import CommentForm from './CommentForm';
import type { Comment } from '@/types';

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('vi-VN', { year: 'numeric', month: 'long', day: 'numeric' });
}

// Server component: approved comments are server-rendered (visible to crawlers),
// the submit form is a client island. Replies are nested by parentId.
export default async function Comments({ postId }: { postId: number }) {
  const comments = await getComments(postId);

  const byParent = new Map<number | null, Comment[]>();
  for (const c of comments) {
    const key = c.parentId ?? null;
    if (!byParent.has(key)) byParent.set(key, []);
    byParent.get(key)!.push(c);
  }

  function renderLevel(parentId: number | null, depth: number): React.ReactNode {
    const items = byParent.get(parentId) ?? [];
    if (items.length === 0) return null;
    return (
      <ul className={depth > 0 ? 'mt-4 space-y-4 border-l border-stone-200 pl-4' : 'space-y-6'}>
        {items.map((c) => (
          <li key={c.id}>
            <div className="flex items-center gap-2 text-sm">
              <span className="font-semibold text-stone-800">{c.authorName}</span>
              <span className="text-stone-400">·</span>
              <time className="text-stone-400">{formatDate(c.createdAt)}</time>
            </div>
            <p className="mt-1 text-stone-700 whitespace-pre-wrap">{c.content}</p>
            {renderLevel(c.id, depth + 1)}
          </li>
        ))}
      </ul>
    );
  }

  return (
    <section id="comments" className="max-w-3xl mx-auto px-4 pb-16">
      <h2 className="text-xl font-bold text-stone-900 mb-6">
        Bình luận{comments.length > 0 ? ` (${comments.length})` : ''}
      </h2>

      {comments.length > 0 ? renderLevel(null, 0) : (
        <p className="text-stone-400 text-sm mb-8">Chưa có bình luận nào. Hãy là người đầu tiên!</p>
      )}

      <div className="mt-10 pt-8 border-t border-stone-200">
        <h3 className="text-base font-semibold text-stone-800 mb-4">Để lại bình luận</h3>
        <CommentForm postId={postId} />
      </div>
    </section>
  );
}
