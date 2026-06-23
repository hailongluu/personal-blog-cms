import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Helmet } from 'react-helmet-async';
import { ExternalLink, GitFork, Code, Briefcase } from 'lucide-react';
import { getProjects } from '@/lib/publicApi';
import type { Project, PagedResponse } from '@/types';

export default function ProjectsPage() {
  const [data, setData] = useState<PagedResponse<Project> | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getProjects()
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, []);

  return (
    <>
      <Helmet>
        <title>Projects — Personal Blog</title>
        <meta name="description" content="Open source projects and side projects I've built." />
      </Helmet>

      <div className="max-w-5xl mx-auto px-4 py-12 md:py-16">
        {/* Header */}
        <div className="mb-10">
          <h1 className="text-3xl md:text-4xl font-bold text-stone-900 mb-3">Projects</h1>
          <p className="text-stone-500 text-lg">Things I've built and shipped.</p>
        </div>

        {loading ? (
          <div className="grid gap-6 md:grid-cols-2">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="bg-white rounded-xl border border-stone-200 p-6 animate-pulse">
                <div className="h-5 bg-stone-200 rounded w-1/2 mb-3" />
                <div className="h-3 bg-stone-100 rounded w-full mb-2" />
                <div className="h-3 bg-stone-100 rounded w-3/4" />
              </div>
            ))}
          </div>
        ) : data && data.data.length > 0 ? (
          <div className="grid gap-6 md:grid-cols-2">
            {data.data.map((project) => (
              <Link
                key={project.id}
                to={`/projects/${project.slug}`}
                className="group bg-white rounded-xl border border-stone-200 p-6 hover:border-stone-400 hover:shadow-md transition-all"
              >
                {project.coverImageUrl ? (
                  <img src={project.coverImageUrl} alt={project.title} className="w-full h-44 object-cover rounded-lg mb-4" loading="lazy" />
                ) : (
                  <div className="w-full h-44 bg-stone-100 rounded-lg mb-4 flex items-center justify-center">
                    <Briefcase size={36} className="text-stone-300" />
                  </div>
                )}
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <h2 className="font-semibold text-stone-900 group-hover:text-stone-600 transition-colors mb-2">
                      {project.title}
                    </h2>
                    {project.description && (
                      <p className="text-sm text-stone-500 line-clamp-2">{project.description}</p>
                    )}
                  </div>
                </div>

                {/* Tech stack */}
                {project.techStack && project.techStack.length > 0 && (
                  <div className="flex flex-wrap gap-1.5 mt-4">
                    {project.techStack.slice(0, 5).map((tech) => (
                      <span key={tech} className="text-xs bg-stone-100 text-stone-600 px-2 py-0.5 rounded-full">
                        {tech}
                      </span>
                    ))}
                    {project.techStack.length > 5 && (
                      <span className="text-xs text-stone-400">+{project.techStack.length - 5}</span>
                    )}
                  </div>
                )}

                {/* Links */}
                <div className="flex items-center gap-3 mt-4 pt-4 border-t border-stone-100">
                  {project.projectUrl && (
                    <span className="inline-flex items-center gap-1 text-xs text-stone-500 hover:text-stone-700">
                      <ExternalLink size={14} /> Live
                    </span>
                  )}
                  {project.repoUrl && (
                    <span className="inline-flex items-center gap-1 text-xs text-stone-500 hover:text-stone-700">
                      <GitFork size={14} /> Repo
                    </span>
                  )}
                  <span className="inline-flex items-center gap-1 text-xs text-stone-400 ml-auto">
                    <Code size={14} /> {project.status}
                  </span>
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <div className="text-center py-16 text-stone-400">
            <Briefcase size={48} className="mx-auto mb-4" />
            <p className="text-lg">No projects to show yet.</p>
          </div>
        )}
      </div>
    </>
  );
}
