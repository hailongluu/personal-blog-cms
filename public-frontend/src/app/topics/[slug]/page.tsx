import type { Metadata } from 'next';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { ArrowLeft, FileText } from 'lucide-react';
import { getTopicBySlug, getTopicPosts } from '@/lib/api';
import { SITE_URL } from '@/lib/site';
import PostCard from '@/components/PostCard';

export const revalidate = 60;

type Params = { params: Promise<{ slug: string }> };

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { slug } = await params;
  const topic = await getTopicBySlug(slug);
  if (!topic) return { title: 'Không tìm thấy chủ đề', robots: { index: false, follow: false } };
  const description = topic.description || `Bài viết trong chủ đề ${topic.name}`;
  return {
    title: topic.name,
    description,
    alternates: { canonical: `/topics/${topic.slug}` },
    openGraph: { type: 'website', title: topic.name, description, url: `${SITE_URL}/topics/${topic.slug}` },
  };
}

export default async function TopicPage({ params }: Params) {
  const { slug } = await params;
  const [topic, postsData] = await Promise.all([getTopicBySlug(slug), getTopicPosts(slug, 1, 30)]);
  if (!topic) notFound();

  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'CollectionPage',
    name: topic.name,
    description: topic.description || undefined,
    url: `${SITE_URL}/topics/${topic.slug}`,
    inLanguage: 'vi-VN',
  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-12 md:py-16">
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />

      <Link href="/blog" className="inline-flex items-center gap-1 text-sm text-stone-500 hover:text-stone-700 mb-6 transition-colors">
        <ArrowLeft size={16} /> Quay lại blog
      </Link>

      <header className="mb-10">
        <div className="flex items-center gap-3 mb-3">
          {topic.icon && <span className="text-2xl">{topic.icon}</span>}
          <h1 className="text-3xl md:text-4xl font-bold text-stone-900">{topic.name}</h1>
        </div>
        {topic.description && <p className="text-lg text-stone-500 max-w-2xl">{topic.description}</p>}
      </header>

      {postsData.data.length > 0 ? (
        <div className="grid gap-10 sm:grid-cols-2 lg:grid-cols-3">
          {postsData.data.map((post) => (
            <PostCard key={post.id} post={post} />
          ))}
        </div>
      ) : (
        <div className="text-center py-12 text-stone-400">
          <FileText size={48} className="mx-auto mb-4" />
          <p>Chưa có bài viết nào trong chủ đề này.</p>
        </div>
      )}
    </div>
  );
}
