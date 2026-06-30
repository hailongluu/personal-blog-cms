import type { Metadata } from 'next';
import './globals.css';
import SiteHeader from '@/components/SiteHeader';
import SiteFooter from '@/components/SiteFooter';
import { TrackingScripts, TrackingNoscript } from '@/components/TrackingScripts';
import CustomScripts from '@/components/CustomScripts';
import { getPublicSettings } from '@/lib/api';
import {
  SITE_URL,
  SITE_NAME,
  SITE_TAGLINE,
  SITE_DESCRIPTION,
  AUTHOR_NAME,
  TWITTER_HANDLE,
  LOCALE,
  ogImageUrl,
} from '@/lib/site';

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: `${SITE_NAME} · ${SITE_TAGLINE}`,
    template: `%s — ${SITE_NAME}`,
  },
  description: SITE_DESCRIPTION,
  applicationName: SITE_NAME,
  authors: [{ name: AUTHOR_NAME, url: `${SITE_URL}/about` }],
  keywords: ['Lưu Hải Long', 'AI', 'công nghệ', 'lập trình', 'blog', 'LLM', 'AI agent', 'machine learning'],
  alternates: {
    canonical: '/',
    types: { 'application/rss+xml': `${SITE_URL}/rss.xml` },
  },
  robots: {
    index: true,
    follow: true,
    googleBot: { index: true, follow: true, 'max-image-preview': 'large', 'max-snippet': -1 },
  },
  openGraph: {
    type: 'website',
    siteName: SITE_NAME,
    title: `${SITE_NAME} · ${SITE_TAGLINE}`,
    description: SITE_DESCRIPTION,
    url: SITE_URL,
    locale: LOCALE,
    images: [{ url: ogImageUrl({ title: SITE_NAME, subtitle: SITE_TAGLINE }), width: 1200, height: 630, alt: SITE_NAME }],
  },
  twitter: {
    card: 'summary_large_image',
    title: `${SITE_NAME} · ${SITE_TAGLINE}`,
    description: SITE_DESCRIPTION,
    site: TWITTER_HANDLE,
    creator: TWITTER_HANDLE,
    images: [ogImageUrl({ title: SITE_NAME, subtitle: SITE_TAGLINE })],
  },
  icons: { icon: '/favicon.svg' },
};

const siteJsonLd = {
  '@context': 'https://schema.org',
  '@type': 'Blog',
  name: SITE_NAME,
  alternateName: 'Long Luu Personal Blog',
  url: `${SITE_URL}/`,
  description: 'Chia sẻ kiến thức chuyên sâu về AI, công nghệ và lập trình',
  inLanguage: 'vi-VN',
  author: {
    '@type': 'Person',
    name: AUTHOR_NAME,
    url: `${SITE_URL}/about`,
    sameAs: ['https://github.com/hailongluu', 'https://t.me/hailongluu'],
  },
  publisher: { '@type': 'Person', name: AUTHOR_NAME },
  potentialAction: {
    '@type': 'SearchAction',
    target: `${SITE_URL}/blog?q={search_term_string}`,
    'query-input': 'required name=search_term_string',
  },
};

function str(v: string | boolean | undefined): string | undefined {
  return typeof v === 'string' && v.trim() ? v : undefined;
}

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const settings = await getPublicSettings();

  return (
    <html lang="vi">
      <body className="min-h-screen flex flex-col">
        {/* Set theme class before paint to avoid flash. */}
        <script
          dangerouslySetInnerHTML={{
            __html:
              "try{var t=localStorage.getItem('theme');if(t==='dark'||(!t&&window.matchMedia('(prefers-color-scheme:dark)').matches)){document.documentElement.classList.add('dark')}}catch(e){}",
          }}
        />
        {/* JSON-LD + provider scripts. React 19 / next-script hoist these to <head>;
            Google reads JSON-LD anywhere in the document. */}
        <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(siteJsonLd) }} />
        <TrackingScripts settings={settings} />
        <TrackingNoscript settings={settings} />
        <SiteHeader />
        <main className="flex-1">{children}</main>
        <SiteFooter />
        <CustomScripts
          headHtml={str(settings['custom.head_scripts'])}
          css={str(settings['custom.css'])}
          bodyStart={str(settings['custom.body_start_scripts'])}
          bodyEnd={str(settings['custom.body_end_scripts'])}
        />
      </body>
    </html>
  );
}
