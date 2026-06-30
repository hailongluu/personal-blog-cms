'use client';

import { useEffect } from 'react';
import DOMPurify from 'isomorphic-dompurify';

// Defense-in-depth: the backend (Jsoup) is the canonical sanitizer, but we run
// admin-authored custom HTML through DOMPurify again before injecting. Scripts
// are allowed back explicitly (tracking snippets need them) but event handlers,
// iframes, and dangerous tags are stripped.
const PURIFY_CONFIG = {
  ADD_TAGS: ['script', 'style', 'meta', 'link', 'noscript'],
  ADD_ATTR: ['src', 'async', 'defer', 'type', 'id', 'nonce', 'crossorigin', 'rel', 'href', 'as', 'name', 'content', 'property', 'charset', 'http-equiv', 'media'],
  FORBID_TAGS: ['iframe', 'object', 'embed', 'form', 'input', 'button', 'frame', 'frameset'],
  FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover', 'onfocus', 'onblur', 'onmouseout'],
  ALLOW_DATA_ATTR: false,
} as const;

function sanitize(html: string): string {
  return DOMPurify.sanitize(html, PURIFY_CONFIG as Record<string, unknown>);
}

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
    if (headHtml) injectHtml(sanitize(headHtml), 'head', 'end');
    if (bodyStart) injectHtml(sanitize(bodyStart), 'body', 'start');
    if (bodyEnd) injectHtml(sanitize(bodyEnd), 'body', 'end');
  }, [headHtml, css, bodyStart, bodyEnd]);

  return null;
}
