import { Lightbulb, AlertTriangle, BookOpen } from 'lucide-react';

/**
 * Renders MDX-like custom blocks (:::takeaways, :::callout, :::reference)
 * as styled UI components. Falls back to plain markdown rendering for other content.
 */
export function parseCustomBlocks(markdown: string): Array<{ type: 'markdown' | 'takeaways' | 'callout' | 'reference'; content: string; meta?: Record<string, string> }> {
  const blocks: Array<{ type: 'markdown' | 'takeaways' | 'callout' | 'reference'; content: string; meta?: Record<string, string> }> = [];
  const lines = markdown.split('\n');
  let i = 0;
  let markdownBuf = '';

  while (i < lines.length) {
    const line = lines[i];

    // Match :::blockname or :::blockname{key="value"}
    const match = line.match(/^:::(\w+)(?:\{([^}]+)\})?\s*$/);
    if (match) {
      // Flush accumulated markdown
      if (markdownBuf.trim()) {
        blocks.push({ type: 'markdown', content: markdownBuf.trim() });
        markdownBuf = '';
      }

      const blockType = match[1];
      const metaStr = match[2] || '';
      const meta: Record<string, string> = {};
      if (metaStr) {
        metaStr.split(',').forEach(pair => {
          const [k, v] = pair.split('=').map(s => s.trim().replace(/^"|"$/g, ''));
          if (k && v) meta[k] = v;
        });
      }

      // Collect content until closing :::
      let blockContent = '';
      i++;
      while (i < lines.length && !lines[i].match(/^:::\s*$/)) {
        blockContent += (blockContent ? '\n' : '') + lines[i];
        i++;
      }

      blocks.push({ type: blockType as any, content: blockContent, meta });
    } else {
      markdownBuf += (markdownBuf ? '\n' : '') + line;
    }
    i++;
  }

  if (markdownBuf.trim()) {
    blocks.push({ type: 'markdown', content: markdownBuf.trim() });
  }

  return blocks;
}

interface CustomBlockProps {
  type: string;
  content: string;
  meta?: Record<string, string>;
}

export const blockStyles: Record<string, { icon: React.ReactNode; bg: string; border: string; text: string }> = {
  takeaways: {
    icon: <Lightbulb size={18} />,
    bg: 'bg-amber-50',
    border: 'border-amber-200',
    text: 'text-amber-900',
  },
  callout: {
    icon: <AlertTriangle size={18} />,
    bg: 'bg-orange-50',
    border: 'border-orange-200',
    text: 'text-orange-900',
  },
  reference: {
    icon: <BookOpen size={18} />,
    bg: 'bg-blue-50',
    border: 'border-blue-200',
    text: 'text-blue-900',
  },
};

export function CustomBlock({ type, content, meta }: CustomBlockProps) {
  const style = blockStyles[type];
  if (!style) {
    return <div className="p-4 border border-border rounded-lg bg-surface my-4"><pre className="text-sm whitespace-pre-wrap">{content}</pre></div>;
  }

  const title = type === 'takeaways' ? 'Key Takeaways'
    : type === 'callout' ? (meta?.type === 'info' ? 'Info' : meta?.type === 'tip' ? 'Tip' : 'Warning')
    : type === 'reference' ? 'Reference'
    : type;

  return (
    <div className={`p-4 border rounded-lg my-4 ${style.bg} ${style.border}`}>
      <div className={`flex items-center gap-2 font-semibold text-sm mb-2 ${style.text}`}>
        {style.icon}
        <span>{title}</span>
      </div>
      <div className={`text-sm ${style.text} opacity-80 whitespace-pre-wrap`}>
        {content}
      </div>
    </div>
  );
}
