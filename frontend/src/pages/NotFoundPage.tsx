import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4">
      <h1 className="text-2xl font-semibold">Page not found</h1>
      <p className="text-muted-foreground">The page you were looking for doesn't exist.</p>
      <Link to="/login" className="text-primary underline underline-offset-4">
        Back to sign in
      </Link>
    </div>
  );
}
