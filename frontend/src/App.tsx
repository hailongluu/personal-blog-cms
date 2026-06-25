import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { HelmetProvider } from 'react-helmet-async';
import { AuthProvider, useAuth } from '@/contexts/AuthContext';
import AdminLayout from '@/layouts/AdminLayout';
import PublicLayout from '@/layouts/PublicLayout';
import LoginPage from '@/pages/LoginPage';
import ForgotPasswordPage from '@/pages/ForgotPasswordPage';
import ResetPasswordPage from '@/pages/ResetPasswordPage';
import DashboardPage from '@/pages/DashboardPage';
import PostsPage from '@/pages/PostsPage';
import TopicsPage from '@/pages/TopicsPage';
import TagsPage from '@/pages/TagsPage';
import ProjectsPage from '@/pages/ProjectsPage';
import MediaPage from '@/pages/MediaPage';
import ScheduledTasksPage from '@/pages/ScheduledTasksPage';
import SettingsPage from '@/pages/SettingsPage';
import ProfilePage from '@/pages/ProfilePage';
import HomePage from '@/pages/public/HomePage';
import BlogListPage from '@/pages/public/BlogListPage';
import BlogDetailPage from '@/pages/public/BlogDetailPage';
import TopicPage from '@/pages/public/TopicPage';
import ProjectsPublicPage from '@/pages/public/ProjectsPage';
import ProjectDetailPage from '@/pages/public/ProjectDetailPage';
import AboutPage from '@/pages/public/AboutPage';
import NowPage from '@/pages/public/NowPage';
import NewsletterPage from '@/pages/public/NewsletterPage';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="flex items-center justify-center h-screen"><div className="animate-spin w-8 h-8 border-4 border-primary border-t-transparent rounded-full" /></div>;
  if (!user) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

function GuestRoute({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="flex items-center justify-center h-screen"><div className="animate-spin w-8 h-8 border-4 border-primary border-t-transparent rounded-full" /></div>;
  if (user) return <Navigate to="/admin" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <HelmetProvider>
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            {/* Public pages (no auth required) */}
            <Route element={<PublicLayout />}>
              <Route index element={<HomePage />} />
              <Route path="blog" element={<BlogListPage />} />
              <Route path="blog/:slug" element={<BlogDetailPage />} />
              <Route path="topics/:slug" element={<TopicPage />} />
              <Route path="projects" element={<ProjectsPublicPage />} />
              <Route path="projects/:slug" element={<ProjectDetailPage />} />
              <Route path="about" element={<AboutPage />} />
              <Route path="now" element={<NowPage />} />
              <Route path="newsletter" element={<NewsletterPage />} />
            </Route>

            {/* Login page */}
            <Route path="/login" element={<GuestRoute><LoginPage /></GuestRoute>} />
            <Route path="/forgot-password" element={<GuestRoute><ForgotPasswordPage /></GuestRoute>} />
            <Route path="/reset-password" element={<GuestRoute><ResetPasswordPage /></GuestRoute>} />

            {/* Admin routes (auth required) */}
            <Route path="/admin" element={<ProtectedRoute><AdminLayout /></ProtectedRoute>}>
              <Route index element={<DashboardPage />} />
              <Route path="posts" element={<PostsPage />} />
              <Route path="topics" element={<TopicsPage />} />
              <Route path="tags" element={<TagsPage />} />
              <Route path="projects" element={<ProjectsPage />} />
              <Route path="media" element={<MediaPage />} />
              <Route path="scheduled-tasks" element={<ScheduledTasksPage />} />
              <Route path="settings" element={<SettingsPage />} />
              <Route path="profile" element={<ProfilePage />} />
            </Route>

            {/* Catch-all */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </HelmetProvider>
  );
}
