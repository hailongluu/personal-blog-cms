import Link from 'next/link';
import { Rss, ExternalLink, GitFork } from 'lucide-react';

export default function SiteFooter() {
  const year = new Date().getFullYear();
  return (
    <footer className="bg-white dark:bg-stone-900 border-t border-stone-200 dark:border-stone-800 mt-auto">
      <div className="max-w-5xl mx-auto px-4 py-10">
        <div className="flex flex-col md:flex-row items-center justify-between gap-4">
          <p className="text-sm text-stone-500">© {year} Personal Blog. All rights reserved.</p>
          <div className="flex items-center gap-4">
            <Link href="/newsletter" className="flex items-center gap-1.5 text-sm text-stone-500 hover:text-stone-700 transition-colors">
              <Rss size={16} /> Newsletter
            </Link>
            <a href="/rss.xml" className="text-stone-400 hover:text-stone-700 transition-colors" aria-label="RSS feed">
              <Rss size={18} />
            </a>
            <a href="https://github.com/hailongluu" target="_blank" rel="noopener noreferrer" className="text-stone-400 hover:text-stone-700 transition-colors" aria-label="GitHub">
              <GitFork size={18} />
            </a>
            <a href="https://twitter.com/hailongluu" target="_blank" rel="noopener noreferrer" className="text-stone-400 hover:text-stone-700 transition-colors" aria-label="X">
              <ExternalLink size={18} />
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}
