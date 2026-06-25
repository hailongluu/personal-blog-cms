import { useState, useEffect, useCallback } from 'react';
import {
  scheduledTasksApi,
  type CronJob,
  type ContentRegistryPage,
  type ScheduledPostItem,
  type NewsletterLogItem,
} from '@/lib/data';
import {
  Clock, Play, Pause, Trash2, RefreshCw, RotateCcw,
  XCircle, FileText, Mail, AlertCircle, CheckCircle2, Filter,
} from 'lucide-react';

type TabKey = 'cron' | 'registry' | 'posts' | 'newsletter';

export default function ScheduledTasksPage() {
  const [tab, setTab] = useState<TabKey>('cron');

  // Aggregate counts
  const [cronJobs, setCronJobs] = useState<CronJob[]>([]);
  const [cronError, setCronError] = useState<string | null>(null);
  const [registry, setRegistry] = useState<ContentRegistryPage | null>(null);
  const [registryTotal, setRegistryTotal] = useState(0);
  const [scheduledPosts, setScheduledPosts] = useState<ScheduledPostItem[]>([]);
  const [newsletterLog, setNewsletterLog] = useState<NewsletterLogItem[]>([]);

  // Registry filters
  const [regSource, setRegSource] = useState('');
  const [regStatus, setRegStatus] = useState('');

  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const flash = (kind: 'ok' | 'err', msg: string) => {
    if (kind === 'ok') setSuccess(msg);
    else setError(msg);
    setTimeout(() => { setSuccess(null); setError(null); }, 3500);
  };

  const loadAggregate = useCallback(async () => {
    setLoading(true);
    try {
      const agg = await scheduledTasksApi.aggregate();
      setCronJobs(agg.cronJobs ?? []);
      setCronError(agg.cronJobsError ?? null);
      setScheduledPosts(agg.scheduledPosts ?? []);
      setNewsletterLog(agg.newsletterLog ?? []);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { error?: string } }; message?: string };
      flash('err', err.response?.data?.error || err.message || 'Failed to load');
    } finally {
      setLoading(false);
    }
  }, []);

  const loadRegistry = useCallback(async () => {
    setLoading(true);
    try {
      const params: { page: number; size: number; source?: string; status?: string } = {
        page: 0, size: 50,
      };
      if (regSource) params.source = regSource;
      if (regStatus) params.status = regStatus;
      const data = await scheduledTasksApi.listContentRegistry(params);
      setRegistry(data);
      setRegistryTotal(data.totalItems);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { error?: string } }; message?: string };
      flash('err', err.response?.data?.error || err.message || 'Failed to load');
    } finally {
      setLoading(false);
    }
  }, [regSource, regStatus]);

  useEffect(() => { loadAggregate(); }, [loadAggregate]);
  useEffect(() => { if (tab === 'registry') loadRegistry(); }, [tab, loadRegistry]);

  // ─── Cron actions ───
  const runCron = async (jobId: string) => {
    setBusy(jobId + ':run');
    try { const msg = await scheduledTasksApi.runCron(jobId); flash('ok', msg || `Triggered ${jobId}`); loadAggregate(); }
    catch (e) { handleError(e, 'run cron'); }
    finally { setBusy(null); }
  };
  const pauseCron = async (jobId: string) => {
    if (!confirm(`Pause cron job ${jobId}?`)) return;
    setBusy(jobId + ':pause');
    try { const msg = await scheduledTasksApi.pauseCron(jobId); flash('ok', msg || `Paused ${jobId}`); loadAggregate(); }
    catch (e) { handleError(e, 'pause cron'); }
    finally { setBusy(null); }
  };
  const resumeCron = async (jobId: string) => {
    setBusy(jobId + ':resume');
    try { const msg = await scheduledTasksApi.resumeCron(jobId); flash('ok', msg || `Resumed ${jobId}`); loadAggregate(); }
    catch (e) { handleError(e, 'resume cron'); }
    finally { setBusy(null); }
  };
  const deleteCron = async (jobId: string) => {
    if (!confirm(`DELETE cron job ${jobId}? Cannot undo!`)) return;
    setBusy(jobId + ':delete');
    try { const msg = await scheduledTasksApi.deleteCron(jobId); flash('ok', msg || `Deleted ${jobId}`); loadAggregate(); }
    catch (e) { handleError(e, 'delete cron'); }
    finally { setBusy(null); }
  };

  // ─── Registry actions ───
  const rejectReg = async (id: number) => {
    if (!confirm(`Reject content registry item ${id}?`)) return;
    setBusy('reg:' + id + ':reject');
    try { await scheduledTasksApi.rejectContentRegistry(id); flash('ok', `Rejected #${id}`); loadRegistry(); }
    catch (e) { handleError(e, 'reject'); }
    finally { setBusy(null); }
  };
  const deleteReg = async (id: number) => {
    if (!confirm(`DELETE content registry item ${id}? Cannot undo!`)) return;
    setBusy('reg:' + id + ':delete');
    try { await scheduledTasksApi.deleteContentRegistry(id); flash('ok', `Deleted #${id}`); loadRegistry(); }
    catch (e) { handleError(e, 'delete'); }
    finally { setBusy(null); }
  };

  // ─── Post actions ───
  const publishPostNow = async (id: number) => {
    if (!confirm(`Publish scheduled post #${id} immediately?`)) return;
    setBusy('post:' + id + ':publish');
    try { await scheduledTasksApi.publishPostNow(id); flash('ok', `Published #${id}`); loadAggregate(); }
    catch (e) { handleError(e, 'publish'); }
    finally { setBusy(null); }
  };
  const cancelSchedule = async (id: number) => {
    if (!confirm(`Cancel scheduled publish for #${id}?`)) return;
    setBusy('post:' + id + ':cancel');
    try { await scheduledTasksApi.cancelScheduledPost(id); flash('ok', `Cancelled #${id}`); loadAggregate(); }
    catch (e) { handleError(e, 'cancel'); }
    finally { setBusy(null); }
  };

  // ─── Newsletter actions ───
  const resendNewsletter = async (id: number) => {
    if (!confirm(`Resend newsletter #${id} to all subscribers?`)) return;
    setBusy('news:' + id + ':resend');
    try { await scheduledTasksApi.resendNewsletter(id); flash('ok', `Resent #${id}`); loadAggregate(); }
    catch (e) { handleError(e, 'resend'); }
    finally { setBusy(null); }
  };
  const deleteNewsletter = async (id: number) => {
    if (!confirm(`DELETE newsletter log #${id}?`)) return;
    setBusy('news:' + id + ':delete');
    try { await scheduledTasksApi.deleteNewsletterLog(id); flash('ok', `Deleted #${id}`); loadAggregate(); }
    catch (e) { handleError(e, 'delete'); }
    finally { setBusy(null); }
  };

  function handleError(e: unknown, action: string) {
    const err = e as { response?: { data?: { error?: string } }; message?: string };
    flash('err', `${action}: ${err.response?.data?.error || err.message || 'unknown error'}`);
  }

  return (
    <div className="p-6 max-w-7xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Clock className="w-6 h-6" />
            Scheduled Tasks
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Quản lý cron jobs, content engine, scheduled posts & newsletter
          </p>
        </div>
        <button
          onClick={() => { loadAggregate(); if (tab === 'registry') loadRegistry(); }}
          className="flex items-center gap-1.5 px-3 py-1.5 text-sm text-gray-700 border rounded hover:bg-gray-50"
          title="Refresh"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* Flash messages */}
      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-800 rounded flex items-start gap-2">
          <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
          <span className="text-sm">{error}</span>
        </div>
      )}
      {success && (
        <div className="mb-4 p-3 bg-green-50 border border-green-200 text-green-800 rounded flex items-start gap-2">
          <CheckCircle2 className="w-4 h-4 mt-0.5 flex-shrink-0" />
          <span className="text-sm">{success}</span>
        </div>
      )}

      {/* Tabs */}
      <div className="flex border-b mb-4">
        <TabBtn active={tab === 'cron'} onClick={() => setTab('cron')} count={cronJobs.length}>
          Cron Jobs
        </TabBtn>
        <TabBtn active={tab === 'registry'} onClick={() => setTab('registry')} count={registryTotal}>
          Content Registry
        </TabBtn>
        <TabBtn active={tab === 'posts'} onClick={() => setTab('posts')} count={scheduledPosts.length}>
          Scheduled Posts
        </TabBtn>
        <TabBtn active={tab === 'newsletter'} onClick={() => setTab('newsletter')} count={newsletterLog.length}>
          Newsletters
        </TabBtn>
      </div>

      {/* Tab content */}
      {tab === 'cron' && (
        <CronTab
          jobs={cronJobs}
          error={cronError}
          busy={busy}
          onRun={runCron}
          onPause={pauseCron}
          onResume={resumeCron}
          onDelete={deleteCron}
        />
      )}
      {tab === 'registry' && (
        <RegistryTab
          data={registry}
          busy={busy}
          source={regSource}
          status={regStatus}
          onSourceChange={setRegSource}
          onStatusChange={setRegStatus}
          onReject={rejectReg}
          onDelete={deleteReg}
        />
      )}
      {tab === 'posts' && (
        <ScheduledPostsTab
          posts={scheduledPosts}
          busy={busy}
          onPublish={publishPostNow}
          onCancel={cancelSchedule}
        />
      )}
      {tab === 'newsletter' && (
        <NewsletterTab
          logs={newsletterLog}
          busy={busy}
          onResend={resendNewsletter}
          onDelete={deleteNewsletter}
        />
      )}
    </div>
  );
}

