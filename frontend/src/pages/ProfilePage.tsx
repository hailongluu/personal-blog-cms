import { useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { authApi } from '@/lib/data';
import { Lock, Save, Eye, EyeOff, CheckCircle2, AlertCircle } from 'lucide-react';

export default function ProfilePage() {
  const { user, logout } = useAuth();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    // Client-side validation
    if (newPassword.length < 8) {
      setError('New password must be at least 8 characters.');
      return;
    }
    if (newPassword.length > 100) {
      setError('New password must be at most 100 characters.');
      return;
    }
    if (newPassword === currentPassword) {
      setError('New password must be different from current password.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('New password and confirmation do not match.');
      return;
    }

    setSaving(true);
    try {
      const result = await authApi.changePassword(currentPassword, newPassword);
      setSuccess(result.message + ' Logging out in 2 seconds...');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      // Auto-logout after password change (server revokes all sessions)
      setTimeout(() => logout(), 2000);
    } catch (e: any) {
      const msg = e?.response?.data?.error?.message ?? 'Failed to change password.';
      setError(msg);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-bold text-text mb-1">Profile</h1>
      <p className="text-text-muted mb-6">Manage your account settings.</p>

      {/* Account info */}
      <div className="bg-surface rounded-xl border border-border p-5 mb-6">
        <h2 className="font-semibold text-text mb-3">Account information</h2>
        <dl className="grid grid-cols-1 md:grid-cols-3 gap-3 text-sm">
          <div>
            <dt className="text-text-muted text-xs">Email</dt>
            <dd className="font-medium text-text">{user?.email ?? '—'}</dd>
          </div>
          <div>
            <dt className="text-text-muted text-xs">Display name</dt>
            <dd className="font-medium text-text">{user?.displayName ?? '—'}</dd>
          </div>
          <div>
            <dt className="text-text-muted text-xs">Role</dt>
            <dd>
              <span className="inline-block text-xs bg-primary-light/20 text-primary-dark px-2 py-0.5 rounded-full font-medium uppercase">
                {user?.role ?? '—'}
              </span>
            </dd>
          </div>
        </dl>
      </div>

      {/* Change password */}
      <div className="bg-surface rounded-xl border border-border p-5">
        <div className="flex items-center gap-2 mb-4">
          <Lock size={18} className="text-text-muted" />
          <h2 className="font-semibold text-text">Change password</h2>
        </div>

        {success && (
          <div className="mb-4 flex items-start gap-2 bg-green-50 border border-green-200 text-green-700 px-3 py-2.5 rounded-lg text-sm">
            <CheckCircle2 size={18} className="shrink-0 mt-0.5" />
            <span>{success}</span>
          </div>
        )}

        {error && (
          <div className="mb-4 flex items-start gap-2 bg-red-50 border border-red-200 text-red-700 px-3 py-2.5 rounded-lg text-sm">
            <AlertCircle size={18} className="shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Current password */}
          <div>
            <label className="block text-sm font-medium text-text mb-1">
              Current password
            </label>
            <div className="relative">
              <input
                type={showCurrent ? 'text' : 'password'}
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                required
                autoComplete="current-password"
                className="w-full px-3 py-2 pr-10 border border-border rounded-lg bg-bg focus:outline-none focus:ring-2 focus:ring-primary"
              />
              <button
                type="button"
                onClick={() => setShowCurrent(!showCurrent)}
                className="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-text-muted hover:text-text"
                tabIndex={-1}
              >
                {showCurrent ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>

          {/* New password */}
          <div>
            <label className="block text-sm font-medium text-text mb-1">
              New password <span className="text-text-muted font-normal">(8–100 characters)</span>
            </label>
            <div className="relative">
              <input
                type={showNew ? 'text' : 'password'}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                minLength={8}
                maxLength={100}
                autoComplete="new-password"
                className="w-full px-3 py-2 pr-10 border border-border rounded-lg bg-bg focus:outline-none focus:ring-2 focus:ring-primary"
              />
              <button
                type="button"
                onClick={() => setShowNew(!showNew)}
                className="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-text-muted hover:text-text"
                tabIndex={-1}
              >
                {showNew ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>

          {/* Confirm */}
          <div>
            <label className="block text-sm font-medium text-text mb-1">
              Confirm new password
            </label>
            <input
              type={showNew ? 'text' : 'password'}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
              autoComplete="new-password"
              className="w-full px-3 py-2 border border-border rounded-lg bg-bg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          {/* Submit */}
          <div className="flex items-center gap-3 pt-2">
            <button
              type="submit"
              disabled={saving || !!success}
              className="inline-flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg font-medium hover:bg-primary-dark transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Save size={16} />
              {saving ? 'Saving...' : 'Change password'}
            </button>
            <p className="text-xs text-text-muted">
              After change, all sessions will be revoked and you'll be logged out.
            </p>
          </div>
        </form>
      </div>
    </div>
  );
}
