'use client';

import { useState } from 'react';
import { Link2, Check } from 'lucide-react';

export default function ShareButtons({ url, title }: { url: string; title: string }) {
  const [copied, setCopied] = useState(false);
  const u = encodeURIComponent(url);
  const t = encodeURIComponent(title);

  const links = [
    { label: 'X', href: `https://twitter.com/intent/tweet?url=${u}&text=${t}` },
    { label: 'Facebook', href: `https://www.facebook.com/sharer/sharer.php?u=${u}` },
    { label: 'Telegram', href: `https://t.me/share/url?url=${u}&text=${t}` },
  ];

  async function copy() {
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      /* clipboard unavailable */
    }
  }

  return (
    <div className="flex flex-wrap items-center gap-3 text-sm text-stone-500 dark:text-stone-400">
      <span className="font-medium">Chia sẻ:</span>
      {links.map((l) => (
        <a
          key={l.label}
          href={l.href}
          target="_blank"
          rel="noopener noreferrer"
          className="hover:text-stone-900 dark:hover:text-white hover:underline"
        >
          {l.label}
        </a>
      ))}
      <button onClick={copy} className="inline-flex items-center gap-1 hover:text-stone-900 dark:hover:text-white">
        {copied ? <Check size={14} /> : <Link2 size={14} />}
        {copied ? 'Đã copy' : 'Copy link'}
      </button>
    </div>
  );
}
