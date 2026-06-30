import { useState, useEffect, useCallback } from 'react';
import { settingsApi } from '@/lib/data';
import type { Settings } from '@/types';
import {
  Save, RefreshCw, Globe, User, Link2, Mail,
  BarChart3, Code2, ShieldCheck, ExternalLink, AlertCircle, CheckCircle2
} from 'lucide-react';

// ── Client-side regex validators (mirror backend) ───────────────
const REGEX = {
  ga4: /^G-[A-Z0-9]{4,12}$/,
  gtm: /^GTM-[A-Z0-9]{4,12}$/,
  fb: /^\d{15,20}$/,
  tiktok: /^C[A-Z0-9]{14,20}$/,
};

type Tab = 'general' | 'tracking' | 'custom';

interface ValidationState {
  valid: boolean;
  message: string;
}

function validateField(key: keyof Settings, value: string | undefined): ValidationState {
  if (!value) return { valid: true, message: '' };
  switch (key) {
    case 'tracking.ga4_measurement_id':
      return REGEX.ga4.test(value)
        ? { valid: true, message: '' }
        : { valid: false, message: 'Format: G-XXXXXXXXXX (e.g., G-ABC123DEF4)' };
    case 'tracking.gtm_container_id':
      return REGEX.gtm.test(value)
        ? { valid: true, message: '' }
        : { valid: false, message: 'Format: GTM-XXXXXXX' };
    case 'tracking.fb_pixel_id':
      return REGEX.fb.test(value)
        ? { valid: true, message: '' }
        : { valid: false, message: 'Format: 15-20 digits (numbers only)' };
    case 'tracking.tiktok_pixel_id':
      return REGEX.tiktok.test(value)
        ? { valid: true, message: '' }
        : { valid: false, message: 'Format: CXXXXXXXXXXXXXXX' };
    default:
      return { valid: true, message: '' };
  }
}

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState<Tab>('general');
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

  function updateField(key: keyof Settings, value: string | boolean) {
    setSettings(prev => ({ ...prev, [key]: value }));
    setSuccess('');
  }

  async function handleSave() {
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      // Client-side validation
      const trackingKeys: (keyof Settings)[] = [
        'tracking.ga4_measurement_id',
        'tracking.gtm_container_id',
        'tracking.fb_pixel_id',
        'tracking.tiktok_pixel_id',
      ];
      for (const k of trackingKeys) {
        const val = settings[k] as string | undefined;
        if (val) {
          const v = validateField(k, val);
          if (!v.valid) {
            setError(`Invalid ${k}: ${v.message}`);
            setSaving(false);
            return;
          }
        }
      }

      const updated = await settingsApi.update(settings);
      setSettings(updated);
      setSuccess('Settings saved successfully.');
    } catch (err: any) {
      setError(err.response?.data?.error?.message || err.response?.data?.error || 'Failed to save settings');
    }
    setSaving(false);
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin w-8 h-8 border-4 border-primary border-t-transparent rounded-full" />
      </div>
    );
  }

  const tabs: { id: Tab; label: string; icon: React.ReactNode; desc: string }[] = [
    { id: 'general', label: 'General', icon: <Globe size={16} />, desc: 'Site info, author, social links' },
    { id: 'tracking', label: 'Tracking Scripts', icon: <BarChart3 size={16} />, desc: 'GA4, GTM, Facebook Pixel, TikTok Pixel' },
    { id: 'custom', label: 'Custom Code', icon: <Code2 size={16} />, desc: 'Head/body scripts and custom CSS' },
  ];

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-text">Settings</h1>
          <p className="text-text-muted text-sm">Manage site configuration and tracking scripts</p>
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

      {error && (
        <div className="mb-4 bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-3 rounded-lg flex items-start gap-2">
          <AlertCircle size={16} className="flex-shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}
      {success && (
        <div className="mb-4 bg-emerald-50 border border-emerald-200 text-emerald-700 text-sm px-4 py-3 rounded-lg flex items-start gap-2">
          <CheckCircle2 size={16} className="flex-shrink-0 mt-0.5" />
          <span>{success}</span>
        </div>
      )}

      {/* Tabs */}
      <div className="flex gap-1 border-b border-border mb-6">
        {tabs.map(t => (
          <button
            key={t.id}
            onClick={() => setActiveTab(t.id)}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
              activeTab === t.id
                ? 'border-primary text-primary'
                : 'border-transparent text-text-muted hover:text-text'
            }`}
          >
            {t.icon}
            {t.label}
          </button>
        ))}
      </div>

      {activeTab === 'general' && (
        <GeneralTab settings={settings} updateField={updateField} />
      )}
      {activeTab === 'tracking' && (
        <TrackingTab settings={settings} updateField={updateField} />
      )}
      {activeTab === 'custom' && (
        <CustomTab settings={settings} updateField={updateField} />
      )}
    </div>
  );
}

// ════════════════════════════════════════════════════════════════
// Tab 1: General (existing fields)
// ════════════════════════════════════════════════════════════════

function GeneralTab({ settings, updateField }: { settings: Settings; updateField: (k: keyof Settings, v: string) => void }) {
  return (
    <div className="space-y-6">
      <Section title="Site" icon={<Globe size={18} />}>
        <Field label="Site Title" value={settings['site.title']} onChange={v => updateField('site.title', v)} />
        <TextareaField label="Site Description" rows={3} value={settings['site.description']} onChange={v => updateField('site.description', v)} />
      </Section>

      <Section title="Author" icon={<User size={18} />}>
        <Field label="Author Name" value={settings['site.author']} onChange={v => updateField('site.author', v)} />
        <TextareaField label="Author Bio" rows={3} value={settings['site.author_bio']} onChange={v => updateField('site.author_bio', v)} />
      </Section>

      <Section title="Social Links" icon={<Link2 size={18} />}>
        <Field label="GitHub URL" value={settings['social.github']} onChange={v => updateField('social.github', v)} placeholder="https://github.com/username" />
        <Field label="LinkedIn URL" value={settings['social.linkedin']} onChange={v => updateField('social.linkedin', v)} placeholder="https://linkedin.com/in/username" />
        <Field label="X (Twitter) URL" value={settings['social.x']} onChange={v => updateField('social.x', v)} placeholder="https://x.com/username" />
        <Field label="YouTube URL" value={settings['social.youtube']} onChange={v => updateField('social.youtube', v)} placeholder="https://youtube.com/@channel" />
      </Section>

      <Section title="Contact" icon={<Mail size={18} />}>
        <Field label="Contact Email" type="email" value={settings['site.email']} onChange={v => updateField('site.email', v)} placeholder="you@example.com" />
      </Section>
    </div>
  );
}

// ════════════════════════════════════════════════════════════════
// Tab 2: Tracking Scripts
// ════════════════════════════════════════════════════════════════

function TrackingTab({ settings, updateField }: { settings: Settings; updateField: (k: keyof Settings, v: string | boolean) => void }) {
  const fields: Array<{
    key: keyof Settings;
    label: string;
    placeholder: string;
    helpUrl?: string;
    helpUrlLabel?: string;
  }> = [
    { key: 'tracking.gtm_container_id', label: 'GTM Container ID', placeholder: 'GTM-XXXXXXX',
      helpUrl: 'https://tagmanager.google.com', helpUrlLabel: 'Tag Manager' },
  ];

  return (
    <div className="space-y-6">
      <Section title="Google Tag Manager" icon={<BarChart3 size={18} />}>
        <div className="mb-4 bg-blue-50 border border-blue-200 text-blue-800 text-sm px-4 py-3 rounded-lg flex items-start gap-2">
          <ShieldCheck size={16} className="flex-shrink-0 mt-0.5" />
          <div>
            <strong>Chỉ cần GTM.</strong> Blog nhúng một container Google Tag Manager. Hãy quản lý
            GA4, Microsoft Clarity, Facebook Pixel… như các tag <em>bên trong</em> GTM — không cần
            khai báo riêng ở đây. ID được validate phía server; để trống = tắt.
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {fields.map(f => {
            const val = (settings[f.key] as string) || '';
            const validation = validateField(f.key, val);
            return (
              <div key={f.key}>
                <label className="block text-sm font-medium text-text-muted mb-1.5">
                  {f.label}
                </label>
                <input
                  type="text"
                  value={val}
                  onChange={e => updateField(f.key, e.target.value as string)}
                  placeholder={f.placeholder}
                  className={`w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none ${
                    val && !validation.valid ? 'border-red-300 bg-red-50' : 'border-border'
                  }`}
                />
                {val && !validation.valid && (
                  <p className="text-xs text-red-600 mt-1 flex items-center gap-1">
                    <AlertCircle size={12} /> {validation.message}
                  </p>
                )}
                {f.helpUrl && (
                  <a
                    href={f.helpUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-xs text-primary hover:underline mt-1 inline-flex items-center gap-1"
                  >
                    Get ID from {f.helpUrlLabel} <ExternalLink size={10} />
                  </a>
                )}
              </div>
            );
          })}
        </div>
      </Section>

      <Section title="GDPR Consent Mode" icon={<ShieldCheck size={18} />}>
        <label className="block text-sm font-medium text-text-muted mb-1.5">
          Consent Mode
        </label>
        <select
          value={(settings['tracking.consent_mode'] as string) || 'none'}
          onChange={e => updateField('tracking.consent_mode', e.target.value)}
          className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none"
        >
          <option value="none">None — Load all tracking by default</option>
          <option value="basic">Basic — Load analytics only after consent</option>
          <option value="full">Full — Require explicit consent for all</option>
        </select>
        <p className="text-xs text-text-muted mt-1">
          For full GDPR compliance, pair with a consent banner (CookieBot, OneTrust, etc.).
        </p>
      </Section>
    </div>
  );
}

// ════════════════════════════════════════════════════════════════
// Tab 3: Custom Code
// ════════════════════════════════════════════════════════════════

function CustomTab({ settings, updateField }: { settings: Settings; updateField: (k: keyof Settings, v: string) => void }) {
  return (
    <div className="space-y-6">
      <div className="bg-amber-50 border border-amber-200 text-amber-800 text-sm px-4 py-3 rounded-lg flex items-start gap-2">
        <ShieldCheck size={16} className="flex-shrink-0 mt-0.5" />
        <div>
          <strong>Security:</strong> Custom code is sanitized server-side with a strict allowlist.
          Allowed tags: <code>&lt;script&gt;</code> (Google/Facebook/TikTok/Microsoft src only),{' '}
          <code>&lt;style&gt;</code>, <code>&lt;meta&gt;</code>, <code>&lt;link&gt;</code>.
          Forbidden: <code>&lt;iframe&gt;</code>, <code>&lt;object&gt;</code>, event handlers,{' '}
          <code>javascript:</code>/<code>data:</code> URLs.
        </div>
      </div>

      <Section title="Head Scripts (before &lt;/head&gt;)" icon={<Code2 size={18} />}>
        <CodeArea
          rows={6}
          value={(settings['custom.head_scripts'] as string) || ''}
          onChange={v => updateField('custom.head_scripts', v)}
          placeholder={`<script async src="https://www.googletagmanager.com/gtag/js?id=G-XXXXX"></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());
  gtag('config', 'G-XXXXX');
</script>`}
        />
        <p className="text-xs text-text-muted mt-2">
          Use for: meta verification tags, preconnect hints, gtag init, Microsoft Clarity, Hotjar.
        </p>
      </Section>

      <Section title="Body Start Scripts (after &lt;body&gt;)" icon={<Code2 size={18} />}>
        <CodeArea
          rows={4}
          value={(settings['custom.body_start_scripts'] as string) || ''}
          onChange={v => updateField('custom.body_start_scripts', v)}
          placeholder={`<!-- e.g., GTM noscript fallback, on-page chatbots -->`}
        />
      </Section>

      <Section title="Body End Scripts (before &lt;/body&gt;)" icon={<Code2 size={18} />}>
        <CodeArea
          rows={4}
          value={(settings['custom.body_end_scripts'] as string) || ''}
          onChange={v => updateField('custom.body_end_scripts', v)}
          placeholder={`<!-- e.g., defer-loaded widgets, analytics beacons -->`}
        />
      </Section>

      <Section title="Custom CSS" icon={<Code2 size={18} />}>
        <CodeArea
          rows={6}
          value={(settings['custom.css'] as string) || ''}
          onChange={v => updateField('custom.css', v)}
          placeholder={`/* Site-wide CSS — loaded in <head> as <style> */
:root {
  --brand-primary: #2563eb;
}
.hero-title {
  font-family: 'Inter', sans-serif;
}`}
          language="css"
        />
      </Section>
    </div>
  );
}

// ════════════════════════════════════════════════════════════════
// Shared sub-components
// ════════════════════════════════════════════════════════════════

function Section({ title, icon, children }: { title: string; icon: React.ReactNode; children: React.ReactNode }) {
  return (
    <div className="bg-surface rounded-xl border border-border p-6">
      <h2 className="text-lg font-semibold text-text mb-4 flex items-center gap-2">{icon} {title}</h2>
      {children}
    </div>
  );
}

function Field({ label, value, onChange, type = 'text', placeholder }: {
  label: string;
  value?: string;
  onChange: (v: string) => void;
  type?: string;
  placeholder?: string;
}) {
  return (
    <div>
      <label className="block text-sm font-medium text-text-muted mb-1.5">{label}</label>
      <input
        type={type}
        value={value || ''}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder || `Enter ${label.toLowerCase()}...`}
        className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none"
      />
    </div>
  );
}

function TextareaField({ label, rows, value, onChange }: {
  label: string;
  rows: number;
  value?: string;
  onChange: (v: string) => void;
}) {
  return (
    <div>
      <label className="block text-sm font-medium text-text-muted mb-1.5">{label}</label>
      <textarea
        value={value || ''}
        onChange={e => onChange(e.target.value)}
        rows={rows}
        className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:ring-2 focus:ring-primary outline-none resize-vertical"
        placeholder={`Enter ${label.toLowerCase()}...`}
      />
    </div>
  );
}

function CodeArea({ rows, value, onChange, placeholder }: {
  rows: number;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  language?: 'css' | 'html';
}) {
  return (
    <textarea
      value={value}
      onChange={e => onChange(e.target.value)}
      rows={rows}
      spellCheck={false}
      className="w-full px-3 py-2 border border-border rounded-lg text-xs font-mono focus:ring-2 focus:ring-primary outline-none resize-vertical bg-gray-50"
      placeholder={placeholder}
    />
  );
}

