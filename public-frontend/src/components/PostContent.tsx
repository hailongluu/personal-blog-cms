import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Lightbulb, AlertTriangle, BookOpen } from 'lucide-react';

// Ported from frontend/src/components/CustomBlockRenderer.tsx so the Next public
// site renders post bodies identically. The canonical source is contentMarkdown
// (backend does NOT generate contentHtml — it stores whatever the editor sent,
// often empty), parsed for MDX-like custom blocks: :::takeaways / :::callout /
// :::reference, with the rest rendered as GitHub-flavored markdown. Server
// component → the full article is in the SSR HTML for crawlers.

type BlockType = 'markdown' | 'takeaways' | 'callout' | 'reference';
type Block = { type: BlockType; content: string; meta?: Record<string, string> };

export function parseCustomBlocks(markdown: string): Block[] {
  const blocks: Block[] = [];
  const lines = markdown.split('\n');
  let i = 0;
  let markdownBuf = '';

  while (i < lines.length) {
    const line = lines[i];
    const match = line.match(/^:::(\w+)(?:\{([^}]+)\})?\s*$/);
    if (match) {
      if (markdownBuf.trim()) {
        blocks.push({ type: 'markdown', content: markdownBuf.trim() });
        markdownBuf = '';
      }
      const blockType = match[1] as BlockType;
      const metaStr = match[2] || '';
      const meta: Record<string, string> = {};
      if (metaStr) {
        metaStr.split(',').forEach((pair) => {
          const [k, v] = pair.split('=').map((s) => s.trim().replace(/^"|"$/g, ''));
          if (k && v) meta[k] = v;
        });
      }
      let blockContent = '';
      i++;
      while (i < lines.length && !lines[i].match(/^:::\s*$/)) {
        blockContent += (blockContent ? '\n' : '') + lines[i];
        i++;
      }
      blocks.push({ type: blockType, content: blockContent, meta });
    } else {
      markdownBuf += (markdownBuf ? '\n' : '') + line;
    }
    i++;
  }
  if (markdownBuf.trim()) blocks.push({ type: 'markdown', content: markdownBuf.trim() });
  return blocks;
}

const blockStyles: Record<string, { icon: React.ReactNode; bg: string; border: string; text: string }> = {
  takeaways: { icon: <Lightbulb size={18} />, bg: 'bg-amber-50', border: 'border-amber-200', text: 'text-amber-900' },
  callout: { icon: <AlertTriangle size={18} />, bg: 'bg-orange-50', border: 'border-orange-200', text: 'text-orange-900' },
  reference: { icon: <BookOpen size={18} />, bg: 'bg-blue-50', border: 'border-blue-200', text: 'text-blue-900' },
};

function CustomBlock({ type, content, meta }: Block) {
  const style = blockStyles[type];
  if (!style) {
    return (
      <div className="p-4 border border-stone-200 rounded-lg bg-stone-50 my-4">
        <pre className="text-sm whitespace-pre-wrap">{content}</pre>
      </div>
    );
  }
  const title =
    type === 'takeaways' ? 'Key Takeaways'
    : type === 'callout' ? (meta?.type === 'info' ? 'Info' : meta?.type === 'tip' ? 'Tip' : 'Warning')
    : type === 'reference' ? 'Reference'
    : type;
  return (
    <div className={`not-prose p-4 border rounded-lg my-4 ${style.bg} ${style.border}`}>
      <div className={`flex items-center gap-2 font-semibold text-sm mb-2 ${style.text}`}>
        {style.icon}
        <span>{title}</span>
      </div>
      <div className={`text-sm ${style.text} opacity-80 whitespace-pre-wrap`}>{content}</div>
    </div>
  );
}

export default function PostContent({ markdown }: { markdown: string }) {
  // Strip a leading H1 (the title is rendered separately in the page header).
  const cleaned = (markdown || '').replace(/^# .+\n\n?/, '');
  const blocks = parseCustomBlocks(cleaned);
  if (blocks.length === 0) return null;
  return (
    <div className="prose prose-stone prose-lg max-w-none">
      {blocks.map((block, idx) =>
        block.type === 'markdown' ? (
          <ReactMarkdown key={idx} remarkPlugins={[remarkGfm]}>
            {block.content}
          </ReactMarkdown>
        ) : (
          <CustomBlock key={idx} type={block.type} content={block.content} meta={block.meta} />
        ),
      )}
    </div>
  );
}
