import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});

let csrfToken: string | null = null;

async function refreshCsrf(): Promise<string | null> {
  try {
    const res = await axios.get('/api/admin/auth/csrf', { withCredentials: true });
    csrfToken = res.data?.data?.csrfToken ?? null;
    return csrfToken;
  } catch {
    return null;
  }
}

// Interceptor: attach CSRF token to state-changing requests
api.interceptors.request.use(async (config) => {
  const method = config.method?.toUpperCase();
  if (method && ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
    if (!csrfToken) {
      await refreshCsrf();
    }
    if (csrfToken) {
      config.headers['X-CSRF-Token'] = csrfToken;
    }
  }
  return config;
});

// Retry once on CSRF failure (401/403 from CSRF mismatch)
api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    if (
      original &&
      !original._csrfRetry &&
      error.response?.status === 403 &&
      error.response?.data?.error?.code === 'CSRF_TOKEN_INVALID'
    ) {
      original._csrfRetry = true;
      const token = await refreshCsrf();
      if (token) {
        original.headers['X-CSRF-Token'] = token;
        return api(original);
      }
    }
    return Promise.reject(error);
  }
);

export { api, refreshCsrf };
