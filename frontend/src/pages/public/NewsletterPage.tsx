import { useState, type FormEvent } from 'react';
import { Helmet } from 'react-helmet-async';
import { Mail, Send, CheckCircle2 } from 'lucide-react';
import { subscribeNewsletter } from '@/lib/publicApi';

export default function NewsletterPage() {
  const [email, setEmail] = useState('');
  const [status, setStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
  const [message, setMessage] = useState('');

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!email.trim()) return;

    setStatus('loading');
    setMessage('');

    try {
      const res = await subscribeNewsletter(email.trim());
      setMessage(res.data?.message || 'Thanks for subscribing! Check your inbox to confirm.');
      setStatus('success');
      setEmail('');
    } catch (err: any) {
      setMessage(err.response?.data?.error?.message || 'Something went wrong. Please try again.');
      setStatus('error');
    }
  }

  return (
    <>
      <Helmet>
        <title>Newsletter — Personal Blog</title>
        <meta name="description" content="Subscribe to my newsletter for updates on new posts and projects." />
      </Helmet>

      <div className="max-w-2xl mx-auto px-4 py-12 md:py-20">
        <div className="text-center mb-10">
          <div className="w-16 h-16 rounded-full bg-stone-100 flex items-center justify-center mx-auto mb-6">
            <Mail size={28} className="text-stone-600" />
          </div>
          <h1 className="text-3xl md:text-4xl font-bold text-stone-900 mb-4">
            Join the newsletter
          </h1>
          <p className="text-lg text-stone-500 max-w-md mx-auto">
            Get notified when I publish new posts. No spam, unsubscribe anytime.
          </p>
        </div>

        {/* Form */}
        {status === 'success' ? (
          <div className="bg-green-50 border border-green-200 rounded-xl p-8 text-center">
            <CheckCircle2 size={48} className="text-green-500 mx-auto mb-4" />
            <h2 className="text-xl font-semibold text-green-800 mb-2">You're in!</h2>
            <p className="text-green-600">{message}</p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="bg-white rounded-xl border border-stone-200 p-8 shadow-sm">
            <div className="flex flex-col sm:flex-row gap-3">
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                required
                disabled={status === 'loading'}
                className="flex-1 px-4 py-3 border border-stone-300 rounded-lg text-stone-900 placeholder-stone-400 focus:ring-2 focus:ring-stone-500 focus:border-stone-500 outline-none text-sm disabled:opacity-50"
              />
              <button
                type="submit"
                disabled={status === 'loading'}
                className="inline-flex items-center justify-center gap-2 px-6 py-3 bg-stone-900 text-white rounded-lg font-medium hover:bg-stone-800 transition-colors disabled:opacity-50"
              >
                {status === 'loading' ? 'Subscribing...' : (
                  <>
                    Subscribe <Send size={16} />
                  </>
                )}
              </button>
            </div>

            {status === 'error' && (
              <p className="mt-4 text-sm text-red-600">{message}</p>
            )}

            <p className="mt-4 text-xs text-stone-400 text-center">
              No spam, ever. Unsubscribe with one click.
            </p>
          </form>
        )}

        {/* Perks */}
        <div className="grid gap-4 sm:grid-cols-3 mt-12">
          {[
            { title: 'Weekly updates', desc: 'New posts and projects delivered to your inbox.' },
            { title: 'No spam', desc: 'Only content I think you\'ll find valuable.' },
            { title: 'Early access', desc: 'Be the first to know about new projects and ideas.' },
          ].map((perk) => (
            <div key={perk.title} className="text-center p-4">
              <h3 className="font-semibold text-stone-900 mb-1">{perk.title}</h3>
              <p className="text-sm text-stone-500">{perk.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </>
  );
}
