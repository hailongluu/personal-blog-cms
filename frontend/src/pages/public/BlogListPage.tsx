import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Helmet } from 'react-helmet-async';
import { getPublishedPosts } from '@/lib/publicApi';
import type { Post } from '@/types';

const SITE_URL = 'https://task.luuhailong.com';
const SITE_NAME = 'Personal Blog';
const SITE_TITLE = 'Blog — AI, Công Nghệ & Lập Trình';
const SITE_DESCRIPTION = 'Chia sẻ kiến thức chuyên sâu về AI, công nghệ và lập trình. Bài viết phân tích, hướng dẫn và đánh giá công cụ mới nhất.';
const SITE_OG_IMAGE = `${SITE_URL}/covers/default-og.png`;

const CATEGORIES = [
  { key: 'all', label: 'ALL' },
  { key: 'ESSAY', label: 'GUIDES' },
  { key: 'RESEARCH_BRIEF', label: 'INSIGHTS' },
  { key: 'BUILD_LOG', label: 'BUILD LOGS' },
  { key: 'REVIEW', label: 'REVIEWS' },
];

const GRADIENT_BG = 'bg-gradient-to-br from-[#E8E8E1] to-[#D1D1C7]';

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  });
}

function PostCard({ post, featured = false }: { post: Post; featured?: boolean }) {
  const titleCls = featured
    ? 'text-2xl md:text-[30px] font-medium leading-[1.3] tracking-[-0.5px]'
    : 'text-xl font-medium leading-[1.3] tracking-[-0.5px]';

  return (
    <Link to={`/blog/${post.slug}`} className="contents">
      <div
        className={`relative overflow-hidden rounded-2xl shrink-0 border border-black/5 transition-all duration-700 ease-in-out
          ${featured ? 'w-full md:w-[40%] aspect-[3/2]' : 'w-full aspect-[3/2] mb-8'}
          ${post.coverImageUrl ? '' : GRADIENT_BG}`}
      >
        {post.coverImageUrl ? (
          <img
            src={post.coverImageUrl}
            alt={post.title}
            className="object-cover w-full h-full group-hover:opacity-100 group-hover:scale-[1.02] transition-all duration-1000 ease-out opacity-95"
            loading="lazy"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-[#b0a89a]">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <path d="m21 15-5-5L5 21" />
            </svg>
          </div>
        )}
      </div>

      <div className={`flex flex-col flex-grow ${featured ? 'md:w-[60%] justify-center py-2 px-2' : 'px-1'}`}>
        <div className="flex items-center gap-1.5 text-xs font-medium uppercase tracking-[0.6px] text-[#7a7a8a] mb-3">
          <span>{post.type?.replace(/_/g, ' ') || 'ESSAY'}</span>
          <span>·</span>
          {post.publishedAt && <time>{formatDate(post.publishedAt)}</time>}
          <span className="text-[9px] font-bold tracking-[0.45px] ml-1">VI</span>
        </div>

        <h2 className={`text-[#2d2d3a] group-hover:text-[#5a5a6a] transition-colors mb-2 line-clamp-2 ${titleCls}`}>
          {post.title}
        </h2>

        {featured && post.excerpt && (
          <p className="text-sm text-[#8a8a9a] leading-relaxed line-clamp-2 mb-3">{post.excerpt}</p>
        )}

        <div className="flex items-center gap-3 text-xs mt-auto">
          <span className="font-medium text-[#7a7a8a]">Long Luu</span>
          <span className="font-light uppercase tracking-[1.1px] text-[#7a7a8a] text-[11px]">
            {post.readingTimeMin || 5} MIN READ
          </span>
        </div>
      </div>
    </Link>
  );
}

export default function BlogListPage() {
  const location = useLocation();
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeCategory, setActiveCategory] = useState('all');

  useEffect(() => {
    getPublishedPosts(1, 13)
      .then((data) => setPosts(data.data))
      .catch(() => setPosts([]))
      .finally(() => setLoading(false));
  }, []);

  const filteredPosts = activeCategory === 'all'
    ? posts
    : posts.filter((p) => p.type === activeCategory);

  const featuredPost = filteredPosts[0];
  const gridPosts = filteredPosts.slice(1);

  return (
    <>
      <Helmet>
        <title>{SITE_TITLE}</title>
        <meta name="description" content={SITE_DESCRIPTION} />
        <meta name="robots" content="index, follow, max-snippet:-1, max-image-preview:large" />
        <meta name="language" content="vi" />
        <meta property="og:type" content="website" />
        <meta property="og:title" content={SITE_TITLE} />
        <meta property="og:description" content={SITE_DESCRIPTION} />
        <meta property="og:image" content={featuredPost?.coverImageUrl
          ? featuredPost.coverImageUrl.startsWith('http')
            ? featuredPost.coverImageUrl
            : `${SITE_URL}${featuredPost.coverImageUrl}`
          : SITE_OG_IMAGE} />
        <meta property="og:image:width" content="1280" />
        <meta property="og:image:height" content="720" />
        <meta property="og:url" content={`${SITE_URL}${location.pathname}`} />
        <meta property="og:site_name" content={SITE_NAME} />
        <meta property="og:locale" content="vi_VN" />
        <meta name="twitter:card" content="summary_large_image" />
        <meta name="twitter:title" content={SITE_TITLE} />
        <meta name="twitter:description" content={SITE_DESCRIPTION} />
        <meta name="twitter:image" content={featuredPost?.coverImageUrl
          ? featuredPost.coverImageUrl.startsWith('http')
            ? featuredPost.coverImageUrl
            : `${SITE_URL}${featuredPost.coverImageUrl}`
          : SITE_OG_IMAGE} />
        <link rel="canonical" href={`${SITE_URL}${location.pathname}`} />
        <script type="application/ld+json">
          {JSON.stringify({
            '@context': 'https://schema.org',
            '@type': 'Blog',
            '@id': `${SITE_URL}/blog#blog`,
            name: SITE_TITLE,
            description: SITE_DESCRIPTION,
            url: `${SITE_URL}${location.pathname}`,
            inLanguage: 'vi',
            publisher: { '@type': 'Person', name: 'Long Luu' },
            blogPost: loading ? [] : posts.slice(0, 10).map((post) => ({
              '@type': 'BlogPosting',
              '@id': `${SITE_URL}/blog/${post.slug}#post`,
              headline: post.title,
              description: post.excerpt || '',
              image: post.coverImageUrl
                ? post.coverImageUrl.startsWith('http')
                  ? post.coverImageUrl
                  : `${SITE_URL}${post.coverImageUrl}`
                : SITE_OG_IMAGE,
              url: `${SITE_URL}/blog/${post.slug}`,
              datePublished: post.publishedAt,
              dateModified: post.updatedAt || post.publishedAt,
              author: { '@type': 'Person', name: 'Long Luu' },
            })),
          })}
        </script>
      </Helmet>

      <main className="max-w-[1200px] mx-auto px-5 md:px-8 py-10 md:py-16">
        <header className="mb-6 md:mb-10">
          <h1 className="text-4xl md:text-5xl font-bold text-[#2d2d3a] tracking-tight">Blog</h1>
          <p className="text-[#7a7a8a] mt-2 text-base md:text-lg max-w-lg">
            Chia sẻ kiến thức về AI, công nghệ và lập trình.
          </p>
        </header>

        {loading ? (
          <div className="space-y-10">
            <div className="animate-pulse flex flex-col md:flex-row md:gap-16">
              <div className="w-full md:w-[40%] aspect-[3/2] bg-stone-200 rounded-2xl" />
              <div className="md:w-[60%] py-2 space-y-3">
                <div className="h-4 bg-stone-200 rounded w-1/3" />
                <div className="h-8 bg-stone-200 rounded w-3/4" />
                <div className="h-4 bg-stone-200 rounded w-full" />
              </div>
            </div>
          </div>
        ) : (
          <>
            {featuredPost && (
              <article className="group relative flex flex-col md:flex-row md:items-stretch md:gap-16 mb-16">
                <PostCard post={featuredPost} featured />
              </article>
            )}

            <div className="flex items-center gap-1 mb-12 overflow-x-auto pb-2" role="tablist" aria-label="Filter by category">
              {CATEGORIES.map((cat) => (
                <button
                  key={cat.key}
                  role="tab"
                  aria-selected={activeCategory === cat.key}
                  onClick={() => setActiveCategory(cat.key)}
                  className={`px-4 py-2 text-xs font-semibold uppercase tracking-[0.6px] rounded-full transition-colors whitespace-nowrap
                    ${activeCategory === cat.key
                      ? 'bg-[#2d2d3a] text-white'
                      : 'text-[#7a7a8a] hover:text-[#2d2d3a] hover:bg-stone-100'
                    }`}
                >
                  {cat.label}
                </button>
              ))}
            </div>

            {gridPosts.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-x-10 gap-y-16">
                {gridPosts.map((post) => (
                  <article key={post.id} className="group relative flex flex-col h-full">
                    <PostCard post={post} />
                  </article>
                ))}
              </div>
            ) : (
              <div className="text-center py-20 text-[#b0a8a0]">
                <p className="text-lg">No posts in this category yet.</p>
              </div>
            )}
          </>
        )}
      </main>
    </>
  );
}
