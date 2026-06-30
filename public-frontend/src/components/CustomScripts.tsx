'use client';

import { useEffect } from 'react';

// Injects admin-authored custom HTML/CSS from settings (custom.head_scripts,
// custom.css, custom.body_start_scripts, custom.body_end_scripts).
//
// The backend (Jsoup / TrackingScriptSanitizer) is the canonical sanitizer, so
// we inject as-is. Browsers don't execute <script> inserted via innerHTML, so we
// clone each <script> into a fresh element (the GTM/Segment pattern).
// TODO(security): re-add DOMPurify defense-in-depth like the old SPA's lib/tracking.

function cloneScripts(parent: ParentNode): void {
  for (const oldScript of Array.from(parent.querySelectorAll('script'))) {
    const newScript = document.createElement('script');
    for (const attr of Array.from(oldScript.attributes)) newScript.setAttribute(attr.name, attr.value);
    newScript.text = oldScript.textContent ?? '';
    oldScript.parentNode?.replaceChild(newScript, oldScript);
  }
}

function injectHtml(html: string, target: 'head' | 'body', position: 'start' | 'end'): void {
  if (!html.trim()) return;
  const container = document.createElement('div');
  container.innerHTML = html;
  cloneScripts(container);
  const root = target === 'head' ? document.head : document.body;
  while (container.firstChild) {
    if (target === 'body' && position === 'start') root.insertBefore(container.firstChild, root.firstChild);
    else root.appendChild(container.firstChild);
  }
}

type Props = {
  headHtml?: string;
  css?: string;
  bodyStart?: string;
  bodyEnd?: string;
};

export default function CustomScripts({ headHtml, css, bodyStart, bodyEnd }: Props) {
  useEffect(() => {
    if (css && css.trim()) {
      const style = document.createElement('style');
      style.setAttribute('data-source', 'cms-custom-css');
      style.textContent = css.replace(/<\/style\s*>/gi, '');
      document.head.appendChild(style);
    }
    if (headHtml) injectHtml(headHtml, 'head', 'end');
    if (bodyStart) injectHtml(bodyStart, 'body', 'start');
    if (bodyEnd) injectHtml(bodyEnd, 'body', 'end');
  }, [headHtml, css, bodyStart, bodyEnd]);

  return null;
}
