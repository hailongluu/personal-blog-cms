import Link from 'next/link';
import { ArrowLeft, ArrowRight } from 'lucide-react';

export default function Pagination({ basePath, page, totalPages }: { basePath: string; page: number; totalPages: number }) {
  if (totalPages <= 1) return null;
  const href = (p: number) => `${basePath}?page=${p}`;
  const linkCls = 'inline-flex items-center gap-1.5 text-sm font-medium text-stone-700 hover:text-stone-900';

  return (
    <nav className="flex items-center justify-between mt-12 pt-6 border-t border-stone-200" aria-label="Phân trang">
      {page > 1 ? (
        <Link href={href(page - 1)} className={linkCls} rel="prev"><ArrowLeft size={16} /> Trước</Link>
      ) : <span />}
      <span className="text-sm text-stone-400">Trang {page} / {totalPages}</span>
      {page < totalPages ? (
        <Link href={href(page + 1)} className={linkCls} rel="next">Sau <ArrowRight size={16} /></Link>
      ) : <span />}
    </nav>
  );
}
