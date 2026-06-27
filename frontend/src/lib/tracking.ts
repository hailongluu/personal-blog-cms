import type { Settings } from '@/types';
import { getPublicSettings } from './publicApi';

// ───────────────────────────────────────────────────────────
// Client-side tracking scripts injection
//
// ARCHITECTURE:
//   Backend (SettingsService + TrackingScriptSanitizer + Jsoup) is the
//   source of truth for what is safe. It strips event handlers, forbids
//   iframe/object/embed, validates each ID against its regex, and only
//   allows src URLs from googletagmanager.com / connect.facebook.net /
//   analytics.tiktok.com. By the time a script snippet reaches this
//   client, it has been validated TWICE (admin form → sanitize → DB).
//
//   We do NOT re-sanitize server-generated tracking snippets with
//   DOMPurify, because:
//     1. DOMPurify aggressively strips <script> tags from sanitized
//        output unless WHOLE_DOCUMENT or FORCE_BODY is set
//        (those options break normal head/body placement)
//     2. The backend Jsoup pass is the canonical sanitizer
//     3. Google/Facebook/TikTok snippets have a known shape that
//        is safe by construction
//
//   We DO sanitize user-supplied custom.* HTML with DOMPurify, since
//   that content can come from any admin and we want defense-in-depth.
//
// HTML5 SPEC PROBLEM:
//   Browsers refuse to execute <script> elements inserted via
//   innerHTML or DocumentFragment on already-mounted elements.
//   The standard fix is to walk the sanitized output, find every
//   <script>, create a NEW script element via createElement, copy
//   attributes and text, and replace the inert node. This is the
//   pattern Segment, GTM, and Plausible all use.
// ───────────────────────────────────────────────────────────

let dompurifyPromise: Promise<typeof import('dompurify').default> | null = null;

async function getDOMPurify(): Promise<typeof import('dompurify').default> {
  if (!dompurifyPromise) {
    dompurifyPromise = import('dompurify').then(m => m.default);
  }
  return dompurifyPromise;
}

function getCustomHtmlPurifyConfig() {
  // Used only for user-supplied custom.* HTML. Server has already
  // stripped event handlers and forbidden tags, but we run DOMPurify
  // anyway as defense-in-depth.
  return {
    ALLOWED_TAGS: ['script', 'style', 'meta', 'link', 'noscript'],
    ALLOWED_ATTR: [
      'src', 'async', 'defer', 'type', 'id', 'nonce', 'crossorigin',
      'rel', 'href', 'as',
      'name', 'content', 'property', 'charset', 'http-equiv',
      'media',
    ],
    ALLOW_DATA_ATTR: false,
    FORBID_TAGS: ['iframe', 'object', 'embed', 'form', 'input', 'button', 'frame', 'frameset'],
    FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover', 'onfocus', 'onblur', 'onmouseout'],
  };
}

/**
 * Walk a fragment's <script> children and replace each with a fresh,
 * live script element. This is the workaround for the HTML5 spec
 * "scripts inserted via innerHTML don't execute" behavior.
 */
function cloneScripts(parent: ParentNode): void {
  const scripts = Array.from(parent.querySelectorAll('script'));
  for (const oldScript of scripts) {
    const newScript = document.createElement('script');
    for (const attr of Array.from(oldScript.attributes)) {
      newScript.setAttribute(attr.name, attr.value);
    }
    newScript.text = oldScript.textContent;
    oldScript.parentNode?.replaceChild(newScript, oldScript);
  }
}

/**
 * Inject server-validated tracking HTML (GA4/GTM/FB/TikTok snippets).
 * Skips DOMPurify because the backend is the source of truth, and
 * DOMPurify would strip <script> elements.
 */
function injectServerValidated(html: string, target: 'head' | 'body', position: 'start' | 'end' = 'end'): void {
  if (!html.trim()) return;
  const container = document.createElement('div');
  container.innerHTML = html;
  cloneScripts(container);

  const root = target === 'head' ? document.head : document.body;
  while (container.firstChild) {
    if (target === 'body' && position === 'start') {
      root.insertBefore(container.firstChild, root.firstChild);
    } else {
      root.appendChild(container.firstChild);
    }
  }
}

/**
 * Inject user-supplied custom HTML through DOMPurify (defense-in-depth)
 * and the script-cloning workaround.
 */
async function injectUserHtml(html: string, target: 'head' | 'body', position: 'start' | 'end' = 'end'): Promise<void> {
  if (!html.trim()) return;
  try {
    const DOMPurify = await getDOMPurify();
    const sanitized = DOMPurify.sanitize(html, getCustomHtmlPurifyConfig());

    const container = document.createElement('div');
    container.innerHTML = sanitized;
    cloneScripts(container);

    const root = target === 'head' ? document.head : document.body;
    while (container.firstChild) {
      if (target === 'body' && position === 'start') {
        root.insertBefore(container.firstChild, root.firstChild);
      } else {
        root.appendChild(container.firstChild);
      }
    }
  } catch (e) {
    console.warn('Tracking: failed to inject custom HTML', e);
  }
}

function injectCss(css: string): void {
  if (!css.trim()) return;
  const safe = css.replace(/<\/style\s*>/gi, '');
  const style = document.createElement('style');
  style.setAttribute('data-source', 'cms-custom-css');
  style.textContent = safe;
  document.head.appendChild(style);
}