// ─── Sub-components ───

function TabBtn({ active, onClick, count, children }: {
  active: boolean; onClick: () => void; count?: number; children: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px flex items-center gap-2 ${
        active
          ? 'border-blue-600 text-blue-600'
          : 'border-transparent text-gray-600 hover:text-gray-900'
      }`}
    >
      {children}
      {count !== undefined && (
        <span className={`text-xs px-1.5 py-0.5 rounded-full ${
          active ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-600'
        }`}>
          {count}
        </span>
      )}
    </button>
  );
}

function CronTab({ jobs, error, busy, onRun, onPause, onResume, onDelete }: {
  jobs: CronJob[]; error: string | null; busy: string | null;
  onRun: (id: string) => void; onPause: (id: string) => void;
  onResume: (id: string) => void; onDelete: (id: string) => void;
}) {
  if (error) {
    return (
      <div className="p-4 bg-amber-50 border border-amber-200 rounded text-amber-900">
        <strong>Hermes CLI error:</strong> {error}
      </div>
    );
  }
  if (jobs.length === 0) {
    return <EmptyState icon={<Clock className="w-8 h-8" />} message="No cron jobs found" />;
  }
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      {jobs.map(job => (
        <div key={job.id} className="border rounded-lg p-4 bg-white">
          <div className="flex items-start justify-between mb-2">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <span className={`text-xs px-1.5 py-0.5 rounded ${
                  job.state === 'active' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-700'
                }`}>
                  {job.state}
                </span>
                {job.deliver && (
                  <span className="text-xs px-1.5 py-0.5 rounded bg-blue-50 text-blue-700">
                    {job.deliver}
                  </span>
                )}
              </div>
              <h3 className="font-semibold text-gray-900 truncate">{job.name}</h3>
              <p className="text-xs text-gray-500 font-mono">{job.schedule}</p>
              {job.script && <p className="text-xs text-gray-400 font-mono mt-0.5">{job.script}</p>}
            </div>
          </div>
          <div className="text-xs text-gray-600 space-y-0.5 mb-3">
            {job.nextRun && <div>Next: <span className="font-mono">{new Date(job.nextRun).toLocaleString('vi-VN')}</span></div>}
            {job.lastRun && (
              <div className="flex items-center gap-1">
                Last:
                <span className="font-mono">{new Date(job.lastRun).toLocaleString('vi-VN')}</span>
                {job.lastStatus === 'ok' && <CheckCircle2 className="w-3 h-3 text-green-600" />}
                {job.lastStatus === 'error' && <AlertCircle className="w-3 h-3 text-red-600" />}
              </div>
            )}
          </div>
          <div className="flex gap-1">
            <Btn icon={<Play className="w-3.5 h-3.5" />} label="Run" onClick={() => onRun(job.id)}
                 busy={busy === job.id + ':run'} variant="green" />
            {job.state === 'active' ? (
              <Btn icon={<Pause className="w-3.5 h-3.5" />} label="Pause" onClick={() => onPause(job.id)}
                   busy={busy === job.id + ':pause'} variant="amber" />
            ) : (
              <Btn icon={<RotateCcw className="w-3.5 h-3.5" />} label="Resume" onClick={() => onResume(job.id)}
                   busy={busy === job.id + ':resume'} variant="blue" />
            )}
            <Btn icon={<Trash2 className="w-3.5 h-3.5" />} label="Delete" onClick={() => onDelete(job.id)}
                 busy={busy === job.id + ':delete'} variant="red" />
          </div>
        </div>
      ))}
    </div>
  );
}

function RegistryTab({ data, busy, source, status, onSourceChange, onStatusChange, onReject, onDelete }: {
  data: ContentRegistryPage | null; busy: string | null;
  source: string; status: string;
  onSourceChange: (v: string) => void; onStatusChange: (v: string) => void;
  onReject: (id: number) => void; onDelete: (id: number) => void;
}) {
  return (
    <div>
      <div className="flex items-center gap-3 mb-3">
        <Filter className="w-4 h-4 text-gray-500" />
        <select value={source} onChange={e => onSourceChange(e.target.value)} className="border rounded px-2 py-1 text-sm">
          <option value="">All sources</option>
          <option value="github">GitHub</option>
          <option value="juejin">Juejin</option>
          <option value="dev-blog:martinfowler">Martin Fowler</option>
        </select>
        <select value={status} onChange={e => onStatusChange(e.target.value)} className="border rounded px-2 py-1 text-sm">
          <option value="">All statuses</option>
          <option value="collected">Collected</option>
          <option value="published">Published</option>
          <option value="rejected">Rejected</option>
        </select>
      </div>
      {!data || data.items.length === 0 ? (
        <EmptyState icon={<FileText className="w-8 h-8" />} message="No content registry items" />
      ) : (
        <div className="bg-white border rounded overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-600 text-xs uppercase">
              <tr>
                <th className="px-3 py-2 text-left">ID</th>
                <th className="px-3 py-2 text-left">Source</th>
                <th className="px-3 py-2 text-left">Topic</th>
                <th className="px-3 py-2 text-left">Pillar</th>
                <th className="px-3 py-2 text-left">Funnel</th>
                <th className="px-3 py-2 text-left">Status</th>
                <th className="px-3 py-2 text-left">Collected</th>
                <th className="px-3 py-2 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {data.items.map(item => (
                <tr key={item.id} className="hover:bg-gray-50">
                  <td className="px-3 py-2 font-mono text-gray-500">#{item.id}</td>
                  <td className="px-3 py-2">
                    <span className="text-xs px-1.5 py-0.5 rounded bg-blue-50 text-blue-700">{item.source}</span>
                  </td>
                  <td className="px-3 py-2 max-w-md">
                    <a href={item.sourceUrl} target="_blank" rel="noreferrer"
                       className="text-gray-900 hover:text-blue-600 line-clamp-1">
                      {item.topic}
                    </a>
                  </td>
                  <td className="px-3 py-2 text-xs text-gray-600">{item.pillar || '—'}</td>
                  <td className="px-3 py-2">
                    {item.funnel && (
                      <span className="text-xs px-1.5 py-0.5 rounded bg-purple-50 text-purple-700">{item.funnel}</span>
                    )}
                  </td>
                  <td className="px-3 py-2">
                    <StatusBadge status={item.status} />
                  </td>
                  <td className="px-3 py-2 text-xs text-gray-500">
                    {item.publishedAt ? new Date(item.publishedAt).toLocaleString('vi-VN') : '—'}
                  </td>
                  <td className="px-3 py-2 text-right">
                    {item.status === 'collected' && (
                      <button
                        onClick={() => onReject(item.id)}
                        disabled={busy !== null}
                        className="text-xs text-amber-600 hover:text-amber-800 disabled:opacity-50"
                        title="Reject this article"
                      >
                        {busy === 'reg:' + item.id + ':reject' ? '...' : 'Reject'}
                      </button>
                    )}
                    {' '}
                    <button
                      onClick={() => onDelete(item.id)}
                      disabled={busy !== null}
                      className="text-xs text-red-600 hover:text-red-800 disabled:opacity-50"
                      title="Delete this entry"
                    >
                      {busy === 'reg:' + item.id + ':delete' ? '...' : <Trash2 className="w-3.5 h-3.5 inline" />}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="px-3 py-2 bg-gray-50 text-xs text-gray-600 border-t">
            Showing {data.items.length} of {data.totalItems} items
          </div>
        </div>
      )}
    </div>
  );
}

function ScheduledPostsTab({ posts, busy, onPublish, onCancel }: {
  posts: ScheduledPostItem[]; busy: string | null;
  onPublish: (id: number) => void; onCancel: (id: number) => void;
}) {
  if (posts.length === 0) {
    return <EmptyState icon={<FileText className="w-8 h-8" />} message="No posts scheduled for publish" />;
  }
  return (
    <div className="space-y-2">
      {posts.map(p => (
        <div key={p.id} className="border rounded p-4 bg-white flex items-center justify-between">
          <div className="flex-1 min-w-0">
            <h3 className="font-medium text-gray-900 truncate">{p.title}</h3>
            <p className="text-xs text-gray-500 font-mono">/blog/{p.slug}</p>
            <p className="text-xs text-gray-600 mt-1">
              Scheduled: <span className="font-mono">{p.scheduledAt ? new Date(p.scheduledAt).toLocaleString('vi-VN') : '—'}</span>
              {' '}
              <span className="text-blue-600">({p.timeUntilPublish})</span>
            </p>
          </div>
          <div className="flex gap-2 flex-shrink-0">
            <button
              onClick={() => onPublish(p.id)}
              disabled={busy !== null}
              className="flex items-center gap-1 px-3 py-1.5 text-sm bg-green-50 text-green-700 border border-green-200 rounded hover:bg-green-100 disabled:opacity-50"
            >
              <Play className="w-3.5 h-3.5" />
              Publish now
            </button>
            <button
              onClick={() => onCancel(p.id)}
              disabled={busy !== null}
              className="flex items-center gap-1 px-3 py-1.5 text-sm bg-amber-50 text-amber-700 border border-amber-200 rounded hover:bg-amber-100 disabled:opacity-50"
            >
              <XCircle className="w-3.5 h-3.5" />
              Cancel
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}

function NewsletterTab({ logs, busy, onResend, onDelete }: {
  logs: NewsletterLogItem[]; busy: string | null;
  onResend: (id: number) => void; onDelete: (id: number) => void;
}) {
  if (logs.length === 0) {
    return <EmptyState icon={<Mail className="w-8 h-8" />} message="No newsletter sends yet" />;
  }
  return (
    <div className="bg-white border rounded overflow-x-auto">
      <table className="w-full text-sm">
        <thead className="bg-gray-50 text-gray-600 text-xs uppercase">
          <tr>
            <th className="px-3 py-2 text-left">ID</th>
            <th className="px-3 py-2 text-left">Subject</th>
            <th className="px-3 py-2 text-right">Recipients</th>
            <th className="px-3 py-2 text-right">Success</th>
            <th className="px-3 py-2 text-right">Failed</th>
            <th className="px-3 py-2 text-left">Status</th>
            <th className="px-3 py-2 text-left">Sent at</th>
            <th className="px-3 py-2 text-right">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y">
          {logs.map(log => (
            <tr key={log.id} className="hover:bg-gray-50">
              <td className="px-3 py-2 font-mono text-gray-500">#{log.id}</td>
              <td className="px-3 py-2 text-gray-900">{log.subject}</td>
              <td className="px-3 py-2 text-right">{log.recipientCount}</td>
              <td className="px-3 py-2 text-right text-green-700">{log.successCount}</td>
              <td className="px-3 py-2 text-right text-red-700">{log.failureCount}</td>
              <td className="px-3 py-2">
                <StatusBadge status={log.deliveryStatus} />
              </td>
              <td className="px-3 py-2 text-xs text-gray-500">
                {new Date(log.sentAt).toLocaleString('vi-VN')}
              </td>
              <td className="px-3 py-2 text-right">
                <button
                  onClick={() => onResend(log.id)}
                  disabled={busy !== null}
                  className="text-xs text-blue-600 hover:text-blue-800 disabled:opacity-50 mr-2"
                >
                  Resend
                </button>
                <button
                  onClick={() => onDelete(log.id)}
                  disabled={busy !== null}
                  className="text-xs text-red-600 hover:text-red-800 disabled:opacity-50"
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const colorMap: Record<string, string> = {
    collected: 'bg-blue-50 text-blue-700',
    published: 'bg-green-50 text-green-700',
    rejected:  'bg-gray-100 text-gray-700',
    ok:        'bg-green-50 text-green-700',
    partial:   'bg-amber-50 text-amber-700',
    failed:    'bg-red-50 text-red-700',
    empty:     'bg-gray-50 text-gray-600',
  };
  return (
    <span className={`text-xs px-1.5 py-0.5 rounded ${colorMap[status] || 'bg-gray-100 text-gray-700'}`}>
      {status}
    </span>
  );
}

function Btn({ icon, label, onClick, busy, variant }: {
  icon: React.ReactNode; label: string; onClick: () => void; busy: boolean;
  variant: 'green' | 'amber' | 'red' | 'blue';
}) {
  const colors = {
    green: 'bg-green-50 text-green-700 border-green-200 hover:bg-green-100',
    amber: 'bg-amber-50 text-amber-700 border-amber-200 hover:bg-amber-100',
    red:   'bg-red-50 text-red-700 border-red-200 hover:bg-red-100',
    blue:  'bg-blue-50 text-blue-700 border-blue-200 hover:bg-blue-100',
  };
  return (
    <button
      onClick={onClick}
      disabled={busy}
      className={`flex items-center gap-1 px-2.5 py-1 text-xs border rounded disabled:opacity-50 ${colors[variant]}`}
    >
      {icon}
      {label}
    </button>
  );
}

function EmptyState({ icon, message }: { icon: React.ReactNode; message: string }) {
  return (
    <div className="p-12 text-center text-gray-400 border-2 border-dashed rounded-lg">
      <div className="inline-flex items-center justify-center w-12 h-12 mb-3 rounded-full bg-gray-50">
        {icon}
      </div>
      <p>{message}</p>
    </div>
  );
}
