import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Helmet } from 'react-helmet-async';
import { ArrowRight, FileText } from 'lucide-react';
import { getFeaturedPosts } from '@/lib/publicApi';
import type { Post } from '@/types';

export default function HomePage() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getFeaturedPosts(3)
      .then(setPosts)
      .catch(() => setPosts([]))
      .finally(() => setLoading(false));
  }, []);

  return (
    <>
      <Helmet>
        <title>Personal Blog — Thoughts on code, design, and life</title>
        <meta name="description" content="A personal blog about software development, design, and technology." />
      </Helmet>

      {/* Hero */}
      <section className="bg-white border-b border-stone-200">
        <div className="max-w-5xl mx-auto px-4 py-20 md:py-28 text-center">
          <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold tracking-tight text-stone-900 mb-6">
            Thoughts on{' '}
            <span className="text-stone-500">code, design,</span>
            <br />
            <span className="text-stone-500">and life</span>
          </h1>
          <p className="text-lg text-stone-500 max-w-xl mx-auto mb-8">
            Sharing ideas, lessons learned, and projects I'm working on.
            Explore articles on web development, open source, and creative coding.
          </p>
          <div className="flex items-center justify-center gap-4">
            <Link
              to="/blog"
              className="inline-flex items-center gap-2 px-6 py-3 bg-stone-900 text-white rounded-lg font-medium hover:bg-stone-800 transition-colors"
            >
              Read the blog <ArrowRight size={18} />
            </Link>
            <Link
              to="/about"
              className="inline-flex items-center gap-2 px-6 py-3 border border-stone-300 text-stone-700 rounded-lg font-medium hover:bg-stone-100 transition-colors"
            >
              About me
            </Link>
          </div>
        </div>
      </section>

      {/* Featured Posts */}
      <section className="max-w-5xl mx-auto px-4 py-16 md:py-20">
        <div className="flex items-center justify-between mb-10">
          <h2 className="text-2xl font-bold text-stone-900">Latest posts</h2>
          <Link to="/blog" className="text-sm font-medium text-stone-500 hover:text-stone-700 transition-colors">
            View all →
          </Link>
        </div>

        {loading ? (
          <div className="grid gap-6 md:grid-cols-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="bg-white rounded-xl border border-stone-200 p-6 animate-pulse">
                <div className="h-4 bg-stone-200 rounded w-3/4 mb-3" />
                <div className="h-3 bg-stone-100 rounded w-full mb-2" />
                <div className="h-3 bg-stone-100 rounded w-2/3" />
              </div>
            ))}
          </div>
        ) : posts.length > 0 ? (
          <div className="grid gap-6 md:grid-cols-3">
            {posts.map((post) => (
              <Link
                key={post.id}
                to={`/blog/${post.slug}`}
                className="group bg-white rounded-xl border border-stone-200 p-6 hover:border-stone-400 hover:shadow-md transition-all"
              >
                {post.coverImageUrl ? (
                  <img
                    src={post.coverImageUrl}
                    alt={post.title}
                    className="w-full h-40 object-cover rounded-lg mb-4"
                    loading="lazy"
                  />
                ) : (
                  <div className="w-full h-40 bg-stone-100 rounded-lg mb-4 flex items-center justify-center">
                    <FileText size={32} className="text-stone-300" />
                  </div>
                )}
                {post.topic && (
                  <span
                    className="inline-block text-xs font-medium px-2 py-0.5 rounded-full mb-2"
                    style={{ backgroundColor: post.topic.color + '20', color: post.topic.color }}
                  >
                    {post.topic.name}
                  </span>
                )}
                <h3 className="font-semibold text-stone-900 group-hover:text-stone-600 transition-colors mb-2 line-clamp-2">
                  {post.title}
                </h3>
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
            <p>No posts yet. Check back soon!</p>
          </div>
        )}
      </section>

      {/* CTA */}
      <section className="bg-stone-900 text-white">
        <div className="max-w-5xl mx-auto px-4 py-16 md:py-20 text-center">
          <h2 className="text-2xl md:text-3xl font-bold mb-4">Stay in the loop</h2>
          <p className="text-stone-400 max-w-md mx-auto mb-8">
            Get notified when I publish new posts. No spam, unsubscribe anytime.
          </p>
          <Link
            to="/newsletter"
            className="inline-flex items-center gap-2 px-6 py-3 bg-white text-stone-900 rounded-lg font-medium hover:bg-stone-100 transition-colors"
          >
            Subscribe to newsletter <ArrowRight size={18} />
          </Link>
        </div>
      </section>
    </>
  );
}
