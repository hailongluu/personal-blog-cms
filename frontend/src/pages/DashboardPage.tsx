import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { dashboardApi, type DashboardData } from '@/lib/data';
import {
  FileText, FolderTree, Tags, Briefcase, Image, Mail,
  Star, Clock, CheckCircle2, Archive, Edit3,
} from 'lucide-react';

export default function DashboardPage() {
  const { user } = useAuth();
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    dashboardApi.get()
      .then(setData)
      .catch((e) => setError(e?.response?.data?.error?.message ?? 'Failed to load dashboard'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="p-6"><h1 className="text-2xl font-bold">Dashboard</h1><p className="text-text-muted">Loading...</p></div>;
  if (error || !data) return <div className="p-6"><h1 className="text-2xl font-bold">Dashboard</h1><p className="text-red-600">{error || 'No data'}</p></div>;

  const mainStats = [
    { label: 'Posts',     icon: FileText,    value: data.totalPosts,    sub: `${data.publishedPosts} published` },
    { label: 'Topics',    icon: FolderTree,  value: data.totalTopics },
    { label: 'Tags',      icon: Tags,        value: data.totalTags },
    { label: 'Projects',  icon: Briefcase,   value: data.totalProjects },
    { label: 'Media',     icon: Image,       value: data.totalMedia },
    { label: 'Newsletter', icon: Mail,       value: data.newsletterSubscribers, sub: 'subscribers' },
  ];

  const lifecycleStats = [
    { label: 'Published', icon: CheckCircle2, value: data.publishedPosts, color: 'text-green-600' },
    { label: 'Drafts',    icon: Edit3,        value: data.draftPosts,     color: 'text-stone-500' },
    { label: 'Reviewing', icon: Clock,        value: data.reviewingPosts, color: 'text-amber-600' },
    { label: 'Archived',  icon: Archive,      value: data.archivedPosts,  color: 'text-stone-400' },
  ];

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold text-text mb-1">Dashboard</h1>
      <p className="text-text-muted mb-6">Welcome back, {user?.displayName}!</p>

      {/* Main stats grid */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-6">
        {mainStats.map(({ label, icon: Icon, value, sub }) => (
          <div key={label} className="bg-surface rounded-xl border border-border p-4 hover:shadow-sm transition-shadow">
            <div className="flex items-center gap-2 text-text-muted mb-2">
              <Icon size={18} />
              <span className="text-sm font-medium">{label}</span>
            </div>
            <p className="text-2xl font-bold text-text">{value}</p>
            {sub && <p className="text-xs text-text-muted mt-0.5">{sub}</p>}
          </div>
        ))}
      </div>

      {/* Lifecycle breakdown */}
      <div className="bg-surface rounded-xl border border-border p-4 mb-6">
        <h2 className="text-sm font-semibold text-text-muted uppercase tracking-wide mb-3">
          Posts by lifecycle
        </h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          {lifecycleStats.map(({ label, icon: Icon, value, color }) => (
            <div key={label} className="flex items-center gap-3 px-3 py-2 rounded-lg bg-bg">
              <Icon size={18} className={color} />
              <div>
                <p className="text-xs text-text-muted">{label}</p>
                <p className={`text-lg font-bold ${color}`}>{value}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Recent + Drafts */}
      <div className="grid gap-4 lg:grid-cols-2">
        {/* Recent published */}
        <div className="bg-surface rounded-xl border border-border p-5">
          <div className="flex items-center justify-between mb-3">
            <h2 className="font-semibold text-text">Recent published</h2>
            <Link to="/admin/posts" className="text-xs text-primary hover:underline">
              View all →
            </Link>
          </div>
          {(data.recentPosts?.length ?? 0) === 0 ? (
            <p className="text-sm text-text-muted py-4 text-center">No published posts yet.</p>
          ) : (
            <ul className="divide-y divide-border">
              {data.recentPosts?.map((p) => (
                <li key={p.id} className="py-2.5 flex items-start gap-2">
                  {p.featured && <Star size={14} className="mt-1 text-amber-500 shrink-0" />}
                  <div className="min-w-0 flex-1">
                    <Link
                      to={`/admin/posts/${p.id}`}
                      className="text-sm font-medium text-text hover:text-primary line-clamp-1"
                    >
                      {p.title}
                    </Link>
                    <p className="text-xs text-text-muted mt-0.5">
                      <span className="font-mono uppercase">{p.type}</span>
                      {' · '}{new Date(p.updatedAt).toLocaleDateString()}
                    </p>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* Drafts */}
        <div className="bg-surface rounded-xl border border-border p-5">
          <div className="flex items-center justify-between mb-3">
            <h2 className="font-semibold text-text">Latest drafts</h2>
            <Link to="/admin/posts?status=draft" className="text-xs text-primary hover:underline">
              View all →
            </Link>
          </div>
          {(data.pendingDrafts?.length ?? 0) === 0 ? (
            <p className="text-sm text-text-muted py-4 text-center">No drafts.</p>
          ) : (
            <ul className="divide-y divide-border">
              {data.pendingDrafts?.map((p) => (
                <li key={p.id} className="py-2.5 flex items-start gap-2">
                  <Edit3 size={14} className="mt-1 text-stone-400 shrink-0" />
                  <div className="min-w-0 flex-1">
                    <Link
                      to={`/admin/posts/${p.id}`}
                      className="text-sm font-medium text-text hover:text-primary line-clamp-1"
                    >
                      {p.title}
                    </Link>
                    <p className="text-xs text-text-muted mt-0.5">
                      <span className="font-mono uppercase">{p.type}</span>
                      {' · '}Updated {new Date(p.updatedAt).toLocaleDateString()}
                    </p>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
