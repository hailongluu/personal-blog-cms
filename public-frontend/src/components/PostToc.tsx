import GithubSlugger from 'github-slugger';

// Parse h2/h3 from markdown and slug them the same way rehype-slug does (both use
// github-slugger in document order) so anchor links match the rendered heading ids.
function extractHeadings(markdown: string) {
  const slugger = new GithubSlugger();
  const out: { level: number; text: string; id: string }[] = [];
  let inCode = false;
  for (const line of (markdown || '').split('\n')) {
    if (line.trim().startsWith('```')) {
      inCode = !inCode;
      continue;
    }
    if (inCode) continue;
    const m = line.match(/^(#{2,3})\s+(.+?)\s*#*$/);
    if (m) {
      const text = m[2].replace(/[*_`]/g, '').trim();
      out.push({ level: m[1].length, text, id: slugger.slug(text) });
    }
  }
  return out;
}

export default function PostToc({ markdown }: { markdown: string }) {
  const headings = extractHeadings((markdown || '').replace(/^# .+\n\n?/, ''));
  if (headings.length < 3) return null;

  return (
    <nav className="mb-10 rounded-xl border border-stone-200 dark:border-stone-700 bg-stone-50 dark:bg-stone-800/40 p-5">
      <p className="text-xs font-semibold uppercase tracking-wide text-stone-400 mb-3">Mục lục</p>
      <ul className="space-y-1.5 text-sm">
        {headings.map((h, i) => (
          <li key={i} className={h.level === 3 ? 'pl-4' : ''}>
            <a href={`#${h.id}`} className="text-stone-600 dark:text-stone-300 hover:text-stone-900 dark:hover:text-white hover:underline">
              {h.text}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  );
}
