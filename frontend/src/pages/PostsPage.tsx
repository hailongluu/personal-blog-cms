import { useState, useEffect, useCallback } from 'react';
import { postsApi, topicsApi, tagsApi } from '@/lib/data';
import type { Post, Topic, Tag } from '@/types';
import { POST_TYPES } from '@/types';
import { Pencil, Trash2, Plus, RefreshCw, Search, Send, RotateCcw, Archive, Copy } from 'lucide-react';
import PostEditor from './PostEditor';

export default function PostsPage() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [type, setType] = useState<string>('');
  const [featured, setFeatured] = useState<string>(''); // '', 'true', 'false'
  const [editing, setEditing] = useState<Post | null>(null);
  const [creating, setCreating] = useState(false);
  const [topics, setTopics] = useState<Topic[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string | number | boolean> = {};
      if (search) params.search = search;
      if (status) params.status = status;
      if (type) params.type = type;
      if (featured) params.featured = featured === 'true';
      const res = await postsApi.list(params);
      setPosts(res.data);
      setTotal(res.meta.totalItems);
    } catch (e) {
      console.error('Failed to load posts', e);
    } finally {
      setLoading(false);
    }
  }, [search, status, type, featured]);

  useEffect(() => { load(); }, [load]);
  useEffect(() => {
    topicsApi.list().then(setTopics).catch(() => {});
    tagsApi.list().then(setTags).catch(() => {});
  }, []);

  async function handleDelete(id: number) {
    if (!confirm('Delete this post?')) return;
    await postsApi.delete(id);
    load();
  }

  async function handlePublish(id: number) {
    await postsApi.publish(id);
    load();
  }

  async function handleUnpublish(id: number) {
    await postsApi.unpublish(id);
    load();
  }

  async function handleArchive(id: number) {
    await postsApi.archive(id);
    load();
  }

  async function handleDuplicate(id: number) {
    await postsApi.duplicate(id);
    load();
  }

  function handleSaved() {
    setCreating(false);
    setEditing(null);
    load();
  }

  if (creating || editing) {
    return (
      <PostEditor
        post={editing}
        topics={topics}
        tags={tags}
        onSave={handleSaved}
        onCancel={() => { setCreating(false); setEditing(null); }}
      />
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <div>
          <h1 className="text-2xl font-bold text-text">Posts</h1>
          <p className="text-text-muted text-sm">{total} total</p>
        </div>
        <button onClick={() => setCreating(true)} className="flex items-center gap-1.5 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary-dark transition-colors text-sm font-medium">
          <Plus size={16} /> New Post
        </button>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3 mb-4">
        <div className="relative flex-1 min-w-[200px] max-w-xs">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
          <input
            type="text" placeholder="Search..."
            value={search} onChange={e => setSearch(e.target.value)}
            className="w-full pl-9 pr-3 py-2 border border-border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none"
          />
        </div>
        <select value={status} onChange={e => setStatus(e.target.value)} className="px-3 py-2 border border-border rounded-lg text-sm bg-surface">
          <option value="">All status</option>
          <option value="draft">Draft</option>
          <option value="reviewing">Reviewing</option>
          <option value="published">Published</option>
          <option value="archived">Archived</option>
        </select>
        <select value={type} onChange={e => setType(e.target.value)} className="px-3 py-2 border border-border rounded-lg text-sm bg-surface">
          <option value="">All types</option>
          {POST_TYPES.map(t => (
            <option key={t.value} value={t.value}>{t.label}</option>
          ))}
        </select>
        <select value={featured} onChange={e => setFeatured(e.target.value)} className="px-3 py-2 border border-border rounded-lg text-sm bg-surface">
          <option value="">All featured</option>
          <option value="true">Featured only</option>
          <option value="false">Not featured</option>
        </select>
        <button onClick={load} className="p-2 border border-border rounded-lg hover:bg-bg" title="Refresh">
          <RefreshCw size={16} />
        </button>
      </div>

      {/* Table */}
      <div className="bg-surface rounded-xl border border-border overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-bg border-b border-border">
            <tr>
              <th className="text-left px-4 py-3 font-medium text-text-muted">Title</th>
              <th className="text-left px-4 py-3 font-medium text-text-muted hidden md:table-cell">Status</th>
              <th className="text-left px-4 py-3 font-medium text-text-muted hidden lg:table-cell">Type</th>
              <th className="text-left px-4 py-3 font-medium text-text-muted hidden lg:table-cell">Topic</th>
              <th className="text-left px-4 py-3 font-medium text-text-muted hidden xl:table-cell">Updated</th>
              <th className="text-right px-4 py-3 font-medium text-text-muted w-44">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr><td colSpan={6} className="text-center py-12 text-text-muted">Loading...</td></tr>
            )}
            {!loading && posts.length === 0 && (
              <tr><td colSpan={6} className="text-center py-12 text-text-muted">No posts yet. Create your first!</td></tr>
            )}
            {posts.map(p => (
              <tr key={p.id} className="border-b border-border last:border-0 hover:bg-bg/50">
                <td className="px-4 py-3">
                  <div className="flex items-center gap-2">
                    {p.featured && (
                      <span className="text-amber-500" title="Featured">★</span>
                    )}
                    <span className="font-medium text-text">{p.title}</span>
                  </div>
                  <div className="text-text-muted text-xs">/{p.slug}</div>
                </td>
                <td className="px-4 py-3 hidden md:table-cell">
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                    p.status === 'published' ? 'bg-green-100 text-green-700' :
                    p.status === 'draft' ? 'bg-yellow-100 text-yellow-700' :
                    p.status === 'reviewing' ? 'bg-blue-100 text-blue-700' :
                    'bg-gray-100 text-gray-600'
                  }`}>{p.status}</span>
                </td>
                <td className="px-4 py-3 hidden lg:table-cell text-text-muted text-xs">
                  {p.type}
                </td>
                <td className="px-4 py-3 hidden lg:table-cell text-text-muted">
                  {p.topic?.name || '—'}
                </td>
                <td className="px-4 py-3 hidden xl:table-cell text-text-muted text-xs">
                  {new Date(p.updatedAt).toLocaleDateString()}
                </td>
                <td className="px-4 py-3 text-right">
                  <div className="flex items-center justify-end gap-1">
                    {p.status === 'published' ? (
                      <button onClick={() => handleUnpublish(p.id)} className="p-1.5 rounded hover:bg-bg text-text-muted hover:text-yellow-600" title="Unpublish">
                        <RotateCcw size={15} />
                      </button>
                    ) : (
                      <button onClick={() => handlePublish(p.id)} className="p-1.5 rounded hover:bg-bg text-text-muted hover:text-green-600" title="Publish">
                        <Send size={15} />
                      </button>
                    )}
                    <button onClick={() => handleDuplicate(p.id)} className="p-1.5 rounded hover:bg-bg text-text-muted hover:text-blue-600" title="Duplicate">
                      <Copy size={15} />
                    </button>
                    {p.status !== 'archived' && (
                      <button onClick={() => handleArchive(p.id)} className="p-1.5 rounded hover:bg-bg text-text-muted hover:text-gray-600" title="Archive">
                        <Archive size={15} />
                      </button>
                    )}
                    <button onClick={() => setEditing(p)} className="p-1.5 rounded hover:bg-bg text-text-muted hover:text-primary" title="Edit">
                      <Pencil size={15} />
                    </button>
                    <button onClick={() => handleDelete(p.id)} className="p-1.5 rounded hover:bg-bg text-text-muted hover:text-red-600" title="Delete">
                      <Trash2 size={15} />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
