'use client';

import { useState, type FormEvent } from 'react';
import { Send } from 'lucide-react';

type Status = 'idle' | 'loading' | 'success' | 'error';

export default function CommentForm({ postId }: { postId: number }) {
  const [authorName, setAuthorName] = useState('');
  const [authorEmail, setAuthorEmail] = useState('');
  const [content, setContent] = useState('');
  const [status, setStatus] = useState<Status>('idle');
  const [message, setMessage] = useState('');

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!authorName.trim() || !content.trim()) return;
    setStatus('loading');
    setMessage('');
    try {
      const res = await fetch('/api/public/comments', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ postId, parentId: null, authorName: authorName.trim(), authorEmail: authorEmail.trim(), content: content.trim() }),
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body?.error?.message || 'Gửi bình luận thất bại.');
      setStatus('success');
      setMessage('Cảm ơn bạn! Bình luận đang chờ kiểm duyệt và sẽ hiển thị sau khi được duyệt.');
      setContent('');
    } catch (err) {
      setStatus('error');
      setMessage(err instanceof Error ? err.message : 'Có lỗi xảy ra. Vui lòng thử lại.');
    }
  }

  if (status === 'success') {
    return <div className="rounded-lg border border-green-200 bg-green-50 p-4 text-sm text-green-700">{message}</div>;
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <div className="grid gap-3 sm:grid-cols-2">
        <input
          type="text"
          value={authorName}
          onChange={(e) => setAuthorName(e.target.value)}
          placeholder="Tên của bạn *"
          required
          disabled={status === 'loading'}
          className="px-3 py-2 border border-stone-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-stone-500 disabled:opacity-50"
        />
        <input
          type="email"
          value={authorEmail}
          onChange={(e) => setAuthorEmail(e.target.value)}
          placeholder="Email (không hiển thị)"
          disabled={status === 'loading'}
          className="px-3 py-2 border border-stone-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-stone-500 disabled:opacity-50"
        />
      </div>
      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder="Viết bình luận… *"
        required
        rows={4}
        disabled={status === 'loading'}
        className="w-full px-3 py-2 border border-stone-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-stone-500 disabled:opacity-50"
      />
      {status === 'error' && <p className="text-sm text-red-600">{message}</p>}
      <button
        type="submit"
        disabled={status === 'loading'}
        className="inline-flex items-center gap-2 px-5 py-2.5 bg-stone-900 text-white rounded-lg text-sm font-medium hover:bg-stone-800 transition-colors disabled:opacity-50"
      >
        {status === 'loading' ? 'Đang gửi…' : (<>Gửi bình luận <Send size={15} /></>)}
      </button>
    </form>
  );
}
