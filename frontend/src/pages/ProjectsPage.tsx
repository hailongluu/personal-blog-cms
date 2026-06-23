import { useState, useEffect, useCallback } from 'react';
import { projectsApi } from '@/lib/data';
import type { Project } from '@/types';
import { Plus, Pencil, Trash2, RefreshCw, ExternalLink, GitBranch } from 'lucide-react';

export default function ProjectsPage() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState<{ open: boolean; project?: Project }>({ open: false });
  const [form, setForm] = useState({ title: '', description: '', projectUrl: '', repoUrl: '', techStack: '', isFeatured: false });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try { setProjects((await projectsApi.list()).data); } catch (e) { console.error(e); }
    setLoading(false);
  }, []);
  useEffect(() => { load(); }, [load]);

  function openCreate() { setForm({ title: '', description: '', projectUrl: '', repoUrl: '', techStack: '', isFeatured: false }); setError(''); setModal({ open: true }); }
  function openEdit(p: Project) { setForm({ title: p.title, description: p.description || '', projectUrl: p.projectUrl || '', repoUrl: p.repoUrl || '', techStack: p.techStack?.join(', ') || '', isFeatured: p.isFeatured }); setError(''); setModal({ open: true, project: p }); }

  async function handleSave() {
    if (!form.title.trim()) return;
    setSaving(true); setError('');
    const data = { ...form, techStack: form.techStack.split(',').map(s => s.trim()).filter(Boolean) as any };
    try {
      if (modal.project) await projectsApi.update(modal.project.id, data as any);
      else await projectsApi.create(data as any);
      setModal({ open: false });
      load();
    } catch (err: any) { setError(err.response?.data?.error?.message || 'Save failed'); }
    setSaving(false);
  }

  async function handleDelete(id: number) {
    if (!confirm('Delete this project?')) return;
    await projectsApi.delete(id);
    load();
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <div><h1 className="text-2xl font-bold text-text">Projects</h1><p className="text-text-muted text-sm">{projects.length} projects</p></div>
        <div className="flex gap-2">
          <button onClick={load} className="p-2 border border-border rounded-lg hover:bg-bg"><RefreshCw size={16} /></button>
          <button onClick={openCreate} className="flex items-center gap-1.5 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary-dark text-sm font-medium"><Plus size={16} /> New Project</button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {loading && <div className="col-span-full text-center py-8 text-text-muted">Loading...</div>}
        {projects.map(p => (
          <div key={p.id} className="bg-surface rounded-xl border border-border p-4 hover:shadow-sm transition-shadow">
            <h3 className="font-medium text-text mb-1">{p.title}</h3>
            {p.description && <p className="text-text-muted text-sm mb-3 line-clamp-2">{p.description}</p>}
            {p.techStack && p.techStack.length > 0 && (
              <div className="flex flex-wrap gap-1 mb-3">
                {p.techStack.map(ts => <span key={ts} className="text-xs bg-bg px-2 py-0.5 rounded-full text-text-muted">{ts}</span>)}
              </div>
            )}
            <div className="flex items-center gap-3">
              {p.projectUrl && <a href={p.projectUrl} target="_blank" className="text-text-muted hover:text-primary"><ExternalLink size={14} /></a>}
              {p.repoUrl && <a href={p.repoUrl} target="_blank" className="text-text-muted hover:text-primary"><GitBranch size={14} /></a>}
              <div className="ml-auto flex gap-1">
                <button onClick={() => openEdit(p)} className="p-1.5 rounded hover:bg-bg text-text-muted hover:text-primary"><Pencil size={14} /></button>
                <button onClick={() => handleDelete(p.id)} className="p-1.5 rounded hover:bg-bg text-text-muted hover:text-red-600"><Trash2 size={14} /></button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Modal */}
      {modal.open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setModal({ open: false })}>
          <div className="bg-surface rounded-xl shadow-lg p-6 w-full max-w-md mx-4" onClick={e => e.stopPropagation()}>
            <h2 className="text-lg font-bold text-text mb-4">{modal.project ? 'Edit Project' : 'New Project'}</h2>
            {error && <div className="mb-3 bg-red-50 text-red-700 text-xs px-3 py-2 rounded-lg">{error}</div>}
            <div className="space-y-3">
              <div><label className="block text-sm font-medium mb-1">Title *</label><input type="text" value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none" /></div>
              <div><label className="block text-sm font-medium mb-1">Description</label><input type="text" value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none" /></div>
              <div><label className="block text-sm font-medium mb-1">Project URL</label><input type="url" value={form.projectUrl} onChange={e => setForm({ ...form, projectUrl: e.target.value })} className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none" /></div>
              <div><label className="block text-sm font-medium mb-1">Repo URL</label><input type="url" value={form.repoUrl} onChange={e => setForm({ ...form, repoUrl: e.target.value })} className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none" /></div>
              <div><label className="block text-sm font-medium mb-1">Tech Stack (comma-separated)</label><input type="text" value={form.techStack} onChange={e => setForm({ ...form, techStack: e.target.value })} className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none" /></div>
              <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={form.isFeatured} onChange={e => setForm({ ...form, isFeatured: e.target.checked })} className="rounded" /> Featured</label>
            </div>
            <div className="flex gap-2 mt-4">
              <button onClick={handleSave} disabled={saving} className="flex-1 py-2 bg-primary text-white rounded-lg text-sm font-medium disabled:opacity-50">{saving ? 'Saving...' : 'Save'}</button>
              <button onClick={() => setModal({ open: false })} className="px-4 py-2 border border-border rounded-lg text-sm">Cancel</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
