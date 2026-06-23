export default function PostsPage() {
  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-text">Posts</h1>
          <p className="text-text-muted">Manage your blog posts</p>
        </div>
        <button className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary-dark transition-colors text-sm font-medium">
          + New Post
        </button>
      </div>
      <div className="bg-surface rounded-xl border border-border p-8 text-center text-text-muted">
        📝 Posts CRUD coming in Story 5
      </div>
    </div>
  );
}

export function TopicsPage() {
  return (
    <div>
      <h1 className="text-2xl font-bold text-text mb-1">Topics</h1>
      <p className="text-text-muted mb-6">Manage categories</p>
      <div className="bg-surface rounded-xl border border-border p-8 text-center text-text-muted">
        📂 Topics CRUD coming in Story 5
      </div>
    </div>
  );
}

export function TagsPage() {
  return (
    <div>
      <h1 className="text-2xl font-bold text-text mb-1">Tags</h1>
      <p className="text-text-muted mb-6">Manage tags</p>
      <div className="bg-surface rounded-xl border border-border p-8 text-center text-text-muted">
        🏷️ Tags CRUD coming in Story 5
      </div>
    </div>
  );
}

export function ProjectsPage() {
  return (
    <div>
      <h1 className="text-2xl font-bold text-text mb-1">Projects</h1>
      <p className="text-text-muted mb-6">Manage portfolio projects</p>
      <div className="bg-surface rounded-xl border border-border p-8 text-center text-text-muted">
        💼 Projects CRUD coming in Story 5
      </div>
    </div>
  );
}

export function MediaPage() {
  return (
    <div>
      <h1 className="text-2xl font-bold text-text mb-1">Media</h1>
      <p className="text-text-muted mb-6">Manage uploaded images & files</p>
      <div className="bg-surface rounded-xl border border-border p-8 text-center text-text-muted">
        🖼️ Media management coming in Story 5
      </div>
    </div>
  );
}
