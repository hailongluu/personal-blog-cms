import { useState, useEffect, useCallback } from 'react';
import { tagsApi } from '@/lib/data';
import type { Tag } from '@/types';
import { Plus, Trash2, RefreshCw } from 'lucide-react';

export default function TagsPage() {
  const [tags, setTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(true);
  const [name, setName] = useState('');
  const [error, setError] = useState('');
  const [adding, setAdding] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try { setTags(await tagsApi.list()); } catch (e) { console.error(e); }
    setLoading(false);
  }, []);
  useEffect(() => { load(); }, [load]);

  async function handleAdd() {
    if (!name.trim()) return;
    setAdding(true); setError('');
    try {
      await tagsApi.create({ name: name.trim() });
      setName('');
      load();
    } catch (err: any) { setError(err.response?.data?.error?.message || 'Failed'); }
    setAdding(false);
  }

  async function handleDelete(id: number) {
    if (!confirm('Delete this tag?')) return;
    await tagsApi.delete(id);
    load();
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <div><h1 className="text-2xl font-bold text-text">Tags</h1><p className="text-text-muted text-sm">{tags.length} tags</p></div>
        <button onClick={load} className="p-2 border border-border rounded-lg hover:bg-bg"><RefreshCw size={16} /></button>
      </div>

      {/* Add form */}
      <div className="flex gap-2 mb-4">
        <input type="text" value={name} onChange={e => setName(e.target.value)} onKeyDown={e => e.key === 'Enter' && handleAdd()} placeholder="New tag name..." className="flex-1 max-w-xs px-3 py-2 border border-border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none" />
        <button onClick={handleAdd} disabled={adding} className="flex items-center gap-1 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary-dark text-sm font-medium disabled:opacity-50"><Plus size={16} /> Add</button>
      </div>
      {error && <div className="mb-3 bg-red-50 text-red-700 text-xs px-3 py-2 rounded-lg">{error}</div>}

      {/* Tag list */}
      <div className="flex flex-wrap gap-2">
        {loading && <div className="text-text-muted text-sm py-4">Loading...</div>}
        {!loading && tags.length === 0 && <div className="text-text-muted text-sm py-4">No tags yet.</div>}
        {tags.map(t => (
          <span key={t.id} className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-bg border border-border rounded-full text-sm">
            {t.name}
            <button onClick={() => handleDelete(t.id)} className="text-text-muted hover:text-red-600"><Trash2 size={13} /></button>
          </span>
        ))}
      </div>
    </div>
  );
}
