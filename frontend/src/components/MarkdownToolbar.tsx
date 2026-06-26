import {
  Bold, Italic, Link, Quote, Code, Image, Table, Minus,
  Heading1, Heading2, Heading3, List, ListOrdered,
  Lightbulb, AlertTriangle, BookOpen
} from 'lucide-react';

interface Props {
  textareaRef: React.RefObject<HTMLTextAreaElement | null>;
  value: string;
  onChange: (value: string) => void;
}

export default function MarkdownToolbar({ textareaRef, value, onChange }: Props) {
  const textarea = textareaRef.current;

  function insert(before: string, after?: string, placeholder?: string) {
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selected = value.substring(start, end);
    const text = selected || (placeholder || '');
    const newValue = value.substring(0, start) + before + text + (after || '') + value.substring(end);
    onChange(newValue);
    // Restore cursor position after state update
    requestAnimationFrame(() => {
      if (textarea) {
        textarea.focus();
        textarea.setSelectionRange(start + before.length, start + before.length + text.length);
      }
    });
  }

  function wrapBlock(before: string, after: string, placeholder?: string) {
    insert('\n' + before + '\n', '\n' + after + '\n', placeholder);
  }

  const btn = "p-1.5 rounded hover:bg-bg transition-colors text-text-muted hover:text-text";
  const sep = <div className="w-px h-6 bg-border" />;

  return (
    <div className="flex items-center gap-0.5 flex-wrap p-1.5 border border-border rounded-t-lg bg-surface">
      {/* Headings */}
      <button type="button" onClick={() => insert('# ', '', 'Heading 1')} className={btn} title="H1">
        <Heading1 size={16} />
      </button>
      <button type="button" onClick={() => insert('## ', '', 'Heading 2')} className={btn} title="H2">
        <Heading2 size={16} />
      </button>
      <button type="button" onClick={() => insert('### ', '', 'Heading 3')} className={btn} title="H3">
        <Heading3 size={16} />
      </button>

      {sep}

      {/* Formatting */}
      <button type="button" onClick={() => insert('**', '**', 'bold text')} className={btn} title="Bold">
        <Bold size={16} />
      </button>
      <button type="button" onClick={() => insert('*', '*', 'italic text')} className={btn} title="Italic">
        <Italic size={16} />
      </button>
      <button type="button" onClick={() => insert('`', '`', 'code')} className={btn} title="Inline Code">
        <Code size={16} />
      </button>
      <button type="button" onClick={() => insert('[', '](url)', 'link text')} className={btn} title="Link">
        <Link size={16} />
      </button>

      {sep}

      {/* Blocks */}
      <button type="button" onClick={() => insert('\n> ', '', 'quote')} className={btn} title="Blockquote">
        <Quote size={16} />
      </button>
      <button type="button" onClick={() => wrapBlock('```', '```', 'code block')} className={btn} title="Code Block">
        <Code size={16} />
      </button>
      <button type="button" onClick={() => insert('\n---\n')} className={btn} title="Divider">
        <Minus size={16} />
      </button>
      <button type="button" onClick={() => insert('\n| Col 1 | Col 2 | Col 3 |\n| --- | --- | --- |\n| ', ' | ', 'content')} className={btn} title="Table">
        <Table size={16} />
      </button>
      <button type="button" onClick={() => insert('![', '](url)', 'alt text')} className={btn} title="Image">
        <Image size={16} />
      </button>
      <button type="button" onClick={() => insert('- ', '', 'list item')} className={btn} title="Bullet List">
        <List size={16} />
      </button>
      <button type="button" onClick={() => insert('1. ', '', 'list item')} className={btn} title="Numbered List">
        <ListOrdered size={16} />
      </button>

      {sep}

      {/* Custom Blocks */}
      <button type="button" onClick={() => wrapBlock(':::takeaways', ':::', '- Ý chính 1\n- Ý chính 2')} className={btn + ' text-amber-600'} title="Takeaways">
        <Lightbulb size={16} />
      </button>
      <button type="button" onClick={() => wrapBlock(':::callout{type="warning"}', ':::', 'Nội dung cảnh báo.')} className={btn + ' text-orange-600'} title="Callout">
        <AlertTriangle size={16} />
      </button>
      <button type="button" onClick={() => wrapBlock(':::reference', ':::', 'Title: ...\nURL: ...')} className={btn + ' text-blue-600'} title="Reference">
        <BookOpen size={16} />
      </button>
    </div>
  );
}
