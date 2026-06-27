import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Helmet } from 'react-helmet-async';
import { ArrowRight, FileText, Calendar } from 'lucide-react';
import { getPublishedPosts } from '@/lib/publicApi';
import type { Post } from '@/types';

export default function HomePage() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getPublishedPosts(1, 6)
      .then((data) => setPosts(data.data))
      .catch(() => setPosts([]))
      .finally(() => setLoading(false));
  }, []);

  return (
    <>
      <Helmet>
        <title>Lưu Hải Long — Góc nhìn thực chiến về AI, công nghệ và sản phẩm số</title>
        <meta
          name="description"
          content="Blog cá nhân của Lưu Hải Long. Chia sẻ góc nhìn thực chiến về AI, dữ liệu lớn, hệ thống và sản phẩm số — phân tích, hướng dẫn và đánh giá công cụ mới nhất."
        />
        <meta property="og:title" content="Lưu Hải Long — Góc nhìn thực chiến về AI, công nghệ và sản phẩm số" />
        <meta property="og:description" content="Chia sẻ kiến thức chuyên sâu về AI, dữ liệu lớn và hệ thống phần mềm. Bởi Lưu Hải Long." />
        <meta property="og:type" content="website" />
        <link rel="canonical" href="https://news.luuhailong.com/" />
      </Helmet>

      {/* Hero */}
      <section className="bg-white border-b border-stone-200">
        <div className="max-w-5xl mx-auto px-4 py-16 md:py-24 text-center">
          <p className="text-xs sm:text-sm font-medium tracking-widest uppercase text-stone-500 mb-4">
            Lưu Hải Long
          </p>
          <h1 className="text-3xl sm:text-4xl md:text-5xl lg:text-6xl font-bold tracking-tight text-stone-900 mb-6 leading-tight">
            Góc nhìn thực chiến về{' '}
            <span className="bg-gradient-to-r from-amber-600 via-orange-600 to-rose-600 bg-clip-text text-transparent">
              AI, công nghệ
            </span>
            <br />
            <span className="text-stone-700">và sản phẩm số</span>
          </h1>
          <p className="text-base sm:text-lg text-stone-600 max-w-2xl mx-auto mb-3 px-4 leading-relaxed">
            Tôi viết về cách ứng dụng <strong className="text-stone-800">AI</strong>,{' '}
            <strong className="text-stone-800">dữ liệu lớn</strong> và{' '}
            <strong className="text-stone-800">công nghệ</strong> để giải quyết bài toán thật
            trong doanh nghiệp.
          </p>
          <p className="text-sm text-stone-500 max-w-xl mx-auto mb-8 px-4">
            Phân tích chuyên sâu · Hướng dẫn thực hành · Đánh giá công cụ mới nhất
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 sm:gap-4">
            <Link
              to="/blog"
              className="inline-flex items-center gap-2 px-6 py-3 bg-stone-900 text-white rounded-lg font-medium hover:bg-stone-800 transition-colors w-full sm:w-auto justify-center shadow-sm"
            >
              Đọc bài mới nhất <ArrowRight size={18} />
            </Link>
            <Link
              to="/topics"
              className="inline-flex items-center gap-2 px-6 py-3 border border-stone-300 text-stone-700 rounded-lg font-medium hover:bg-stone-100 transition-colors w-full sm:w-auto justify-center"
            >
              Khám phá chủ đề
            </Link>
          </div>
        </div>
      </section>

      {/* Latest Posts — responsive grid */}
      <section className="max-w-6xl mx-auto px-4 py-12 md:py-20">
        <div className="flex items-center justify-between mb-8 md:mb-10">
          <div>
            <h2 className="text-xl sm:text-2xl font-bold text-stone-900">Bài viết mới nhất</h2>
            <p className="text-sm text-stone-500 mt-1">Cập nhật liên tục · {new Date().getFullYear()}</p>
          </div>
          <Link
            to="/blog"
            className="text-sm font-medium text-stone-600 hover:text-stone-900 transition-colors whitespace-nowrap"
          >
            Xem tất cả →
          </Link>
        </div>

        {loading ? (
          <div className="grid gap-4 sm:gap-6 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <div key={i} className="bg-white rounded-xl border border-stone-200 p-5 animate-pulse">
                <div className="w-full h-36 sm:h-40 bg-stone-200 rounded-lg mb-4" />
                <div className="h-4 bg-stone-200 rounded w-3/4 mb-3" />
                <div className="h-3 bg-stone-100 rounded w-full mb-2" />
                <div className="h-3 bg-stone-100 rounded w-2/3" />
              </div>
            ))}
          </div>
        ) : posts.length > 0 ? (
          <div className="grid gap-4 sm:gap-6 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
            {posts.map((post) => (
              <Link
                key={post.id}
                to={`/blog/${post.slug}`}
                className="group bg-white rounded-xl border border-stone-200 p-5 hover:border-stone-400 hover:shadow-lg transition-all duration-200 flex flex-col"
              >
                {post.coverImageUrl ? (
                  <img
                    src={post.coverImageUrl}
                    alt={post.title}
                    className="w-full h-36 sm:h-40 object-cover rounded-lg mb-4"
                    loading="lazy"
                  />
                ) : (
                  <div className="w-full h-36 sm:h-40 bg-gradient-to-br from-stone-100 to-stone-200 rounded-lg mb-4 flex items-center justify-center">
                    <FileText size={32} className="text-stone-300" />
                  </div>
                )}

                {post.topic && (
                  <span
                    className="inline-block text-xs font-medium px-2 py-0.5 rounded-full mb-2 self-start"
                    style={{ backgroundColor: post.topic.color + '20', color: post.topic.color }}
                  >
                    {post.topic.name}
                  </span>
                )}

                <h3 className="font-semibold text-stone-900 group-hover:text-stone-600 transition-colors mb-2 line-clamp-2 leading-snug">
                  {post.title}
                </h3>

                {post.excerpt && (
                  <p className="text-sm text-stone-500 line-clamp-2 mb-3 flex-1">{post.excerpt}</p>
                )}

                <div className="flex items-center gap-2 text-xs text-stone-400 mt-auto pt-3 border-t border-stone-100">
                  <Calendar size={12} />
                  {post.publishedAt && (
                    <time dateTime={post.publishedAt}>
                      {new Date(post.publishedAt).toLocaleDateString('vi-VN', {
                        day: '2-digit',
                        month: '2-digit',
                        year: 'numeric',
                      })}
                    </time>
                  )}
                  <span>·</span>
                  <span>{post.readingTimeMin} phút đọc</span>
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <div className="text-center py-12 text-stone-400">
            <FileText size={48} className="mx-auto mb-4 mx-auto" />
            <p>Chưa có bài viết. Quay lại sau nhé!</p>
          </div>
        )}
      </section>

      {/* CTA Newsletter */}
      <section className="bg-stone-900 text-white">
        <div className="max-w-5xl mx-auto px-4 py-16 md:py-20 text-center">
          <h2 className="text-2xl md:text-3xl font-bold mb-4">Đăng ký nhận bài mới</h2>
          <p className="text-stone-400 max-w-md mx-auto mb-8">
            Nhận bài viết mới qua email mỗi tuần. Không spam. Hủy đăng ký bất cứ lúc nào.
          </p>
          <Link
            to="/newsletter"
            className="inline-flex items-center gap-2 px-6 py-3 bg-white text-stone-900 rounded-lg font-medium hover:bg-stone-100 transition-colors"
          >
            Đăng ký newsletter <ArrowRight size={18} />
          </Link>
        </div>
      </section>
    </>
  );
}
