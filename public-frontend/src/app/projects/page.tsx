import type { Metadata } from 'next';
import Link from 'next/link';
import { ExternalLink, GitFork, Code, Briefcase } from 'lucide-react';
import { getProjects } from '@/lib/api';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Projects',
  description: 'Các dự án mã nguồn mở và side project tôi đã xây dựng.',
  alternates: { canonical: '/projects' },
};

export default async function ProjectsPage() {
  const projects = await getProjects();

  return (
    <div className="max-w-5xl mx-auto px-4 py-12 md:py-16">
      <header className="mb-10">
        <h1 className="text-3xl md:text-4xl font-bold text-stone-900 mb-3">Projects</h1>
        <p className="text-stone-500 text-lg">Những thứ tôi đã xây dựng và phát hành.</p>
      </header>

      {projects.length > 0 ? (
        <div className="grid gap-6 md:grid-cols-2">
          {projects.map((project) => (
            <Link
              key={project.id}
              href={`/projects/${project.slug}`}
              className="group bg-white rounded-xl border border-stone-200 p-6 hover:border-stone-400 hover:shadow-md transition-all"
            >
              {project.coverImageUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={project.coverImageUrl} alt={project.title} className="w-full h-44 object-cover rounded-lg mb-4" loading="lazy" />
              ) : (
                <div className="w-full h-44 bg-stone-100 rounded-lg mb-4 flex items-center justify-center">
                  <Briefcase size={36} className="text-stone-300" />
                </div>
              )}
              <h2 className="font-semibold text-stone-900 group-hover:text-stone-600 transition-colors mb-2">{project.title}</h2>
              {project.description && <p className="text-sm text-stone-500 line-clamp-2">{project.description}</p>}

              {project.techStack?.length > 0 && (
                <div className="flex flex-wrap gap-1.5 mt-4">
                  {project.techStack.slice(0, 5).map((tech) => (
                    <span key={tech} className="text-xs bg-stone-100 text-stone-600 px-2 py-0.5 rounded-full">{tech}</span>
                  ))}
                  {project.techStack.length > 5 && <span className="text-xs text-stone-400">+{project.techStack.length - 5}</span>}
                </div>
              )}

              <div className="flex items-center gap-3 mt-4 pt-4 border-t border-stone-100">
                {project.projectUrl && <span className="inline-flex items-center gap-1 text-xs text-stone-500"><ExternalLink size={14} /> Live</span>}
                {project.repoUrl && <span className="inline-flex items-center gap-1 text-xs text-stone-500"><GitFork size={14} /> Repo</span>}
                <span className="inline-flex items-center gap-1 text-xs text-stone-400 ml-auto"><Code size={14} /> {project.status}</span>
              </div>
            </Link>
          ))}
        </div>
      ) : (
        <div className="text-center py-16 text-stone-400">
          <Briefcase size={48} className="mx-auto mb-4" />
          <p className="text-lg">Chưa có dự án nào.</p>
        </div>
      )}
    </div>
  );
}
