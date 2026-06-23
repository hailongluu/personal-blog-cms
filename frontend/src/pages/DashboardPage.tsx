import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { postsApi, topicsApi, tagsApi, projectsApi, mediaApi } from '@/lib/data';
import { FileText, FolderTree, Tags, Briefcase, Image } from 'lucide-react';

export default function DashboardPage() {
  const { user } = useAuth();
  const [stats, setStats] = useState([
    { label: 'Posts', icon: FileText, value: '—' },
    { label: 'Topics', icon: FolderTree, value: '—' },
    { label: 'Tags', icon: Tags, value: '—' },
    { label: 'Projects', icon: Briefcase, value: '—' },
    { label: 'Media', icon: Image, value: '—' },
  ]);

  useEffect(() => {
    async function load() {
      try {
        const [posts, topics, tags, projects, media] = await Promise.all([
          postsApi.list().catch(() => ({ meta: { totalItems: 0 } })),
          topicsApi.list().catch(() => []),
          tagsApi.list().catch(() => []),
          projectsApi.list().catch(() => ({ meta: { totalItems: 0 } })),
          mediaApi.list().catch(() => ({ meta: { totalItems: 0 } })),
        ]);
        setStats([
          { label: 'Posts', icon: FileText, value: String(posts.meta?.totalItems ?? 0) },
          { label: 'Topics', icon: FolderTree, value: String(topics.length) },
          { label: 'Tags', icon: Tags, value: String(tags.length) },
          { label: 'Projects', icon: Briefcase, value: String(projects.meta?.totalItems ?? 0) },
          { label: 'Media', icon: Image, value: String(media.meta?.totalItems ?? 0) },
        ]);
      } catch (e) {
        console.error('Failed to load dashboard stats', e);
      }
    }
    load();
  }, []);

  return (
    <div>
      <h1 className="text-2xl font-bold text-text mb-1">Dashboard</h1>
      <p className="text-text-muted mb-6">Welcome back, {user?.displayName}!</p>

      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
        {stats.map(({ label, icon: Icon, value }) => (
          <div key={label} className="bg-surface rounded-xl border border-border p-4 hover:shadow-sm transition-shadow">
            <div className="flex items-center gap-2 text-text-muted mb-2">
              <Icon size={18} />
              <span className="text-sm font-medium">{label}</span>
            </div>
            <p className="text-2xl font-bold text-text">{value}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
