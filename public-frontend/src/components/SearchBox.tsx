'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { Search } from 'lucide-react';

export default function SearchBox({ initial = '', autoFocus = false }: { initial?: string; autoFocus?: boolean }) {
  const router = useRouter();
  const [q, setQ] = useState(initial);

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        const v = q.trim();
        if (v) router.push(`/search?q=${encodeURIComponent(v)}`);
      }}
      className="relative"
      role="search"
    >
      <Search size={15} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-stone-400 pointer-events-none" />
      <input
        type="search"
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder="Tìm kiếm…"
        aria-label="Tìm kiếm bài viết"
        autoFocus={autoFocus}
        className="w-full sm:w-52 pl-8 pr-3 py-1.5 text-sm rounded-lg border border-stone-300 bg-white outline-none focus:ring-2 focus:ring-stone-400"
      />
    </form>
  );
}
