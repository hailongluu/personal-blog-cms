import { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Helmet } from 'react-helmet-async';
import { ArrowLeft, Lock, Eye, EyeOff, CheckCircle } from 'lucide-react';
import { authApi } from '@/lib/data';

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const token = searchParams.get('token') || '';
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (!token) {
      setError('Link không hợp lệ. Vui lòng yêu cầu link mới.');
    }
  }, [token]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (newPassword.length < 8) {
      setError('Mật khẩu phải có ít nhất 8 ký tự.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.');
      return;
    }

    setSubmitting(true);
    try {
      await authApi.resetPassword(token, newPassword);
      setSuccess(true);
      // Auto redirect after 3 seconds
      setTimeout(() => navigate('/login'), 3000);
    } catch (err: any) {
      const msg = err.response?.data?.error?.message || 'Đặt lại mật khẩu thất bại. Link có thể đã hết hạn.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <Helmet>
        <title>Đặt lại mật khẩu — Personal Blog Admin</title>
        <meta name="robots" content="noindex" />
      </Helmet>

      <div className="min-h-screen flex items-center justify-center bg-stone-50 px-4 py-12">
        <div className="w-full max-w-md">
          <Link
            to="/login"
            className="inline-flex items-center gap-1 text-sm text-stone-500 hover:text-stone-700 mb-6 transition-colors"
          >
            <ArrowLeft size={16} /> Quay lại đăng nhập
          </Link>

          <div className="bg-white rounded-2xl border border-stone-200 p-8 shadow-sm">
            {!success ? (
              <>
                <div className="flex items-center gap-3 mb-6">
                  <div className="w-12 h-12 bg-stone-100 rounded-xl flex items-center justify-center">
                    <Lock size={24} className="text-stone-600" />
                  </div>
                  <div>
                    <h1 className="text-xl font-bold text-stone-900">Đặt lại mật khẩu</h1>
                    <p className="text-sm text-stone-500">Nhập mật khẩu mới của bạn</p>
                  </div>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                  <div>
                    <label htmlFor="newPassword" className="block text-sm font-medium text-stone-700 mb-1">
                      Mật khẩu mới
                    </label>
                    <div className="relative">
                      <input
                        id="newPassword"
                        type={showPassword ? 'text' : 'password'}
                        required
                        minLength={8}
                        autoComplete="new-password"
                        autoFocus
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        className="w-full px-4 py-2.5 pr-12 border border-stone-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-stone-900 focus:border-transparent transition-colors"
                        placeholder="Tối thiểu 8 ký tự"
                        disabled={!token || submitting}
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-stone-400 hover:text-stone-600"
                        tabIndex={-1}
                      >
                        {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                      </button>
                    </div>
                  </div>

                  <div>
                    <label htmlFor="confirmPassword" className="block text-sm font-medium text-stone-700 mb-1">
                      Xác nhận mật khẩu
                    </label>
                    <input
                      id="confirmPassword"
                      type={showPassword ? 'text' : 'password'}
                      required
                      minLength={8}
                      autoComplete="new-password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      className="w-full px-4 py-2.5 border border-stone-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-stone-900 focus:border-transparent transition-colors"
                      placeholder="Nhập lại mật khẩu"
                      disabled={!token || submitting}
                    />
                  </div>

                  {error && (
                    <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3">
                      {error}
                    </div>
                  )}

                  <button
                    type="submit"
                    disabled={submitting || !token || !newPassword || !confirmPassword}
                    className="w-full py-2.5 bg-stone-900 text-white rounded-lg font-medium hover:bg-stone-800 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {submitting ? 'Đang đặt lại...' : 'Đặt lại mật khẩu'}
                  </button>
                </form>

                <p className="text-xs text-stone-400 text-center mt-6">
                  Mật khẩu phải có ít nhất 8 ký tự. Sau khi đặt lại, tất cả phiên đăng nhập khác sẽ bị đăng xuất.
                </p>
              </>
            ) : (
              <div className="text-center py-4">
                <div className="w-16 h-16 mx-auto mb-4 bg-green-50 rounded-full flex items-center justify-center">
                  <CheckCircle size={32} className="text-green-600" />
                </div>
                <h2 className="text-lg font-bold text-stone-900 mb-2">Đặt lại thành công!</h2>
                <p className="text-sm text-stone-500 mb-6">
                  Mật khẩu của bạn đã được cập nhật. Đang chuyển về trang đăng nhập...
                </p>
                <Link
                  to="/login"
                  className="inline-flex items-center gap-1 text-sm font-medium text-stone-700 hover:text-stone-900 transition-colors"
                >
                  Đăng nhập ngay
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  );
}
