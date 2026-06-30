'use client';

import { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Menu, X } from 'lucide-react';
import SearchBox from './SearchBox';
import ThemeToggle from './ThemeToggle';

const navLinks = [
  { href: '/', label: 'Home' },
  { href: '/blog', label: 'Blog' },
  { href: '/projects', label: 'Projects' },
  { href: '/about', label: 'About' },
  { href: '/now', label: 'Now' },
];

function isActive(pathname: string, href: string): boolean {
  return href === '/' ? pathname === '/' : pathname.startsWith(href);
}

export default function SiteHeader() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const pathname = usePathname() || '/';

  return (
    <header className="sticky top-0 z-50 bg-white/80 dark:bg-stone-900/80 backdrop-blur-md border-b border-stone-200 dark:border-stone-800">
      <div className="max-w-5xl mx-auto px-4 h-16 flex items-center justify-between">
        <Link href="/" className="text-xl font-bold tracking-tight text-stone-900 dark:text-stone-100 hover:text-stone-700 dark:hover:text-white transition-colors">
          ✦ Personal Blog
        </Link>

        <div className="hidden md:flex items-center gap-4">
          <nav className="flex items-center gap-1">
            {navLinks.map(({ href, label }) => (
              <Link
                key={href}
                href={href}
                className={[
                  'px-3 py-2 rounded-lg text-sm font-medium transition-colors',
                  isActive(pathname, href)
                    ? 'bg-stone-200 text-stone-900'
                    : 'text-stone-600 hover:text-stone-900 hover:bg-stone-100',
                ].join(' ')}
              >
                {label}
              </Link>
            ))}
          </nav>
          <SearchBox />
          <ThemeToggle />
        </div>

        <button
          onClick={() => setMobileOpen(!mobileOpen)}
          className="md:hidden p-2 rounded-lg text-stone-600 hover:bg-stone-100"
          aria-label="Toggle menu"
        >
          {mobileOpen ? <X size={20} /> : <Menu size={20} />}
        </button>
      </div>

      {mobileOpen && (
        <div className="md:hidden border-t border-stone-200 bg-white">
          <nav className="max-w-5xl mx-auto px-4 py-3 flex flex-col gap-1">
            <div className="mb-2">
              <SearchBox />
            </div>
            {navLinks.map(({ href, label }) => (
              <Link
                key={href}
                href={href}
                onClick={() => setMobileOpen(false)}
                className={[
                  'px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
                  isActive(pathname, href)
                    ? 'bg-stone-200 text-stone-900'
                    : 'text-stone-600 hover:text-stone-900 hover:bg-stone-100',
                ].join(' ')}
              >
                {label}
              </Link>
            ))}
          </nav>
        </div>
      )}
    </header>
  );
}
