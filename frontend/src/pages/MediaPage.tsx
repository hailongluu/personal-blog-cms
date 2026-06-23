import { useState, useEffect, useCallback, useRef } from 'react';
import { mediaApi } from '@/lib/data';
import type { Media } from '@/types';
import { Upload, Trash2, RefreshCw, FileText } from 'lucide-react';

export default function MediaPage() {
  const [media, setMedia] = useState<Media[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const fileRef = useRef<HTMLInputElement>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try { setMedia((await mediaApi.list()).data); } catch (e) { console.error(e); }
    setLoading(false);
  }, []);
  useEffect(() => { load(); }, [load]);

  async function handleUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true); setError('');
    try {
      await mediaApi.upload(file);
      load();
      if (fileRef.current) fileRef.current.value = '';
    } catch (err: any) { setError(err.response?.data?.error?.message || 'Upload failed'); }
    setUploading(false);
  }

  async function handleDelete(id: number) {
    if (!confirm('Delete this file?')) return;
    await mediaApi.delete(id);
    load();
  }

  function formatSize(bytes: number) {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <div><h1 className="text-2xl font-bold text-text">Media</h1><p className="text-text-muted text-sm">{media.length} files</p></div>
        <div className="flex gap-2">
          <button onClick={load} className="p-2 border border-border rounded-lg hover:bg-bg"><RefreshCw size={16} /></button>
          <label className={`flex items-center gap-1.5 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary-dark text-sm font-medium cursor-pointer transition-colors ${uploading ? 'opacity-50' : ''}`}>
            <Upload size={16} /> {uploading ? 'Uploading...' : 'Upload'}
            <input ref={fileRef} type="file" accept="image/*,.pdf" onChange={handleUpload} className="hidden" disabled={uploading} />
          </label>
        </div>
      </div>
      {error && <div className="mb-3 bg-red-50 text-red-700 text-xs px-3 py-2 rounded-lg">{error}</div>}

      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-3">
        {loading && <div className="col-span-full text-center py-8 text-text-muted">Loading...</div>}
        {!loading && media.length === 0 && <div className="col-span-full text-center py-8 text-text-muted">No files uploaded yet.</div>}
        {media.map(m => (
          <div key={m.id} className="bg-surface rounded-xl border border-border overflow-hidden group relative">
            {m.mimeType.startsWith('image/') ? (
              <img src={m.publicUrl} alt={m.altText || m.originalName} className="w-full h-32 object-cover" />
            ) : (
              <div className="w-full h-32 flex items-center justify-center bg-bg text-text-muted"><FileText size={32} /></div>
            )}
            <div className="p-2">
              <p className="text-xs text-text truncate" title={m.originalName}>{m.originalName}</p>
              <p className="text-xs text-text-muted">{formatSize(m.sizeBytes)}</p>
            </div>
            <button onClick={() => handleDelete(m.id)} className="absolute top-1 right-1 p-1 bg-white/90 rounded hover:bg-red-50 text-text-muted hover:text-red-600 opacity-0 group-hover:opacity-100 transition-opacity"><Trash2 size={14} /></button>
          </div>
        ))}
      </div>
    </div>
  );
}