/**
 * Build the canonical tracking snippet block for the four supported
 * providers. The backend has already validated each ID; we just
 * template them here.
 */
function buildTrackingScripts(s: Settings): string {
  const parts: string[] = [];

  // ── GA4 (gtag) ─────────────────────────────────────────
  const ga4 = s['tracking.ga4_measurement_id'];
  if (ga4 && s['tracking.gtag_enabled'] !== false) {
    parts.push(
      '<!-- Google Analytics 4 -->',
      `<script async src="https://www.googletagmanager.com/gtag/js?id=${ga4}"></script>`,
      `<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());
  gtag('config', '${ga4}', { anonymize_ip: true });
</script>`,
      '<!-- End GA4 -->',
    );
  }

  // ── Google Tag Manager ─────────────────────────────────
  const gtm = s['tracking.gtm_container_id'];
  if (gtm) {
    parts.push(
      '<!-- Google Tag Manager -->',
      `<script>(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':
new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],
j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=
'https://www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);
})(window,document,'script','dataLayer','${gtm}');</script>`,
      '<!-- End GTM -->',
    );
  }

  // ── Facebook Pixel ──────────────────────────────────────
  const fb = s['tracking.fb_pixel_id'];
  if (fb && s['tracking.fb_enabled'] !== false) {
    parts.push(
      '<!-- Facebook Pixel -->',
      `<script>
  !function(f,b,e,v,n,t,s)
  {if(f.fbq)return;n=f.fbq=function(){n.callMethod?
  n.callMethod.apply(n,arguments):n.queue.push(arguments)};
  if(!f._fbq)f._fbq=n;n.push=n;n.loaded=!0;n.version='2.0';
  n.queue=[];t=b.createElement(e);t.async=!0;
  t.src=v;s=b.getElementsByTagName(e)[0];
  s.parentNode.insertBefore(t,s)}(window, document,'script',
  'https://connect.facebook.net/en_US/fbevents.js');
  fbq('init', '${fb}');
  fbq('track', 'PageView');
</script>`,
      '<!-- End Facebook Pixel -->',
    );
  }

  // ── TikTok Pixel ────────────────────────────────────────
  const tt = s['tracking.tiktok_pixel_id'];
  if (tt && s['tracking.tiktok_enabled'] !== false) {
    parts.push(
      '<!-- TikTok Pixel -->',
      `<script>
  !function (w, d, t) {
    w.TiktokAnalyticsObject=t;var ttq=w[t]=w[t]||[];ttq.methods=["page","track","identify","instances","debug","on","off","once","ready","alias","group","enableCookie","disableCookie"],ttq.setAndDefer=function(t,e){t[e]=t[e]||function(){t[e].push([e+"-called",arguments])}};for(var i=0;i<ttq.methods.length;i++)ttq.setAndDefer(ttq,ttq.methods[i]);ttq.instance=function(t){for(var e=ttq._i[t]||[],n=0;n<ttq.methods.length;n++)ttq.setAndDefer(e,ttq.methods[n]);return e},ttq.load=function(e,n){var i="https://analytics.tiktok.com/i18n/pixel/events.js";ttq._i=ttq._i||{},ttq._i[e]=[],ttq._i[e]._u=i,ttq._t=ttq._t||{},ttq._t[e]=+new Date,ttq._o=ttq._o||{},ttq._o[e]=n||{};var o=d.createElement("script");o.type="text/javascript",o.async=!0,o.src=i+"?sdkid="+e+"&lib="+t;var a=d.getElementsByTagName("script")[0];a.parentNode.insertBefore(o,a)};
    ttq.load('${tt}');
    ttq.page();
  }(window, document, 'ttq');
</script>`,
      '<!-- End TikTok Pixel -->',
    );
  }

  return parts.join('\n');
}

/**
 * Main entry point. Called by PublicLayout on mount.
 *
 * Inject order:
 *   1. Server-validated tracking scripts (GA4/GTM/FB/TikTok)
 *   2. User-supplied custom HTML (DOMPurify pass)
 *   3. Custom CSS as <style>
 *   4. Body start/end scripts (next tick)
 */
export async function injectTrackingScripts(settings: Settings): Promise<void> {
  // 1. Auto-built tracking snippets — server-validated, no DOMPurify
  const trackingScripts = buildTrackingScripts(settings);
  if (trackingScripts.trim()) {
    injectServerValidated(trackingScripts, 'head');
  }

  // 2. Custom head HTML — DOMPurify pass (defense-in-depth)
  if (settings['custom.head_scripts']) {
    await injectUserHtml(settings['custom.head_scripts'], 'head', 'end');
  }

  // 3. Custom CSS as <style>
  if (settings['custom.css']) {
    injectCss(settings['custom.css']);
  }

  // 4. Body scripts (next tick — body must exist)
  setTimeout(async () => {
    if (settings['custom.body_start_scripts']) {
      await injectUserHtml(settings['custom.body_start_scripts'], 'body', 'start');
    }
    if (settings['custom.body_end_scripts']) {
      await injectUserHtml(settings['custom.body_end_scripts'], 'body', 'end');
    }
  }, 0);
}

/**
 * Convenience: fetch public settings + inject.
 * Fails silently so the site still renders without tracking.
 */
export async function loadAndInjectTracking(): Promise<void> {
  try {
    const settings = await getPublicSettings();
    await injectTrackingScripts(settings);
  } catch (e) {
    console.warn('Tracking: settings fetch failed', e);
  }
}
