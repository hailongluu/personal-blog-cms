import type { Metadata } from 'next';
import { Mail } from 'lucide-react';
import NewsletterForm from '@/components/NewsletterForm';

export const metadata: Metadata = {
  title: 'Newsletter',
  description: 'Đăng ký nhận bản tin để cập nhật bài viết và dự án mới nhất.',
  alternates: { canonical: '/newsletter' },
};

const perks = [
  { title: 'Cập nhật hàng tuần', desc: 'Bài viết và dự án mới gửi thẳng tới hộp thư của bạn.' },
  { title: 'Không spam', desc: 'Chỉ những nội dung tôi nghĩ bạn sẽ thấy giá trị.' },
  { title: 'Truy cập sớm', desc: 'Là người đầu tiên biết về dự án và ý tưởng mới.' },
];

export default function NewsletterPage() {
  return (
    <div className="max-w-2xl mx-auto px-4 py-12 md:py-20">
      <div className="text-center mb-10">
        <div className="w-16 h-16 rounded-full bg-stone-100 flex items-center justify-center mx-auto mb-6">
          <Mail size={28} className="text-stone-600" />
        </div>
        <h1 className="text-3xl md:text-4xl font-bold text-stone-900 mb-4">Đăng ký nhận bản tin</h1>
        <p className="text-lg text-stone-500 max-w-md mx-auto">
          Nhận thông báo khi tôi đăng bài mới. Không spam, hủy bất cứ lúc nào.
        </p>
      </div>

      <NewsletterForm />

      <div className="grid gap-4 sm:grid-cols-3 mt-12">
        {perks.map((perk) => (
          <div key={perk.title} className="text-center p-4">
            <h2 className="font-semibold text-stone-900 mb-1">{perk.title}</h2>
            <p className="text-sm text-stone-500">{perk.desc}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
