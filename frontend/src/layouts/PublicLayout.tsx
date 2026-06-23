import { useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { Menu, X, Rss, ExternalLink, GitFork } from 'lucide-react';
import { cn } from '@/lib/utils';

const navLinks = [
  { to: '/', label: 'Home' },
  { to: '/blog', label: 'Blog' },
  { to: '/projects', label: 'Projects' },
  { to: '/about', label: 'About' },
  { to: '/now', label: 'Now' },
];

export default function PublicLayout() {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <div className="min-h-screen flex flex-col bg-stone-50 text-stone-900">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b border-stone-200">
        <div className="max-w-5xl mx-auto px-4 h-16 flex items-center justify-between">
          {/* Logo */}
          <Link to="/" className="text-xl font-bold tracking-tight text-stone-900 hover:text-stone-700 transition-colors">
            ✦ Personal Blog
          </Link>

          {/* Desktop nav */}
          <nav className="hidden md:flex items-center gap-1">
            {navLinks.map(({ to, label }) => (
              <NavLink
                key={to}
                to={to}
                end={to === '/'}
                className={({ isActive }) =>
                  cn(
                    'px-3 py-2 rounded-lg text-sm font-medium transition-colors',
                    isActive
                      ? 'bg-stone-200 text-stone-900'
                      : 'text-stone-600 hover:text-stone-900 hover:bg-stone-100',
                  )
                }
              >
                {label}
              </NavLink>
            ))}
          </nav>

          {/* Mobile menu button */}
          <button
            onClick={() => setMobileOpen(!mobileOpen)}
            className="md:hidden p-2 rounded-lg text-stone-600 hover:bg-stone-100"
            aria-label="Toggle menu"
          >
            {mobileOpen ? <X size={20} /> : <Menu size={20} />}
          </button>
        </div>

        {/* Mobile nav */}
        {mobileOpen && (
          <div className="md:hidden border-t border-stone-200 bg-white">
            <nav className="max-w-5xl mx-auto px-4 py-3 flex flex-col gap-1">
              {navLinks.map(({ to, label }) => (
                <NavLink
                  key={to}
                  to={to}
                  end={to === '/'}
                  onClick={() => setMobileOpen(false)}
                  className={({ isActive }) =>
                    cn(
                      'px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-stone-200 text-stone-900'
                        : 'text-stone-600 hover:text-stone-900 hover:bg-stone-100',
                    )
                  }
                >
                  {label}
                </NavLink>
              ))}
            </nav>
          </div>
        )}
      </header>

      {/* Main content */}
      <main className="flex-1">
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-stone-200 mt-auto">
        <div className="max-w-5xl mx-auto px-4 py-10">
          <div className="flex flex-col md:flex-row items-center justify-between gap-4">
            <div>
              <p className="text-sm text-stone-500">
                © {new Date().getFullYear()} Personal Blog. All rights reserved.
              </p>
            </div>
            <div className="flex items-center gap-4">
              <a href="/newsletter" className="flex items-center gap-1.5 text-sm text-stone-500 hover:text-stone-700 transition-colors">
                <Rss size={16} /> Newsletter
              </a>
              <a href="https://github.com" target="_blank" rel="noopener noreferrer" className="text-stone-400 hover:text-stone-700 transition-colors">
                <GitFork size={18} />
              </a>
              <a href="https://twitter.com" target="_blank" rel="noopener noreferrer" className="text-stone-400 hover:text-stone-700 transition-colors">
                <ExternalLink size={18} />
              </a>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
