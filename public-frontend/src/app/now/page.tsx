import type { Metadata } from 'next';
import { MapPin, BookOpen, Code, Coffee } from 'lucide-react';

export const metadata: Metadata = {
  title: 'Now',
  description: 'Những gì tôi đang tập trung hiện tại — dự án, học tập và cuộc sống.',
  alternates: { canonical: '/now' },
};

const nowItems = [
  { icon: Code, title: 'Đang xây dựng', description: 'Một hệ thống CMS cá nhân với React, Java/Spring và PostgreSQL. Khám phá kiến trúc serverless và edge computing.' },
  { icon: BookOpen, title: 'Đang đọc', description: '"The Pragmatic Programmer" của David Thomas & Andrew Hunt — đọc lại cuốn kinh điển này để tìm góc nhìn mới.' },
  { icon: MapPin, title: 'Vị trí', description: 'Làm việc từ xa và tận hưởng sự linh hoạt mà nó mang lại.' },
  { icon: Coffee, title: 'Cuộc sống', description: 'Tập chụp ảnh, cải thiện kỹ thuật pha cà phê và cố gắng chạy bộ đều đặn hơn.' },
];

export default function NowPage() {
  return (
    <div className="max-w-3xl mx-auto px-4 py-12 md:py-16">
      <div className="mb-12">
        <h1 className="text-3xl md:text-4xl font-bold text-stone-900 mb-4">Now</h1>
        <p className="text-lg text-stone-500">
          Những gì tôi đang tập trung hiện tại. Lấy cảm hứng từ{' '}
          <a href="https://nownownow.com/about" target="_blank" rel="noopener noreferrer" className="text-stone-700 underline hover:text-stone-900">
            phong trào /now của Derek Sivers
          </a>.
        </p>
        <p className="text-sm text-stone-400 mt-2">Cập nhật lần cuối: Tháng 6, 2026</p>
      </div>

      <div className="grid gap-6 sm:grid-cols-2">
        {nowItems.map((item) => (
          <div key={item.title} className="bg-white rounded-xl border border-stone-200 p-6 hover:border-stone-300 transition-colors">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-10 h-10 rounded-lg bg-stone-100 flex items-center justify-center">
                <item.icon size={20} className="text-stone-600" />
              </div>
              <h2 className="font-semibold text-stone-900">{item.title}</h2>
            </div>
            <p className="text-sm text-stone-500 leading-relaxed">{item.description}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
