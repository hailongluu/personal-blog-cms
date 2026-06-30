import type { Metadata } from 'next';
import { AUTHOR_NAME, SITE_URL } from '@/lib/site';

export const metadata: Metadata = {
  title: 'About',
  description: `Giới thiệu về ${AUTHOR_NAME} — nền tảng, mối quan tâm và blog này nói về điều gì.`,
  alternates: { canonical: '/about' },
};

const jsonLd = {
  '@context': 'https://schema.org',
  '@type': 'ProfilePage',
  mainEntity: {
    '@type': 'Person',
    name: AUTHOR_NAME,
    url: `${SITE_URL}/about`,
    sameAs: ['https://github.com/hailongluu', 'https://t.me/hailongluu'],
  },
};

export default function AboutPage() {
  return (
    <div className="max-w-3xl mx-auto px-4 py-12 md:py-16">
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />

      <div className="mb-12 text-center md:text-left">
        <div className="w-24 h-24 rounded-full bg-stone-200 mx-auto md:mx-0 mb-6 flex items-center justify-center text-4xl">👋</div>
        <h1 className="text-3xl md:text-4xl font-bold text-stone-900 mb-4">Xin chào, tôi là {AUTHOR_NAME}</h1>
        <p className="text-lg text-stone-500 max-w-xl">
          Một lập trình viên đam mê xây dựng sản phẩm cho web, AI và công nghệ.
        </p>
      </div>

      <div className="prose prose-stone prose-lg max-w-none">
        <h2>Tôi làm gì</h2>
        <p>
          Tôi tập trung vào phát triển web hiện đại với TypeScript, React và Java/Spring. Tôi quan tâm
          đến AI, developer tooling, design systems và xây dựng những sản phẩm giúp cuộc sống dễ dàng hơn.
        </p>

        <h2>Blog này</h2>
        <p>
          Đây là góc nhỏ của tôi trên internet, nơi tôi chia sẻ những gì mình học được, các dự án đang
          xây dựng, và suy nghĩ về công nghệ. Tôi viết về AI, phát triển phần mềm, năng suất và đôi khi
          là những chủ đề khác khiến tôi tò mò.
        </p>

        <h2>Liên hệ</h2>
        <p>
          Tôi luôn sẵn lòng kết nối với các lập trình viên và những người thú vị. Hãy liên hệ qua{' '}
          <a href="https://github.com/hailongluu" className="text-stone-900 underline">GitHub</a> hoặc{' '}
          <a href="https://t.me/hailongluu" className="text-stone-900 underline">Telegram</a>.
        </p>
      </div>
    </div>
  );
}
