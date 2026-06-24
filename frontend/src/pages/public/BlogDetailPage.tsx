import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Helmet } from 'react-helmet-async';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { ArrowLeft, Clock, Eye, Calendar } from 'lucide-react';
import { getPostBySlug } from '@/lib/publicApi';
import { parseCustomBlocks, CustomBlock } from '@/components/CustomBlockRenderer';
import type { Post } from '@/types';

export default function BlogDetailPage() {
  const { slug } = useParams<{ slug: string }>();
  const [post, setPost] = useState<Post | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!slug) return;
    setLoading(true);
    setError('');
    getPostBySlug(slug)
      .then(setPost)
      .catch((err) => setError(err.response?.status === 404 ? 'Post not found' : 'Failed to load post'))
      .finally(() => setLoading(false));
  }, [slug]);

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-16">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-stone-200 rounded w-3/4" />
          <div className="h-4 bg-stone-100 rounded w-1/4" />
          <div className="h-64 bg-stone-100 rounded-xl mt-8" />
          <div className="space-y-3 mt-8">
            <div className="h-4 bg-stone-100 rounded w-full" />
            <div className="h-4 bg-stone-100 rounded w-5/6" />
            <div className="h-4 bg-stone-100 rounded w-4/6" />
          </div>
        </div>
      </div>
    );
  }

  if (error || !post) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-16 text-center">
        <h2 className="text-xl font-semibold text-stone-700 mb-4">{error || 'Post not found'}</h2>
        <Link to="/blog" className="inline-flex items-center gap-1 text-stone-500 hover:text-stone-700 transition-colors">
          <ArrowLeft size={16} /> Back to blog
        </Link>
      </div>
    );
  }

  return (
    <>
      <Helmet>
        <title>{post.title} — Personal Blog</title>
        <meta name="description" content={post.excerpt || `Read ${post.title} on Personal Blog`} />
        <meta property="og:title" content={post.title} />
        <meta property="og:description" content={post.excerpt || ''} />
        {post.coverImageUrl && <meta property="og:image" content={post.coverImageUrl} />}
        <meta property="og:type" content="article" />
      </Helmet>

      <article className="max-w-3xl mx-auto px-4 py-12 md:py-16">
        {/* Back link */}
        <Link to="/blog" className="inline-flex items-center gap-1 text-sm text-stone-500 hover:text-stone-700 mb-8 transition-colors">
          <ArrowLeft size={16} /> Back to blog
        </Link>

        {/* Header */}
        <header className="mb-10">
          {post.topic && (
            <Link
              to={`/topics/${post.topic.slug}`}
              className="inline-block text-xs font-medium px-2.5 py-1 rounded-full mb-4"
              style={{ backgroundColor: post.topic.color + '20', color: post.topic.color }}
            >
              {post.topic.name}
            </Link>
          )}
          <h1 className="text-3xl md:text-4xl font-bold text-stone-900 mb-4 leading-tight">
            {post.title}
          </h1>
          {post.excerpt && (
            <p className="text-lg text-stone-500 mb-6">{post.excerpt}</p>
          )}
          <div className="flex flex-wrap items-center gap-4 text-sm text-stone-400">
            {post.author && (
              <span className="flex items-center gap-1.5">
                {post.author.avatarUrl ? (
                  <img src={post.author.avatarUrl} alt="" className="w-5 h-5 rounded-full" />
                ) : (
                  <div className="w-5 h-5 rounded-full bg-stone-300" />
                )}
                {post.author.displayName}
              </span>
            )}
            {post.publishedAt && (
              <span className="flex items-center gap-1">
                <Calendar size={14} />
                {new Date(post.publishedAt).toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })}
              </span>
            )}
            <span className="flex items-center gap-1">
              <Clock size={14} /> {post.readingTimeMin} min read
            </span>
            <span className="flex items-center gap-1">
              <Eye size={14} /> {post.viewCount} views
            </span>
          </div>
        </header>

        {/* Cover image */}
        {post.coverImageUrl && (
          <img
            src={post.coverImageUrl}
            alt={post.title}
            className="w-full rounded-xl mb-10 object-cover max-h-96"
          />
        )}

        {/* Content */}
        <div className="prose prose-stone prose-lg max-w-none">
          {(() => {
            const markdown = post.contentMarkdown?.replace(/^# .+\n\n?/, '') || '';
            const blocks = parseCustomBlocks(markdown);
            if (blocks.length === 0) return null;
            return blocks.map((block, idx) => {
              if (block.type === 'markdown') {
                return <ReactMarkdown key={idx} remarkPlugins={[remarkGfm]}>{block.content}</ReactMarkdown>;
              }
              return <CustomBlock key={idx} type={block.type} content={block.content} meta={block.meta} />;
            });
          })()}
        </div>

        {/* Tags */}
        {post.tags && post.tags.length > 0 && (
          <div className="flex flex-wrap gap-2 mt-10 pt-6 border-t border-stone-200">
            {post.tags.map((tag) => (
              <span
                key={tag.id}
                className="text-xs bg-stone-100 text-stone-600 px-2.5 py-1 rounded-full"
              >
                #{tag.name}
              </span>
            ))}
          </div>
        )}
      </article>
    </>
  );
}
