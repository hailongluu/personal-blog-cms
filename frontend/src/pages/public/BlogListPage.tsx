import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Helmet } from 'react-helmet-async';
import { FileText, ChevronLeft, ChevronRight } from 'lucide-react';
import { getPublishedPosts } from '@/lib/publicApi';
import type { Post, PagedResponse } from '@/types';

export default function BlogListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [data, setData] = useState<PagedResponse<Post> | null>(null);
  const [loading, setLoading] = useState(true);

  const page = Number(searchParams.get('page') || 1);
  const pageSize = 9;

  useEffect(() => {
    setLoading(true);
    getPublishedPosts(page, pageSize)
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, [page]);

  const totalPages = data?.meta?.totalPages ?? 1;

  function goToPage(p: number) {
    if (p < 1 || p > totalPages) return;
    setSearchParams({ page: String(p) });
  }

  return (
    <>
      <Helmet>
        <title>Blog — Personal Blog</title>
        <meta name="description" content="Articles about web development, design, and technology." />
      </Helmet>

      <div className="max-w-5xl mx-auto px-4 py-12 md:py-16">
        {/* Header */}
        <div className="mb-10">
          <h1 className="text-3xl md:text-4xl font-bold text-stone-900 mb-3">Blog</h1>
          <p className="text-stone-500 text-lg">Articles on code, design, and everything in between.</p>
        </div>

        {/* Posts Grid */}
        {loading ? (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="bg-white rounded-xl border border-stone-200 p-6 animate-pulse">
                <div className="h-4 bg-stone-200 rounded w-3/4 mb-3" />
                <div className="h-3 bg-stone-100 rounded w-full mb-2" />
                <div className="h-3 bg-stone-100 rounded w-2/3" />
              </div>
            ))}
          </div>
        ) : data && data.data.length > 0 ? (
          <>
            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {data.data.map((post) => (
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

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-2 mt-12">
                <button
                  onClick={() => goToPage(page - 1)}
                  disabled={page <= 1}
                  className="p-2 rounded-lg border border-stone-200 text-stone-600 hover:bg-stone-100 disabled:opacity-30 disabled:cursor-not-allowed"
                >
                  <ChevronLeft size={18} />
                </button>
                {Array.from({ length: totalPages }).map((_, i) => (
                  <button
                    key={i}
                    onClick={() => goToPage(i + 1)}
                    className={`w-10 h-10 rounded-lg text-sm font-medium transition-colors ${
                      page === i + 1
                        ? 'bg-stone-900 text-white'
                        : 'border border-stone-200 text-stone-600 hover:bg-stone-100'
                    }`}
                  >
                    {i + 1}
                  </button>
                ))}
                <button
                  onClick={() => goToPage(page + 1)}
                  disabled={page >= totalPages}
                  className="p-2 rounded-lg border border-stone-200 text-stone-600 hover:bg-stone-100 disabled:opacity-30 disabled:cursor-not-allowed"
                >
                  <ChevronRight size={18} />
                </button>
              </div>
            )}
          </>
        ) : (
          <div className="text-center py-16 text-stone-400">
            <FileText size={48} className="mx-auto mb-4" />
            <p className="text-lg">No published posts yet.</p>
          </div>
        )}
      </div>
    </>
  );
}
