import { X, Clock, User, Tag as TagIcon } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { parseCustomBlocks, CustomBlock } from '@/components/CustomBlockRenderer';
import type { Post } from '@/types';

interface Props {
  post: Post | null;
  onClose: () => void;
}

export default function PreviewModal({ post, onClose }: Props) {
  if (!post) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center pt-8 px-4 bg-black/50 overflow-y-auto"
         onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="relative w-full max-w-3xl bg-white rounded-xl shadow-2xl max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="sticky top-0 bg-white border-b border-border px-6 py-4 flex items-center justify-between rounded-t-xl z-10">
          <div className="flex items-center gap-2">
            <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-amber-100 text-amber-800">
              DRAFT PREVIEW
            </span>
            <span className="text-xs text-text-muted">| Not visible to readers</span>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-bg transition-colors">
            <X size={20} />
          </button>
        </div>

        {/* Cover image */}
        {post.coverImageUrl && (
          <div className="w-full aspect-[2/1] overflow-hidden">
            <img src={post.coverImageUrl} alt={post.title} className="w-full h-full object-cover" />
          </div>
        )}

        {/* Content */}
        <div className="px-6 py-8">
          {/* Type badge */}
          {post.type && (
            <span className="inline-block px-2 py-0.5 text-xs font-medium rounded-full bg-primary/10 text-primary mb-3 uppercase">
              {post.type.replace('_', ' ')}
            </span>
          )}

          <h1 className="text-3xl font-bold text-text leading-tight mb-3">{post.title}</h1>

          {post.subtitle && (
            <p className="text-lg text-text-muted mb-4">{post.subtitle}</p>
          )}

          {/* Metadata */}
          <div className="flex flex-wrap items-center gap-4 mb-6 text-sm text-text-muted">
            {post.author?.displayName && (
              <div className="flex items-center gap-1">
                <User size={14} />
                <span>{post.author.displayName}</span>
              </div>
            )}
            {post.readingTimeMin != null && post.readingTimeMin > 0 && (
              <div className="flex items-center gap-1">
                <Clock size={14} />
                <span>{post.readingTimeMin} min read</span>
              </div>
            )}
            {post.topic && (
              <span className="px-2 py-0.5 rounded-full bg-bg text-text-muted text-xs">
                {post.topic.name}
              </span>
            )}
          </div>

          {/* Tags */}
          {post.tags && post.tags.length > 0 && (
            <div className="flex flex-wrap items-center gap-2 mb-6">
              <TagIcon size={14} className="text-text-muted" />
              {post.tags.map(tag => (
                <span key={tag.id} className="px-2 py-0.5 text-xs rounded-full bg-bg text-text-muted">
                  #{tag.name}
                </span>
              ))}
            </div>
          )}

          {/* Body */}
          <div className="prose prose-lg max-w-none prose-headings:text-text prose-a:text-primary prose-img:rounded-lg">
            {(() => {
              const blocks = parseCustomBlocks(post.contentMarkdown || '*No content*');
              if (blocks.length === 0) return <p className="text-text-muted italic">No content</p>;
              return blocks.map((block, idx) => {
                if (block.type === 'markdown') {
                  return <ReactMarkdown key={idx} remarkPlugins={[remarkGfm]}>{block.content}</ReactMarkdown>;
                }
                return <CustomBlock key={idx} type={block.type} content={block.content} meta={block.meta} />;
              });
            })()}
          </div>
        </div>
      </div>
    </div>
  );
}
