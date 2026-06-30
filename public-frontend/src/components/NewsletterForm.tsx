'use client';

import { useState, type FormEvent } from 'react';
import { Send, CheckCircle2 } from 'lucide-react';

type Status = 'idle' | 'loading' | 'success' | 'error';

export default function NewsletterForm() {
  const [email, setEmail] = useState('');
  const [status, setStatus] = useState<Status>('idle');
  const [message, setMessage] = useState('');

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!email.trim()) return;
    setStatus('loading');
    setMessage('');
    try {
      const res = await fetch('/api/public/newsletter/subscribe', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email.trim() }),
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body?.error?.message || 'Đăng ký thất bại.');
      setMessage(body?.data?.message || 'Cảm ơn bạn đã đăng ký! Kiểm tra hộp thư để xác nhận.');
      setStatus('success');
      setEmail('');
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Có lỗi xảy ra. Vui lòng thử lại.');
      setStatus('error');
    }
  }

  if (status === 'success') {
    return (
      <div className="bg-green-50 border border-green-200 rounded-xl p-8 text-center">
        <CheckCircle2 size={48} className="text-green-500 mx-auto mb-4" />
        <h2 className="text-xl font-semibold text-green-800 mb-2">Bạn đã đăng ký!</h2>
        <p className="text-green-600">{message}</p>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="bg-white rounded-xl border border-stone-200 p-8 shadow-sm">
      <div className="flex flex-col sm:flex-row gap-3">
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="you@example.com"
          required
          disabled={status === 'loading'}
          className="flex-1 px-4 py-3 border border-stone-300 rounded-lg text-stone-900 placeholder-stone-400 focus:ring-2 focus:ring-stone-500 focus:border-stone-500 outline-none text-sm disabled:opacity-50"
        />
        <button
          type="submit"
          disabled={status === 'loading'}
          className="inline-flex items-center justify-center gap-2 px-6 py-3 bg-stone-900 text-white rounded-lg font-medium hover:bg-stone-800 transition-colors disabled:opacity-50"
        >
          {status === 'loading' ? 'Đang đăng ký...' : (<>Đăng ký <Send size={16} /></>)}
        </button>
      </div>
      {status === 'error' && <p className="mt-4 text-sm text-red-600">{message}</p>}
      <p className="mt-4 text-xs text-stone-400 text-center">Không spam. Hủy đăng ký bất cứ lúc nào.</p>
    </form>
  );
}
