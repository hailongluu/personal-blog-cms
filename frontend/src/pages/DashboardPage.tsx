import { useAuth } from '@/contexts/AuthContext';
import { FileText, FolderTree, Tags, Briefcase, Image } from 'lucide-react';

export default function DashboardPage() {
  const { user } = useAuth();

  const stats = [
    { label: 'Posts', icon: FileText, value: '—' },
    { label: 'Topics', icon: FolderTree, value: '—' },
    { label: 'Tags', icon: Tags, value: '—' },
    { label: 'Projects', icon: Briefcase, value: '—' },
    { label: 'Media', icon: Image, value: '—' },
  ];

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

      <div className="mt-8 p-8 bg-surface rounded-xl border border-border text-center text-text-muted">
        <p className="text-lg">🚀 Story 4 complete — Admin shell ready</p>
        <p className="text-sm mt-1">CRUD pages coming in Story 5 (Editor) + Story 6 (Public)</p>
      </div>
    </div>
  );
}
