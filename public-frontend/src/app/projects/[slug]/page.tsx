import type { Metadata } from 'next';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { ArrowLeft, ExternalLink, GitFork, Code } from 'lucide-react';
import { getProjectBySlug } from '@/lib/api';
import { SITE_URL, AUTHOR_NAME, absUrl, ogImageUrl } from '@/lib/site';

export const revalidate = 60;

type Params = { params: Promise<{ slug: string }> };

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { slug } = await params;
  const project = await getProjectBySlug(slug);
  if (!project) return { title: 'Không tìm thấy dự án', robots: { index: false, follow: false } };
  const description = project.description || `Dự án: ${project.title}`;
  const image = project.coverImageUrl ? absUrl(project.coverImageUrl) : ogImageUrl({ title: project.title, subtitle: 'Project' });
  return {
    title: project.title,
    description,
    alternates: { canonical: `/projects/${project.slug}` },
    openGraph: {
      type: 'article',
      title: project.title,
      description,
      url: `${SITE_URL}/projects/${project.slug}`,
      images: [{ url: image, width: 1200, height: 630, alt: project.title }],
    },
    twitter: { card: 'summary_large_image', title: project.title, description, images: [image] },
  };
}

export default async function ProjectDetailPage({ params }: Params) {
  const { slug } = await params;
  const project = await getProjectBySlug(slug);
  if (!project) notFound();

  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'SoftwareSourceCode',
    name: project.title,
    description: project.description || undefined,
    url: `${SITE_URL}/projects/${project.slug}`,
    ...(project.repoUrl ? { codeRepository: project.repoUrl } : {}),
    ...(project.techStack?.length ? { programmingLanguage: project.techStack } : {}),
    author: { '@type': 'Person', name: AUTHOR_NAME },
  };

  return (
    <article className="max-w-3xl mx-auto px-4 py-12 md:py-16">
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />

      <Link href="/projects" className="inline-flex items-center gap-1 text-sm text-stone-500 hover:text-stone-700 mb-8 transition-colors">
        <ArrowLeft size={16} /> Quay lại projects
      </Link>

      <header className="mb-10">
        <h1 className="text-3xl md:text-4xl font-bold text-stone-900 mb-4">{project.title}</h1>
        {project.description && <p className="text-lg text-stone-500 mb-6">{project.description}</p>}

        <div className="flex flex-wrap items-center gap-3 mb-6">
          <span className="inline-flex items-center gap-1 text-sm text-stone-500 bg-stone-100 px-2.5 py-1 rounded-full">
            <Code size={14} /> {project.status}
          </span>
          {project.isFeatured && <span className="text-sm text-amber-600 bg-amber-50 px-2.5 py-1 rounded-full font-medium">★ Featured</span>}
        </div>

        <div className="flex flex-wrap gap-3">
          {project.projectUrl && (
            <a href={project.projectUrl} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-2 px-4 py-2 bg-stone-900 text-white rounded-lg text-sm font-medium hover:bg-stone-800 transition-colors">
              <ExternalLink size={16} /> View Live
            </a>
          )}
          {project.repoUrl && (
            <a href={project.repoUrl} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-2 px-4 py-2 border border-stone-300 text-stone-700 rounded-lg text-sm font-medium hover:bg-stone-100 transition-colors">
              <GitFork size={16} /> Source Code
            </a>
          )}
        </div>
      </header>

      {project.coverImageUrl && (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={project.coverImageUrl} alt={project.title} className="w-full rounded-xl mb-10 object-cover max-h-96" />
      )}

      {project.techStack?.length > 0 && (
        <div className="mb-10">
          <h2 className="text-sm font-semibold text-stone-500 uppercase tracking-wider mb-3">Tech Stack</h2>
          <div className="flex flex-wrap gap-2">
            {project.techStack.map((tech) => (
              <span key={tech} className="text-sm bg-stone-100 text-stone-700 px-3 py-1 rounded-full">{tech}</span>
            ))}
          </div>
        </div>
      )}

      {project.contentMarkdown && (
        <div className="prose prose-stone prose-lg max-w-none">
          <ReactMarkdown remarkPlugins={[remarkGfm]}>{project.contentMarkdown}</ReactMarkdown>
        </div>
      )}
    </article>
  );
}
