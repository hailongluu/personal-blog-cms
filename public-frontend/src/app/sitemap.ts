import type { MetadataRoute } from 'next';
import { SITE_URL } from '@/lib/site';
import { getAllPostSlugs, getTopics, getProjects } from '@/lib/api';

export const dynamic = 'force-dynamic';

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const [posts, topics, projects] = await Promise.all([
    getAllPostSlugs(),
    getTopics(),
    getProjects(),
  ]);

  const staticRoutes: MetadataRoute.Sitemap = [
    { url: `${SITE_URL}/`, changeFrequency: 'daily', priority: 1.0 },
    { url: `${SITE_URL}/blog`, changeFrequency: 'daily', priority: 0.9 },
    { url: `${SITE_URL}/projects`, changeFrequency: 'monthly', priority: 0.6 },
    { url: `${SITE_URL}/about`, changeFrequency: 'monthly', priority: 0.6 },
    { url: `${SITE_URL}/now`, changeFrequency: 'monthly', priority: 0.5 },
    { url: `${SITE_URL}/newsletter`, changeFrequency: 'monthly', priority: 0.7 },
  ];

  const postRoutes: MetadataRoute.Sitemap = posts.map((p) => ({
    url: `${SITE_URL}/blog/${p.slug}`,
    lastModified: p.updatedAt ? new Date(p.updatedAt) : undefined,
    changeFrequency: 'weekly',
    priority: 0.8,
  }));

  const topicRoutes: MetadataRoute.Sitemap = topics.map((t) => ({
    url: `${SITE_URL}/topics/${t.slug}`,
    changeFrequency: 'weekly',
    priority: 0.6,
  }));

  const projectRoutes: MetadataRoute.Sitemap = projects.map((pr) => ({
    url: `${SITE_URL}/projects/${pr.slug}`,
    changeFrequency: 'monthly',
    priority: 0.5,
  }));

  return [...staticRoutes, ...postRoutes, ...topicRoutes, ...projectRoutes];
}
