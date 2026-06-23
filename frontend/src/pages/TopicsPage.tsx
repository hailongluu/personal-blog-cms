import { useState, useEffect, useCallback } from 'react';
import { topicsApi } from '@/lib/data';
import type { Topic } from '@/types';
import { Plus, Pencil, Trash2, RefreshCw } from 'lucide-react';

export default function TopicsPage() {
  const [topics, setTopics] = useState<Topic[]>([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState<{ open: boolean; topic?: Topic }>({ open: false });
  const [name, setName] = useState('');
  const [desc, setDesc] = useState('');
  const [color, setColor] = useState('#059669');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try { setTopics(await topicsApi.list()); } catch (e) { console.error(e); }
    setLoading(false);
  }, []);
  useEffect(() => { load(); }, [load]);

  function openCreate() { setName(''); setDesc(''); setColor('#059669'); setError(''); setModal({ open: true }); }
  function openEdit(t: Topic) { setName(t.name); setDesc(t.description || ''); setColor(t.color || '#059669'); setError(''); setModal({ open: true, topic: t }); }

  async function handleSave() {
    if (!name.trim()) return;
    setSaving(true); setError('');
    try {
      if (modal.topic) await topicsApi.update(modal.topic.id, { name: name.trim(), description: desc.trim() || undefined, color: color || undefined });
      else await topicsApi.create({ name: name.trim(), description: desc.trim() || undefined, color: color || undefined });
      setModal({ open: false });
      load();
    } catch (err: any) { setError(err.response?.data?.error?.message || 'Save failed'); }
    setSaving(false);
  }

  async function handleDelete(id: number) {
    if (!confirm('Delete this topic?')) return;
    await topicsApi.delete(id);
    load();
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <div><h1 className="text-2xl font-bold text-text">Topics</h1><p className="text-text-muted text-sm">{topics.length} topics</p></div>
        <div className="flex gap-2">
          <button onClick={load} className="p-2 border border-border rounded-lg hover:bg-bg"><RefreshCw size={16} /></button>
          <button onClick={openCreate} className="flex items-center gap-1.5 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary-dark text-sm font-medium"><Plus size={16} /> New Topic</button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {loading && <div className="col-span-full text-center py-8 text-text-muted">Loading...</div>}
        {!loading && topics.length === 0 && <div className="col-span-full text-center py-8 text-text-muted">No topics yet.</div>}
        {topics.map(t => (
          <div key={t.id} className="bg-surface rounded-xl border border-border p-4 hover:shadow-sm transition-shadow">
            <div className="flex items-center gap-3 mb-2">
              <div className="w-3 h-3 rounded-full" style={{ backgroundColor: t.color || '#ccc' }} />
              <h3 className="font-medium text-text">{t.name}</h3>
            </div>
            {t.description && <p className="text-text-muted text-sm mb-3">{t.description}</p>}
            <div className="flex items-center gap-1">
              <button onClick={() => openEdit(t)} className="p-1.5 rounded hover:bg-bg text-text-muted hover:text-primary"><Pencil size={14} /></button>
              <button onClick={() => handleDelete(t.id)} className="p-1.5 rounded hover:bg-bg text-text-muted hover:text-red-600"><Trash2 size={14} /></button>
            </div>
          </div>
        ))}
      </div>

      {/* Modal */}
      {modal.open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setModal({ open: false })}>
          <div className="bg-surface rounded-xl shadow-lg p-6 w-full max-w-md mx-4" onClick={e => e.stopPropagation()}>
            <h2 className="text-lg font-bold text-text mb-4">{modal.topic ? 'Edit Topic' : 'New Topic'}</h2>
            {error && <div className="mb-3 bg-red-50 text-red-700 text-xs px-3 py-2 rounded-lg">{error}</div>}
            <div className="space-y-3">
              <div><label className="block text-sm font-medium mb-1">Name *</label><input type="text" value={name} onChange={e => setName(e.target.value)} className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none" /></div>
              <div><label className="block text-sm font-medium mb-1">Description</label><input type="text" value={desc} onChange={e => setDesc(e.target.value)} className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none" /></div>
              <div><label className="block text-sm font-medium mb-1">Color</label><input type="color" value={color} onChange={e => setColor(e.target.value)} className="w-10 h-10 border border-border rounded cursor-pointer" /></div>
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
