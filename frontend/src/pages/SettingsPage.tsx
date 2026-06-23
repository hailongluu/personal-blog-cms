import { useState, useEffect, useCallback } from 'react';
import { settingsApi } from '@/lib/data';
import type { Settings } from '@/types';
import { Save, RefreshCw, Globe, User, Link2, Mail } from 'lucide-react';

const FIELD_CONFIG: { key: keyof Settings; label: string; icon: React.ReactNode; section: string }[] = [
  { key: 'site.title', label: 'Site Title', icon: <Globe size={16} />, section: 'Site' },
  { key: 'site.description', label: 'Site Description', icon: <Globe size={16} />, section: 'Site' },
  { key: 'site.author', label: 'Author Name', icon: <User size={16} />, section: 'Author' },
  { key: 'site.author_bio', label: 'Author Bio', icon: <User size={16} />, section: 'Author' },
  { key: 'social.github', label: 'GitHub URL', icon: <Link2 size={16} />, section: 'Social Links' },
  { key: 'social.linkedin', label: 'LinkedIn URL', icon: <Link2 size={16} />, section: 'Social Links' },
  { key: 'social.x', label: 'X (Twitter) URL', icon: <Link2 size={16} />, section: 'Social Links' },
  { key: 'social.youtube', label: 'YouTube URL', icon: <Link2 size={16} />, section: 'Social Links' },
  { key: 'site.email', label: 'Contact Email', icon: <Mail size={16} />, section: 'Contact' },
];

export default function SettingsPage() {
  const [settings, setSettings] = useState<Settings>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await settingsApi.getAdmin();
      setSettings(data);
    } catch (err: any) {
      setError(err.response?.data?.error?.message || err.response?.data?.error || 'Failed to load settings');
    }
    setLoading(false);
  }, []);

  useEffect(() => { load(); }, [load]);

  function updateField(key: keyof Settings, value: string) {
    setSettings(prev => ({ ...prev, [key]: value }));
    setSuccess('');
  }

  async function handleSave() {
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const updated = await settingsApi.update(settings);
      setSettings(updated);
      setSuccess('Settings saved successfully.');
    } catch (err: any) {
      setError(err.response?.data?.error?.message || err.response?.data?.error || 'Failed to save settings');
    }
    setSaving(false);
  }

  const sections = [...new Set(FIELD_CONFIG.map(f => f.section))];

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin w-8 h-8 border-4 border-primary border-t-transparent rounded-full" />
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-text">Settings</h1>
          <p className="text-text-muted text-sm">Manage site configuration and author info</p>
        </div>
        <div className="flex gap-2">
          <button onClick={load} className="p-2 border border-border rounded-lg hover:bg-bg" title="Refresh">
            <RefreshCw size={16} />
          </button>
          <button
            onClick={handleSave}
            disabled={saving}
            className="flex items-center gap-1.5 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary-dark text-sm font-medium disabled:opacity-50"
          >
            <Save size={16} />
            {saving ? 'Saving...' : 'Save Settings'}
          </button>
        </div>
      </div>

      {/* Status messages */}
      {error && (
        <div className="mb-4 bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-3 rounded-lg">
          {error}
        </div>
      )}
      {success && (
        <div className="mb-4 bg-emerald-50 border border-emerald-200 text-emerald-700 text-sm px-4 py-3 rounded-lg">
          {success}
        </div>
      )}

      {/* Settings form */}
      <div className="space-y-6">
        {sections.map(section => {
          const fields = FIELD_CONFIG.filter(f => f.section === section);
          return (
            <div key={section} className="bg-surface rounded-xl border border-border p-6">
              <h2 className="text-lg font-semibold text-text mb-4">{section}</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {fields.map(field => (
                  <div key={field.key}>
                    <label className="flex items-center gap-2 text-sm font-medium text-text-muted mb-1.5">
                      {field.icon}
                      {field.label}
                    </label>
                    {field.key === 'site.author_bio' ? (
                      <textarea
                        value={settings[field.key] || ''}
                        onChange={e => updateField(field.key, e.target.value)}
                        rows={3}
                        className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none resize-vertical"
                        placeholder={`Enter ${field.label.toLowerCase()}...`}
                      />
                    ) : (
                      <input
                        type={field.key === 'site.email' ? 'email' : 'text'}
                        value={settings[field.key] || ''}
                        onChange={e => updateField(field.key, e.target.value)}
                        className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none"
                        placeholder={`Enter ${field.label.toLowerCase()}...`}
                      />
                    )}
                  </div>
                ))}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
