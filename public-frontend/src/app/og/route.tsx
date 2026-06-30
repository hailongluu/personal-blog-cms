import { ImageResponse } from 'next/og';
import { readFile } from 'node:fs/promises';
import { join } from 'node:path';

export const runtime = 'nodejs';

// Dynamic Open Graph image (PNG 1200×630) — replaces the old static SVG, which
// Facebook/X/LinkedIn/Telegram do not render. Called as /og?title=...&subtitle=...
// Uses Be Vietnam Pro so Vietnamese diacritics render correctly (the default
// next/og font lacks full Vietnamese coverage). Fonts live in public/fonts and
// are bundled into the standalone image (Dockerfile copies ./public).

let fontsCache: { name: string; data: Buffer; weight: 400 | 600; style: 'normal' }[] | null = null;

async function loadFonts() {
  if (fontsCache) return fontsCache;
  const dir = join(process.cwd(), 'public', 'fonts');
  const [regular, semibold] = await Promise.all([
    readFile(join(dir, 'BeVietnamPro-Regular.ttf')),
    readFile(join(dir, 'BeVietnamPro-SemiBold.ttf')),
  ]);
  fontsCache = [
    { name: 'Be Vietnam Pro', data: regular, weight: 400, style: 'normal' },
    { name: 'Be Vietnam Pro', data: semibold, weight: 600, style: 'normal' },
  ];
  return fontsCache;
}

export async function GET(req: Request) {
  const { searchParams } = new URL(req.url);
  const title = (searchParams.get('title') ?? 'Lưu Hải Long — Personal Blog').slice(0, 120);
  const subtitle = (searchParams.get('subtitle') ?? '').slice(0, 80);
  const author = (searchParams.get('author') ?? '').slice(0, 60);

  const fonts = await loadFonts();

  return new ImageResponse(
    (
      <div
        style={{
          width: '1200px',
          height: '630px',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          background: 'linear-gradient(135deg, #faf6ee 0%, #f0e9dc 100%)',
          fontFamily: 'Be Vietnam Pro',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', height: '100px', background: '#2d2d3a', padding: '0 60px' }}>
          {/* Plain text only — glyphs outside Be Vietnam Pro (e.g. ✦) make satori
              fetch a fallback font over the network, which fails offline. */}
          <div style={{ color: '#f5f0ea', fontSize: 32, fontWeight: 600 }}>Lưu Hải Long</div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', padding: '0 70px', flex: 1, justifyContent: 'center' }}>
          <div style={{ fontSize: 60, fontWeight: 600, color: '#2d2d3a', lineHeight: 1.15 }}>{title}</div>
          {subtitle && <div style={{ marginTop: 24, fontSize: 30, fontWeight: 400, color: '#57534e' }}>{subtitle}</div>}
          {author && <div style={{ marginTop: 16, fontSize: 24, fontWeight: 400, color: '#78716c' }}>{`— ${author}`}</div>}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', height: '60px', background: '#2d2d3a', padding: '0 60px' }}>
          <div style={{ color: '#f5f0ea', fontSize: 20, fontWeight: 400 }}>news.luuhailong.com</div>
        </div>
      </div>
    ),
    {
      width: 1200,
      height: 630,
      fonts: fonts.map((f) => ({ name: f.name, data: f.data, weight: f.weight, style: f.style })),
    },
  );
}
