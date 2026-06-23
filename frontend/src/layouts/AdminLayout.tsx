import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import {
  LayoutDashboard, FileText, FolderTree, Tags, Briefcase, Image, Settings, User, LogOut, Menu, X
} from 'lucide-react';
import { useState } from 'react';
import { cn } from '@/lib/utils';

const navItems = [
  { to: '/admin', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/admin/posts', icon: FileText, label: 'Posts' },
  { to: '/admin/topics', icon: FolderTree, label: 'Topics' },
  { to: '/admin/tags', icon: Tags, label: 'Tags' },
  { to: '/admin/projects', icon: Briefcase, label: 'Projects' },
  { to: '/admin/media', icon: Image, label: 'Media' },
  { to: '/admin/settings', icon: Settings, label: 'Settings' },
];

export default function AdminLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="flex h-screen overflow-hidden">
      {/* Sidebar */}
      <aside className={cn(
        "fixed inset-y-0 left-0 z-50 w-64 bg-primary-dark text-white transform transition-transform duration-200",
        "lg:relative lg:translate-x-0",
        sidebarOpen ? "translate-x-0" : "-translate-x-full"
      )}>
        <div className="flex items-center justify-between h-16 px-4 border-b border-primary">
          <span className="text-xl font-bold tracking-tight">📝 Blog CMS</span>
          <button onClick={() => setSidebarOpen(false)} className="lg:hidden p-1 rounded hover:bg-primary">
            <X size={20} />
          </button>
        </div>
        <nav className="px-2 py-4 space-y-1">
          {navItems.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/admin'}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) => cn(
                "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors",
                isActive ? "bg-primary text-white" : "text-primary-light hover:bg-primary hover:text-white"
              )}
            >
              <Icon size={18} />
              {label}
            </NavLink>
          ))}
        </nav>
      </aside>

      {/* Overlay for mobile */}
      {sidebarOpen && (
        <div className="fixed inset-0 z-40 bg-black/50 lg:hidden" onClick={() => setSidebarOpen(false)} />
      )}

      {/* Main content */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Header */}
        <header className="h-16 flex items-center justify-between px-4 border-b border-border bg-surface shrink-0">
          <button onClick={() => setSidebarOpen(true)} className="lg:hidden p-2 rounded-lg hover:bg-bg">
            <Menu size={20} />
          </button>
          <div className="flex items-center gap-3 ml-auto">
            <button
              onClick={() => navigate('/admin/profile')}
              className="text-sm text-text-muted hover:text-text transition-colors"
              title="View profile / change password"
            >
              <User size={14} className="inline mr-1" />
              {user?.displayName}
              <span className="ml-2 text-xs bg-primary-light/20 text-primary-dark px-2 py-0.5 rounded-full">
                {user?.role}
              </span>
            </button>
            <button
              onClick={logout}
              className="flex items-center gap-1 text-sm text-text-muted hover:text-red-600 transition-colors"
            >
              <LogOut size={16} /> Logout
            </button>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-auto p-6 bg-bg">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
