import { useState, type FormEvent } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { postsApi } from '@/lib/data';
import type { Post, Topic, Tag, PostType } from '@/types';
import { POST_TYPES } from '@/types';
import { ArrowLeft, Eye, Edit3 } from 'lucide-react';

interface Props {
  post: Post | null;
  topics: Topic[];
  tags: Tag[];
  onSave: () => void;
  onCancel: () => void;
}

export default function PostEditor({ post, topics, tags, onSave, onCancel }: Props) {
  const [title, setTitle] = useState(post?.title || '');
  const [slug, setSlug] = useState(post?.slug || '');
  const [subtitle, setSubtitle] = useState(post?.subtitle || '');
  const [contentMarkdown, setContentMarkdown] = useState(post?.contentMarkdown || '');
  const [excerpt, setExcerpt] = useState(post?.excerpt || '');
  const [status, setStatus] = useState(post?.status || 'draft');
  const [type, setType] = useState<PostType>(post?.type || 'essay');
  const [featured, setFeatured] = useState<boolean>(post?.featured ?? false);
  const [topicId, setTopicId] = useState<number | undefined>(post?.topic?.id);
  const [selectedTags, setSelectedTags] = useState<number[]>(post?.tags?.map(t => t.id) || []);
  const [preview, setPreview] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;
    setSaving(true);
    setError('');
    try {
      const data = {
        title: title.trim(),
        slug: slug.trim() || undefined,
        subtitle: subtitle.trim() || undefined,
        contentMarkdown,
        excerpt: excerpt.trim() || undefined,
        status,
        type,
        featured,
        topicId: topicId || undefined,
        tagIds: selectedTags.length > 0 ? selectedTags : undefined,
      };
      if (post) {
        await postsApi.update(post.id, data);
      } else {
        await postsApi.create(data);
      }
      onSave();
    } catch (err: any) {
      setError(err.response?.data?.error?.message || 'Save failed');
    } finally {
      setSaving(false);
    }
  }

  function toggleTag(id: number) {
    setSelectedTags(prev => prev.includes(id) ? prev.filter(t => t !== id) : [...prev, id]);
  }

  return (
    <div>
      <div className="flex items-center gap-4 mb-4">
        <button onClick={onCancel} className="p-2 rounded-lg hover:bg-bg"><ArrowLeft size={20} /></button>
        <h1 className="text-xl font-bold text-text">{post ? 'Edit Post' : 'New Post'}</h1>
      </div>

      {error && <div className="mb-4 bg-red-50 text-red-700 text-sm px-3 py-2 rounded-lg border border-red-200">{error}</div>}

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Title + Slug */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-text mb-1">Title *</label>
            <input
              type="text" value={title}
              onChange={e => { setTitle(e.target.value); if (!slug || slug === (post?.slug || '')) setSlug(e.target.value.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '')); }}
              required className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none"
              placeholder="Post title"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-text mb-1">Slug</label>
            <input type="text" value={slug} onChange={e => setSlug(e.target.value)} className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none text-text-muted" placeholder="auto-generated" />
            <input type="text" value={subtitle} onChange={e => setSubtitle(e.target.value)} className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none" placeholder="Subtitle (optional tagline)" maxLength={1000} />
          </div>
        </div>

        {/* Topic + Tags + Status */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-text mb-1">Topic</label>
            <select value={topicId || ''} onChange={e => setTopicId(e.target.value ? Number(e.target.value) : undefined)} className="w-full px-3 py-2 border border-border rounded-lg text-sm bg-surface">
              <option value="">None</option>
              {topics.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-text mb-1">Status</label>
            <select value={status} onChange={e => setStatus(e.target.value as Post['status'])} className="w-full px-3 py-2 border border-border rounded-lg text-sm bg-surface">
              <option value="draft">Draft</option>
              <option value="reviewing">Reviewing</option>
              <option value="published">Published</option>
              <option value="archived">Archived</option>
            </select>
            <select value={type} onChange={e => setType(e.target.value as PostType)} className="w-full px-3 py-2 border border-border rounded-lg text-sm bg-surface">
              {POST_TYPES.map(t => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
            <label className="flex items-center gap-2 text-sm cursor-pointer">
              <input
                type="checkbox"
                checked={featured}
                onChange={e => setFeatured(e.target.checked)}
                className="w-4 h-4 rounded border-border text-primary focus:ring-2 focus:ring-primary"
              />
              <span className="font-medium">Featured post</span>
              <span className="text-text-muted text-xs">(highlight on homepage)</span>
            </label>
          </div>
          <div>
            <label className="block text-sm font-medium text-text mb-1">Tags</label>
            <div className="flex flex-wrap gap-1.5">
              {tags.map(t => (
                <button key={t.id} type="button" onClick={() => toggleTag(t.id)}
                  className={`text-xs px-2 py-1 rounded-full border transition-colors ${selectedTags.includes(t.id) ? 'bg-primary text-white border-primary' : 'bg-surface text-text-muted border-border hover:border-primary'}`}
                >{t.name}</button>
              ))}
              {tags.length === 0 && <span className="text-xs text-text-muted">No tags yet</span>}
            </div>
          </div>
        </div>

        {/* Excerpt */}
        <div>
          <label className="block text-sm font-medium text-text mb-1">Excerpt</label>
          <input type="text" value={excerpt} onChange={e => setExcerpt(e.target.value)} className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none" placeholder="Short description (optional)" />
        </div>

        {/* Editor / Preview */}
        <div>
          <div className="flex items-center justify-between mb-1">
            <label className="text-sm font-medium text-text">Content (Markdown)</label>
            <button type="button" onClick={() => setPreview(!preview)}
              className="flex items-center gap-1 text-xs text-text-muted hover:text-primary transition-colors"
            >
              {preview ? <><Edit3 size={14} /> Edit</> : <><Eye size={14} /> Preview</>}
            </button>
          </div>
          {preview ? (
            <div className="min-h-[300px] p-4 border border-border rounded-lg bg-white prose prose-sm max-w-none">
              <ReactMarkdown remarkPlugins={[remarkGfm]}>{contentMarkdown || '*No content yet*'}</ReactMarkdown>
            </div>
          ) : (
            <textarea
              value={contentMarkdown}
              onChange={e => setContentMarkdown(e.target.value)}
              className="w-full min-h-[300px] px-3 py-2 border border-border rounded-lg text-sm font-mono focus:ring-2 focus:ring-primary outline-none resize-y"
              placeholder="Write your post in Markdown..."
            />
          )}
        </div>

        {/* Actions */}
        <div className="flex items-center gap-3 pt-2">
          <button type="submit" disabled={saving} className="px-6 py-2 bg-primary text-white rounded-lg hover:bg-primary-dark transition-colors text-sm font-medium disabled:opacity-50">
            {saving ? 'Saving...' : post ? 'Update Post' : 'Create Post'}
          </button>
          <button type="button" onClick={onCancel} className="px-4 py-2 border border-border rounded-lg text-sm hover:bg-bg transition-colors">Cancel</button>
        </div>
      </form>
    </div>
  );
}
