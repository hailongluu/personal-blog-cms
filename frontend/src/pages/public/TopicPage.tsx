import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Helmet } from 'react-helmet-async';
import { FileText, ArrowLeft, FolderTree } from 'lucide-react';
import { getTopicBySlug, getTopicPosts } from '@/lib/publicApi';
import type { Topic, Post, PagedResponse } from '@/types';

export default function TopicPage() {
  const { slug } = useParams<{ slug: string }>();
  const [topic, setTopic] = useState<Topic | null>(null);
  const [postsData, setPostsData] = useState<PagedResponse<Post> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!slug) return;
    setLoading(true);
    setError('');
    Promise.all([getTopicBySlug(slug), getTopicPosts(slug)])
      .then(([t, p]) => {
        setTopic(t);
        setPostsData(p);
      })
      .catch((err) => setError(err.response?.status === 404 ? 'Topic not found' : 'Failed to load topic'))
      .finally(() => setLoading(false));
  }, [slug]);

  if (loading) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-12">
        <div className="animate-pulse space-y-4 mb-10">
          <div className="h-8 bg-stone-200 rounded w-1/3" />
          <div className="h-4 bg-stone-100 rounded w-2/3" />
        </div>
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-48 bg-stone-100 rounded-xl" />
          ))}
        </div>
      </div>
    );
  }

  if (error || !topic) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-16 text-center">
        <h2 className="text-xl font-semibold text-stone-700 mb-4">{error || 'Topic not found'}</h2>
        <Link to="/blog" className="inline-flex items-center gap-1 text-stone-500 hover:text-stone-700">
          <ArrowLeft size={16} /> Back to blog
        </Link>
      </div>
    );
  }

  return (
    <>
      <Helmet>
        <title>{topic.name} — Personal Blog</title>
        <meta name="description" content={topic.description || `Posts in the ${topic.name} topic`} />
      </Helmet>

      <div className="max-w-5xl mx-auto px-4 py-12 md:py-16">
        {/* Back link */}
        <Link to="/blog" className="inline-flex items-center gap-1 text-sm text-stone-500 hover:text-stone-700 mb-6 transition-colors">
          <ArrowLeft size={16} /> Back to blog
        </Link>

        {/* Topic header */}
        <div className="mb-10">
          <div className="flex items-center gap-3 mb-3">
            {topic.icon ? (
              <span className="text-2xl">{topic.icon}</span>
            ) : (
              <FolderTree size={28} className="text-stone-400" />
            )}
            <h1 className="text-3xl md:text-4xl font-bold text-stone-900">{topic.name}</h1>
          </div>
          {topic.description && (
            <p className="text-lg text-stone-500 max-w-2xl">{topic.description}</p>
          )}
        </div>

        {/* Posts */}
        {postsData && postsData.data.length > 0 ? (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {postsData.data.map((post) => (
              <Link
                key={post.id}
                to={`/blog/${post.slug}`}
                className="group bg-white rounded-xl border border-stone-200 p-6 hover:border-stone-400 hover:shadow-md transition-all"
              >
                {post.coverImageUrl ? (
                  <img src={post.coverImageUrl} alt={post.title} className="w-full h-40 object-cover rounded-lg mb-4" loading="lazy" />
                ) : (
                  <div className="w-full h-40 bg-stone-100 rounded-lg mb-4 flex items-center justify-center">
                    <FileText size={32} className="text-stone-300" />
                  </div>
                )}
                <h2 className="font-semibold text-stone-900 group-hover:text-stone-600 transition-colors mb-2 line-clamp-2">
                  {post.title}
                </h2>
                {post.excerpt && (
                  <p className="text-sm text-stone-500 line-clamp-2">{post.excerpt}</p>
                )}
                <div className="flex items-center gap-2 mt-3 text-xs text-stone-400">
                  <span>{post.readingTimeMin} min read</span>
                  {post.publishedAt && (
                    <>
                      <span>·</span>
                      <span>{new Date(post.publishedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}</span>
                    </>
                  )}
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <div className="text-center py-12 text-stone-400">
            <FileText size={48} className="mx-auto mb-4" />
            <p>No posts in this topic yet.</p>
          </div>
        )}
      </div>
    </>
  );
}
