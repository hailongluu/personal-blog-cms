import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Helmet } from 'react-helmet-async';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { ArrowLeft, ExternalLink, GitFork, Code } from 'lucide-react';
import { getProjectBySlug } from '@/lib/publicApi';
import type { Project } from '@/types';

export default function ProjectDetailPage() {
  const { slug } = useParams<{ slug: string }>();
  const [project, setProject] = useState<Project | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!slug) return;
    setLoading(true);
    setError('');
    getProjectBySlug(slug)
      .then(setProject)
      .catch((err) => setError(err.response?.status === 404 ? 'Project not found' : 'Failed to load project'))
      .finally(() => setLoading(false));
  }, [slug]);

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-16">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-stone-200 rounded w-3/4" />
          <div className="h-4 bg-stone-100 rounded w-1/3" />
          <div className="h-64 bg-stone-100 rounded-xl mt-8" />
        </div>
      </div>
    );
  }

  if (error || !project) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-16 text-center">
        <h2 className="text-xl font-semibold text-stone-700 mb-4">{error || 'Project not found'}</h2>
        <Link to="/projects" className="inline-flex items-center gap-1 text-stone-500 hover:text-stone-700">
          <ArrowLeft size={16} /> Back to projects
        </Link>
      </div>
    );
  }

  return (
    <>
      <Helmet>
        <title>{project.title} — Personal Blog</title>
        <meta name="description" content={project.description || `Project: ${project.title}`} />
        <meta property="og:title" content={project.title} />
        <meta property="og:description" content={project.description || ''} />
      </Helmet>

      <article className="max-w-3xl mx-auto px-4 py-12 md:py-16">
        {/* Back link */}
        <Link to="/projects" className="inline-flex items-center gap-1 text-sm text-stone-500 hover:text-stone-700 mb-8 transition-colors">
          <ArrowLeft size={16} /> Back to projects
        </Link>

        {/* Header */}
        <header className="mb-10">
          <h1 className="text-3xl md:text-4xl font-bold text-stone-900 mb-4">{project.title}</h1>
          {project.description && (
            <p className="text-lg text-stone-500 mb-6">{project.description}</p>
          )}

          {/* Meta */}
          <div className="flex flex-wrap items-center gap-3 mb-6">
            <span className="inline-flex items-center gap-1 text-sm text-stone-500 bg-stone-100 px-2.5 py-1 rounded-full">
              <Code size={14} /> {project.status}
            </span>
            {project.isFeatured && (
              <span className="text-sm text-amber-600 bg-amber-50 px-2.5 py-1 rounded-full font-medium">
                ★ Featured
              </span>
            )}
          </div>

          {/* Action links */}
          <div className="flex flex-wrap gap-3">
            {project.projectUrl && (
              <a
                href={project.projectUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 px-4 py-2 bg-stone-900 text-white rounded-lg text-sm font-medium hover:bg-stone-800 transition-colors"
              >
                <ExternalLink size={16} /> View Live
              </a>
            )}
            {project.repoUrl && (
              <a
                href={project.repoUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 px-4 py-2 border border-stone-300 text-stone-700 rounded-lg text-sm font-medium hover:bg-stone-100 transition-colors"
              >
                <GitFork size={16} /> Source Code
              </a>
            )}
          </div>
        </header>

        {/* Cover image */}
        {project.coverImageUrl && (
          <img src={project.coverImageUrl} alt={project.title} className="w-full rounded-xl mb-10 object-cover max-h-96" />
        )}

        {/* Tech stack */}
        {project.techStack && project.techStack.length > 0 && (
          <div className="mb-10">
            <h2 className="text-sm font-semibold text-stone-500 uppercase tracking-wider mb-3">Tech Stack</h2>
            <div className="flex flex-wrap gap-2">
              {project.techStack.map((tech) => (
                <span key={tech} className="text-sm bg-stone-100 text-stone-700 px-3 py-1 rounded-full">
                  {tech}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* Content */}
        {project.contentMarkdown && (
          <div className="prose prose-stone prose-lg max-w-none">
            <ReactMarkdown remarkPlugins={[remarkGfm]}>
              {project.contentMarkdown}
            </ReactMarkdown>
          </div>
        )}
      </article>
    </>
  );
}
