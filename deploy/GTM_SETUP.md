# Analytics via Google Tag Manager (GTM-only)

The blog embeds **one** Google Tag Manager container. Every other channel
(GA4, Microsoft Clarity, Facebook Pixel, TikTok, Google Ads, …) is configured as
a **tag inside GTM** — not in the CMS.

## In the CMS (one-time)
Admin → **Settings → Google Tag Manager** → paste your **GTM Container ID**
(`GTM-XXXXXXX`) → Save. That's the only analytics field.

The public site then renders the GTM snippet (`<head>` loader + `<noscript>` body
iframe). It does **not** inject GA4/Clarity/FB directly — GTM does.

## In GTM (manage all channels here)
At <https://tagmanager.google.com>, in your container, add tags (Trigger = **All Pages**):

| Channel | How |
|---|---|
| **GA4** | Tag type *Google Analytics: GA4 Configuration*, paste Measurement ID `G-XXXXXXXX` |
| **Microsoft Clarity** | Community template *Microsoft Clarity* (or Custom HTML with the Clarity snippet) |
| **Facebook Pixel** | Community template *Facebook Pixel* (or Custom HTML) |
| **TikTok / Google Ads / …** | their templates, same pattern |

Use GTM **Preview** to verify, then **Submit/Publish**.

## Why GTM-only
- Add/remove/edit channels with **no code or CMS change** — just publish in GTM.
- Central **Consent Mode v2** + tag sequencing/triggers.
- One third-party loader instead of many.

## Notes for this stack
- **CSP is GTM-friendly**: `script-src`/`connect-src`/`frame-src` allow any HTTPS
  origin, so a new tag added in GTM works without touching nginx
  (`deploy/nginx/conf.d/blog.conf`).
- **Kill-switch**: Settings → *GDPR Consent Mode = None* loads tracking by default;
  any other value currently gates **all** tracking off until you wire a consent
  banner. Leave it at the default unless you add a banner.
- Don't also fill GA4/FB IDs anywhere — they're managed in GTM (avoids double-firing).
