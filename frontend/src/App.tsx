import { createBrowserRouter, RouterProvider, Navigate, Outlet } from 'react-router-dom';
import { QueryProvider } from '@/providers/QueryProvider';
import { AuthProvider } from '@/providers/AuthProvider';
import ProtectedRoute from '@/components/ProtectedRoute';
import LoginPage from '@/pages/auth/LoginPage';
import RegisterPage from '@/pages/auth/RegisterPage';
import ForgotPasswordPage from '@/pages/auth/ForgotPasswordPage';
import ResetPasswordPage from '@/pages/auth/ResetPasswordPage';
import NotFoundPage from '@/pages/NotFoundPage';
import UnauthorizedPage from '@/pages/UnauthorizedPage';
import { Toaster } from '@/components/ui/sonner';
import RacerPortalLayout from '@/pages/racer/RacerPortalLayout';
import ProfilePage from '@/pages/racer/ProfilePage';
import CarsPage from '@/pages/racer/CarsPage';
import TranspondersPage from '@/pages/racer/TranspondersPage';
import EntriesPage from '@/pages/racer/EntriesPage';
import EventSchedulePage from '@/pages/events/EventSchedulePage';
import AdminPanelLayout from '@/pages/admin/AdminPanelLayout';
import EventListPage from '@/pages/admin/events/EventListPage';
import EventDetailPage from '@/pages/admin/events/EventDetailPage';

// Placeholder for Plan 06 routes (championships, club, tracks, formats, categories)
function AdminComingSoonPage() {
  return (
    <div className="flex items-center justify-center py-20">
      <p className="text-muted-foreground text-sm">Coming in Plan 06.</p>
    </div>
  );
}

function RootLayout() {
  return (
    <AuthProvider>
      <Outlet />
      <Toaster />
    </AuthProvider>
  );
}

const router = createBrowserRouter([
  {
    element: <RootLayout />,
    children: [
      { path: '/', element: <Navigate to="/login" replace /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/forgot-password', element: <ForgotPasswordPage /> },
      { path: '/reset-password', element: <ResetPasswordPage /> },
      {
        path: '/admin',
        element: (
          <ProtectedRoute roles={['ADMIN', 'RACE_DIRECTOR', 'REFEREE']}>
            <AdminPanelLayout />
          </ProtectedRoute>
        ),
        children: [
          { index: true, element: <Navigate to="/admin/events" replace /> },
          { path: 'events', element: <EventListPage /> },
          { path: 'events/:id', element: <EventDetailPage /> },
          // Plan 06 will replace these placeholders with full pages
          { path: 'championships', element: <AdminComingSoonPage /> },
          { path: 'championships/:id', element: <AdminComingSoonPage /> },
          { path: 'tracks', element: <AdminComingSoonPage /> },
          { path: 'formats', element: <AdminComingSoonPage /> },
          { path: 'club', element: <AdminComingSoonPage /> },
          { path: 'categories', element: <AdminComingSoonPage /> },
        ],
      },
      {
        path: '/racer',
        element: (
          <ProtectedRoute>
            <RacerPortalLayout />
          </ProtectedRoute>
        ),
        children: [
          { index: true, element: <Navigate to="/racer/profile" replace /> },
          { path: 'profile', element: <ProfilePage /> },
          { path: 'cars', element: <CarsPage /> },
          { path: 'transponders', element: <TranspondersPage /> },
          { path: 'entries', element: <EntriesPage /> },
        ],
      },
      { path: '/events', element: <EventSchedulePage /> },
      { path: '/unauthorized', element: <UnauthorizedPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]);

export default function App() {
  return (
    <QueryProvider>
      <RouterProvider router={router} />
    </QueryProvider>
  );
}
