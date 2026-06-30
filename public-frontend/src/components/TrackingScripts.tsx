import Script from 'next/script';
import type { Settings } from '@/types';

// Server-driven tracking. The backend (SettingsService + TrackingScriptSanitizer
// + Jsoup) is the source of truth: it validates each ID and only permits known
// provider origins. Here we just template the canonical snippets and let
// next/script load them after hydration (afterInteractive). Bots ignore these —
// they don't affect SEO/meta, which is already in the server-rendered HTML.

function str(v: string | boolean | undefined): string {
  return typeof v === 'string' ? v : '';
}

export function TrackingScripts({ settings: s }: { settings: Settings }) {
  const ga4 = str(s['tracking.ga4_measurement_id']);
  const gtm = str(s['tracking.gtm_container_id']);
  const fb = str(s['tracking.fb_pixel_id']);
  const tt = str(s['tracking.tiktok_pixel_id']);
  const gtagOn = s['tracking.gtag_enabled'] !== false;
  const fbOn = s['tracking.fb_enabled'] !== false;
  const ttOn = s['tracking.tiktok_enabled'] !== false;

  return (
    <>
      {ga4 && gtagOn && (
        <>
          <Script src={`https://www.googletagmanager.com/gtag/js?id=${ga4}`} strategy="afterInteractive" />
          <Script id="ga4-init" strategy="afterInteractive">
            {`window.dataLayer=window.dataLayer||[];function gtag(){dataLayer.push(arguments);}gtag('js',new Date());gtag('config','${ga4}',{anonymize_ip:true});`}
          </Script>
        </>
      )}

      {gtm && (
        <Script id="gtm" strategy="afterInteractive">
          {`(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src='https://www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);})(window,document,'script','dataLayer','${gtm}');`}
        </Script>
      )}

      {fb && fbOn && (
        <Script id="fb-pixel" strategy="afterInteractive">
          {`!function(f,b,e,v,n,t,s){if(f.fbq)return;n=f.fbq=function(){n.callMethod?n.callMethod.apply(n,arguments):n.queue.push(arguments)};if(!f._fbq)f._fbq=n;n.push=n;n.loaded=!0;n.version='2.0';n.queue=[];t=b.createElement(e);t.async=!0;t.src=v;s=b.getElementsByTagName(e)[0];s.parentNode.insertBefore(t,s)}(window,document,'script','https://connect.facebook.net/en_US/fbevents.js');fbq('init','${fb}');fbq('track','PageView');`}
        </Script>
      )}

      {tt && ttOn && (
        <Script id="tiktok-pixel" strategy="afterInteractive">
          {`!function(w,d,t){w.TiktokAnalyticsObject=t;var ttq=w[t]=w[t]||[];ttq.methods=["page","track","identify","instances","debug","on","off","once","ready","alias","group","enableCookie","disableCookie"],ttq.setAndDefer=function(t,e){t[e]=t[e]||function(){t[e].push([e+"-called",arguments])}};for(var i=0;i<ttq.methods.length;i++)ttq.setAndDefer(ttq,ttq.methods[i]);ttq.instance=function(t){for(var e=ttq._i[t]||[],n=0;n<ttq.methods.length;n++)ttq.setAndDefer(e,ttq.methods[n]);return e},ttq.load=function(e,n){var i="https://analytics.tiktok.com/i18n/pixel/events.js";ttq._i=ttq._i||{},ttq._i[e]=[],ttq._i[e]._u=i,ttq._t=ttq._t||{},ttq._t[e]=+new Date,ttq._o=ttq._o||{},ttq._o[e]=n||{};var o=d.createElement("script");o.type="text/javascript",o.async=!0,o.src=i+"?sdkid="+e+"&lib="+t;var a=d.getElementsByTagName("script")[0];a.parentNode.insertBefore(o,a)};ttq.load('${tt}');ttq.page();}(window,document,'ttq');`}
        </Script>
      )}
    </>
  );
}

/** GTM + FB <noscript> fallbacks — placed at the top of <body>. */
export function TrackingNoscript({ settings: s }: { settings: Settings }) {
  const gtm = str(s['tracking.gtm_container_id']);
  const fb = str(s['tracking.fb_pixel_id']);
  const fbOn = s['tracking.fb_enabled'] !== false;
  return (
    <>
      {gtm && (
        <noscript>
          <iframe
            src={`https://www.googletagmanager.com/ns.html?id=${gtm}`}
            height="0"
            width="0"
            style={{ display: 'none', visibility: 'hidden' }}
          />
        </noscript>
      )}
      {fb && fbOn && (
        <noscript>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img height="1" width="1" style={{ display: 'none' }} alt="" src={`https://www.facebook.com/tr?id=${fb}&ev=PageView&noscript=1`} />
        </noscript>
      )}
    </>
  );
}
