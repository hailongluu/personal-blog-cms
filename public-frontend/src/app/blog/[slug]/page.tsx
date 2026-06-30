import type { Metadata } from 'next';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { ArrowLeft, Clock, Eye, Calendar } from 'lucide-react';
import { getPostBySlug, getPublicSettings } from '@/lib/api';
import PostContent from '@/components/PostContent';
import PostToc from '@/components/PostToc';
import ReadingProgress from '@/components/ReadingProgress';
import Comments from '@/components/Comments';
import RelatedPosts from '@/components/RelatedPosts';
import { SITE_URL, SITE_NAME, AUTHOR_NAME, TWITTER_HANDLE, LOCALE, absUrl, ogImageUrl } from '@/lib/site';

export const revalidate = 60;

type Params = { params: Promise<{ slug: string }> };

function ogImageForPost(post: {
  title: string;
  ogImageUrl: string | null;
  coverImageUrl: string | null;
  topic: { name: string } | null;
  author: { displayName: string };
}) {
  // Admin-set OG image > cover image > dynamically generated /og PNG.
  if (post.ogImageUrl?.trim()) return absUrl(post.ogImageUrl);
  if (post.coverImageUrl) return absUrl(post.coverImageUrl);
  return ogImageUrl({ title: post.title, subtitle: post.topic?.name, author: post.author?.displayName });
}

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { slug } = await params;
  const post = await getPostBySlug(slug);
  if (!post) return { title: 'Không tìm thấy bài viết', robots: { index: false, follow: false } };

  // Prefer admin-set SEO fields, fall back to the post's own content.
  const metaTitle = post.metaTitle?.trim() || post.title;
  const description = post.metaDescription?.trim() || post.excerpt || `${post.title} — ${SITE_NAME}`;
  const canonical = post.canonicalUrl?.trim() || `/blog/${post.slug}`;
  const url = `${SITE_URL}/blog/${post.slug}`;
  const image = ogImageForPost(post);

  return {
    title: metaTitle,
    description,
    alternates: { canonical },
    openGraph: {
      type: 'article',
      title: metaTitle,
      description,
      url,
      siteName: SITE_NAME,
      locale: LOCALE,
      publishedTime: post.publishedAt ?? undefined,
      modifiedTime: post.updatedAt ?? undefined,
      authors: [post.author?.displayName ?? AUTHOR_NAME],
      tags: post.tags?.map((t) => t.name),
      images: [{ url: image, width: 1200, height: 630, alt: post.title }],
    },
    twitter: {
      card: 'summary_large_image',
      title: metaTitle,
      description,
      site: TWITTER_HANDLE,
      creator: TWITTER_HANDLE,
      images: [image],
    },
  };
}

function formatDate(iso: string | null): string {
  if (!iso) return '';
  return new Date(iso).toLocaleDateString('vi-VN', { year: 'numeric', month: 'long', day: 'numeric' });
}

export default async function BlogDetailPage({ params }: Params) {
  const { slug } = await params;
  const post = await getPostBySlug(slug);
  if (!post) notFound();

  const settings = await getPublicSettings();
  const allowComments = settings['posts.allow_comments'] === true;

  const url = post.canonicalUrl?.trim() || `${SITE_URL}/blog/${post.slug}`;
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'BlogPosting',
    headline: (post.metaTitle?.trim() || post.title).slice(0, 110),
    description: post.metaDescription?.trim() || post.excerpt || undefined,
    image: [ogImageForPost(post)],
    author: {
      '@type': 'Person',
      name: post.author?.displayName ?? AUTHOR_NAME,
      ...(post.author?.avatarUrl ? { image: absUrl(post.author.avatarUrl) } : {}),
    },
    publisher: { '@type': 'Person', name: AUTHOR_NAME },
    datePublished: post.publishedAt ?? undefined,
    dateModified: post.updatedAt ?? undefined,
    mainEntityOfPage: { '@type': 'WebPage', '@id': url },
    ...(post.topic ? { articleSection: post.topic.name } : {}),
    ...(post.tags?.length ? { keywords: post.tags.map((t) => t.name).join(', ') } : {}),
    inLanguage: 'vi-VN',
  };

  return (
    <>
    <ReadingProgress />
    <article className="max-w-3xl mx-auto px-4 py-12 md:py-16">
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />

      <Link href="/blog" className="inline-flex items-center gap-1 text-sm text-stone-500 hover:text-stone-700 mb-8 transition-colors">
        <ArrowLeft size={16} /> Quay lại blog
      </Link>

      <header className="mb-10">
        {post.topic && (
          <Link
            href={`/topics/${post.topic.slug}`}
            className="inline-block text-xs font-medium px-2.5 py-1 rounded-full mb-4"
            style={{ backgroundColor: (post.topic.color || '#78716c') + '20', color: post.topic.color || '#78716c' }}
          >
            {post.topic.name}
          </Link>
        )}
        <h1 className="text-3xl md:text-4xl font-bold text-stone-900 mb-4 leading-tight">{post.title}</h1>
        {post.excerpt && <p className="text-lg text-stone-500 mb-6">{post.excerpt}</p>}
        <div className="flex flex-wrap items-center gap-4 text-sm text-stone-400">
          {post.author && (
            <span className="flex items-center gap-1.5">
              {post.author.avatarUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={post.author.avatarUrl} alt="" className="w-5 h-5 rounded-full" />
              ) : (
                <span className="w-5 h-5 rounded-full bg-stone-300 inline-block" />
              )}
              {post.author.displayName}
            </span>
          )}
          {post.publishedAt && (
            <span className="flex items-center gap-1">
              <Calendar size={14} /> {formatDate(post.publishedAt)}
            </span>
          )}
          <span className="flex items-center gap-1">
            <Clock size={14} /> {post.readingTimeMin} phút đọc
          </span>
          <span className="flex items-center gap-1">
            <Eye size={14} /> {post.viewCount} lượt xem
          </span>
        </div>
      </header>

      {post.coverImageUrl && (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={post.coverImageUrl} alt={post.title} className="w-full rounded-xl mb-10 object-cover max-h-96" />
      )}

      <PostToc markdown={post.contentMarkdown} />

      {/* Rendered from contentMarkdown with custom blocks (:::takeaways/callout/
          reference) — backend doesn't reliably populate contentHtml. SSR → full
          article text is in the crawler-visible HTML. */}
      <PostContent markdown={post.contentMarkdown} />

      {post.tags?.length > 0 && (
        <div className="flex flex-wrap gap-2 mt-10 pt-6 border-t border-stone-200">
          {post.tags.map((tag) => (
            <Link
              key={tag.id}
              href={`/tags/${tag.slug}`}
              className="text-xs bg-stone-100 text-stone-600 px-2.5 py-1 rounded-full hover:bg-stone-200 transition-colors"
            >
              #{tag.name}
            </Link>
          ))}
        </div>
      )}
    </article>

    <RelatedPosts slug={post.slug} />
    {allowComments && <Comments postId={post.id} />}
    </>
  );
}
