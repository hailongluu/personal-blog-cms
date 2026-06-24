import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Helmet } from 'react-helmet-async';
import { ArrowLeft, Mail, CheckCircle } from 'lucide-react';
import { authApi } from '@/lib/data';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await authApi.forgotPassword(email.trim());
      setSubmitted(true);
    } catch (err: any) {
      const msg = err.response?.data?.error?.message || 'Đã xảy ra lỗi. Vui lòng thử lại.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <Helmet>
        <title>Quên mật khẩu — Personal Blog Admin</title>
        <meta name="robots" content="noindex" />
      </Helmet>

      <div className="min-h-screen flex items-center justify-center bg-stone-50 px-4 py-12">
        <div className="w-full max-w-md">
          {/* Back link */}
          <Link
            to="/login"
            className="inline-flex items-center gap-1 text-sm text-stone-500 hover:text-stone-700 mb-6 transition-colors"
          >
            <ArrowLeft size={16} /> Quay lại đăng nhập
          </Link>

          {/* Card */}
          <div className="bg-white rounded-2xl border border-stone-200 p-8 shadow-sm">
            {!submitted ? (
              <>
                {/* Header */}
                <div className="flex items-center gap-3 mb-6">
                  <div className="w-12 h-12 bg-stone-100 rounded-xl flex items-center justify-center">
                    <Mail size={24} className="text-stone-600" />
                  </div>
                  <div>
                    <h1 className="text-xl font-bold text-stone-900">Quên mật khẩu</h1>
                    <p className="text-sm text-stone-500">Nhập email để nhận link đặt lại mật khẩu</p>
                  </div>
                </div>

                {/* Form */}
                <form onSubmit={handleSubmit} className="space-y-4">
                  <div>
                    <label htmlFor="email" className="block text-sm font-medium text-stone-700 mb-1">
                      Email
                    </label>
                    <input
                      id="email"
                      type="email"
                      required
                      autoComplete="email"
                      autoFocus
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      className="w-full px-4 py-2.5 border border-stone-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-stone-900 focus:border-transparent transition-colors"
                      placeholder="admin@example.com"
                      disabled={submitting}
                    />
                  </div>

                  {error && (
                    <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3">
                      {error}
                    </div>
                  )}

                  <button
                    type="submit"
                    disabled={submitting || !email}
                    className="w-full py-2.5 bg-stone-900 text-white rounded-lg font-medium hover:bg-stone-800 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {submitting ? 'Đang gửi...' : 'Gửi link đặt lại'}
                  </button>
                </form>

                <p className="text-xs text-stone-400 text-center mt-6">
                  Vì lý do bảo mật, link sẽ hết hạn sau 1 giờ và chỉ dùng được một lần.
                </p>
              </>
            ) : (
              /* Success state */
              <div className="text-center py-4">
                <div className="w-16 h-16 mx-auto mb-4 bg-green-50 rounded-full flex items-center justify-center">
                  <CheckCircle size={32} className="text-green-600" />
                </div>
                <h2 className="text-lg font-bold text-stone-900 mb-2">Đã gửi email!</h2>
                <p className="text-sm text-stone-500 mb-6">
                  Nếu email <span className="font-medium text-stone-700">{email}</span> tồn tại trong hệ thống,
                  bạn sẽ nhận được link đặt lại mật khẩu.
                </p>
                <Link
                  to="/login"
                  className="inline-flex items-center gap-1 text-sm font-medium text-stone-700 hover:text-stone-900 transition-colors"
                >
                  <ArrowLeft size={16} /> Quay lại đăng nhập
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  );
}
