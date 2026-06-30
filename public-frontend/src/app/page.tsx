import type { Metadata } from 'next';
import Link from 'next/link';
import { ArrowRight, FileText, Calendar } from 'lucide-react';
import { getPublishedPosts } from '@/lib/api';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: { absolute: 'Lưu Hải Long — Góc nhìn thực chiến về AI, công nghệ và sản phẩm số' },
  description:
    'Blog cá nhân của Lưu Hải Long. Chia sẻ góc nhìn thực chiến về AI, dữ liệu lớn, hệ thống và sản phẩm số — phân tích, hướng dẫn và đánh giá công cụ mới nhất.',
  alternates: { canonical: '/' },
};

function formatDate(iso: string | null): string {
  if (!iso) return '';
  return new Date(iso).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

export default async function HomePage() {
  const { data: posts } = await getPublishedPosts(1, 6);

  return (
    <>
      {/* Hero */}
      <section className="bg-white dark:bg-stone-900 border-b border-stone-200 dark:border-stone-800">
        <div className="max-w-5xl mx-auto px-4 py-16 md:py-24 text-center">
          <p className="text-xs sm:text-sm font-medium tracking-widest uppercase text-stone-500 dark:text-stone-400 mb-4">
            Lưu Hải Long
          </p>
          <h1 className="text-3xl sm:text-4xl md:text-5xl lg:text-6xl font-bold tracking-tight text-stone-900 dark:text-stone-100 mb-6 leading-tight">
            Góc nhìn thực chiến về{' '}
            <span className="bg-gradient-to-r from-amber-600 via-orange-600 to-rose-600 bg-clip-text text-transparent">
              AI, công nghệ
            </span>
            <br />
            <span className="text-stone-700 dark:text-stone-300">và sản phẩm số</span>
          </h1>
          <p className="text-base sm:text-lg text-stone-600 dark:text-stone-300 max-w-2xl mx-auto mb-3 px-4 leading-relaxed">
            Tôi viết về cách ứng dụng <strong className="text-stone-800 dark:text-stone-100">AI</strong>,{' '}
            <strong className="text-stone-800 dark:text-stone-100">dữ liệu lớn</strong> và{' '}
            <strong className="text-stone-800 dark:text-stone-100">công nghệ</strong> để giải quyết bài toán thật trong doanh nghiệp.
          </p>
          <p className="text-sm text-stone-500 dark:text-stone-400 max-w-xl mx-auto mb-8 px-4">
            Phân tích chuyên sâu · Hướng dẫn thực hành · Đánh giá công cụ mới nhất
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 sm:gap-4">
            <Link
              href="/blog"
              className="inline-flex items-center gap-2 px-6 py-3 bg-stone-900 dark:bg-stone-100 text-white dark:text-stone-900 rounded-lg font-medium hover:bg-stone-800 dark:hover:bg-white transition-colors w-full sm:w-auto justify-center shadow-sm"
            >
              Đọc bài mới nhất <ArrowRight size={18} />
            </Link>
            <Link
              href="/topics"
              className="inline-flex items-center gap-2 px-6 py-3 border border-stone-300 dark:border-stone-700 text-stone-700 dark:text-stone-200 rounded-lg font-medium hover:bg-stone-100 dark:hover:bg-stone-800 transition-colors w-full sm:w-auto justify-center"
            >
              Khám phá chủ đề
            </Link>
          </div>
        </div>
      </section>

      {/* Latest posts */}
      <section className="max-w-6xl mx-auto px-4 py-12 md:py-20">
        <div className="flex items-center justify-between mb-8 md:mb-10">
          <div>
            <h2 className="text-xl sm:text-2xl font-bold text-stone-900 dark:text-stone-100">Bài viết mới nhất</h2>
            <p className="text-sm text-stone-500 dark:text-stone-400 mt-1">Cập nhật liên tục · {new Date().getFullYear()}</p>
          </div>
          <Link href="/blog" className="text-sm font-medium text-stone-600 dark:text-stone-300 hover:text-stone-900 dark:hover:text-white transition-colors whitespace-nowrap">
            Xem tất cả →
          </Link>
        </div>

        {posts.length > 0 ? (
          <div className="grid gap-4 sm:gap-6 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
            {posts.map((post) => (
              <Link
                key={post.id}
                href={`/blog/${post.slug}`}
                className="group bg-white dark:bg-stone-900 rounded-xl border border-stone-200 dark:border-stone-800 p-5 hover:border-stone-400 dark:hover:border-stone-600 hover:shadow-lg transition-all duration-200 flex flex-col"
              >
                {post.coverImageUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={post.coverImageUrl} alt={post.title} className="w-full h-36 sm:h-40 object-cover rounded-lg mb-4" loading="lazy" />
                ) : (
                  <div className="w-full h-36 sm:h-40 bg-gradient-to-br from-stone-100 to-stone-200 dark:from-stone-800 dark:to-stone-700 rounded-lg mb-4 flex items-center justify-center">
                    <FileText size={32} className="text-stone-300 dark:text-stone-600" />
                  </div>
                )}

                {post.topic && (
                  <span
                    className="inline-block text-xs font-medium px-2 py-0.5 rounded-full mb-2 self-start"
                    style={{ backgroundColor: (post.topic.color || '#78716c') + '20', color: post.topic.color || '#78716c' }}
                  >
                    {post.topic.name}
                  </span>
                )}

                <h3 className="font-semibold text-stone-900 dark:text-stone-100 group-hover:text-stone-600 dark:group-hover:text-stone-300 transition-colors mb-2 line-clamp-2 leading-snug">
                  {post.title}
                </h3>

                {post.excerpt && <p className="text-sm text-stone-500 dark:text-stone-400 line-clamp-2 mb-3 flex-1">{post.excerpt}</p>}

                <div className="flex items-center gap-2 text-xs text-stone-400 dark:text-stone-500 mt-auto pt-3 border-t border-stone-100 dark:border-stone-800">
                  <Calendar size={12} />
                  {post.publishedAt && <time dateTime={post.publishedAt}>{formatDate(post.publishedAt)}</time>}
                  <span>·</span>
                  <span>{post.readingTimeMin} phút đọc</span>
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <div className="text-center py-12 text-stone-400">
            <FileText size={48} className="mx-auto mb-4" />
            <p>Chưa có bài viết. Quay lại sau nhé!</p>
          </div>
        )}
      </section>

      {/* Newsletter CTA */}
      <section className="bg-stone-900 dark:bg-black text-white">
        <div className="max-w-5xl mx-auto px-4 py-16 md:py-20 text-center">
          <h2 className="text-2xl md:text-3xl font-bold mb-4">Đăng ký nhận bài mới</h2>
          <p className="text-stone-400 max-w-md mx-auto mb-8">
            Nhận bài viết mới qua email mỗi tuần. Không spam. Hủy đăng ký bất cứ lúc nào.
          </p>
          <Link href="/newsletter" className="inline-flex items-center gap-2 px-6 py-3 bg-white text-stone-900 rounded-lg font-medium hover:bg-stone-100 transition-colors">
            Đăng ký newsletter <ArrowRight size={18} />
          </Link>
        </div>
      </section>
    </>
  );
}
