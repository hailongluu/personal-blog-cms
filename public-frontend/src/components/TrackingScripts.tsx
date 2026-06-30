import Script from 'next/script';
import type { Settings } from '@/types';

// GTM-only: the blog embeds a single Google Tag Manager container. GA4, Microsoft
// Clarity, Facebook Pixel, etc. are configured as tags INSIDE GTM — the CMS only
// stores the GTM Container ID. (consent_mode='none' is a site-wide kill-switch.)

function str(v: string | boolean | undefined): string {
  return typeof v === 'string' ? v : '';
}

function trackingDisabled(s: Settings): boolean {
  return str(s['tracking.consent_mode']).toLowerCase() === 'none';
}

export function TrackingScripts({ settings: s }: { settings: Settings }) {
  if (trackingDisabled(s)) return null;
  const gtm = str(s['tracking.gtm_container_id']);
  if (!gtm) return null;

  return (
    <Script id="gtm" strategy="afterInteractive">
      {`(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src='https://www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);})(window,document,'script','dataLayer','${gtm}');`}
    </Script>
  );
}

/** GTM <noscript> fallback — placed at the top of <body>. */
export function TrackingNoscript({ settings: s }: { settings: Settings }) {
  if (trackingDisabled(s)) return null;
  const gtm = str(s['tracking.gtm_container_id']);
  if (!gtm) return null;

  return (
    <noscript>
      <iframe
        src={`https://www.googletagmanager.com/ns.html?id=${gtm}`}
        height="0"
        width="0"
        style={{ display: 'none', visibility: 'hidden' }}
      />
    </noscript>
  );
}
