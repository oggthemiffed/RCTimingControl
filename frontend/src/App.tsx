import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom';
import { QueryProvider } from '@/providers/QueryProvider';
import { AuthProvider } from '@/providers/AuthProvider';
import ProtectedRoute from '@/components/ProtectedRoute';
import LoginPage from '@/pages/auth/LoginPage';
import RegisterPage from '@/pages/auth/RegisterPage';
import ForgotPasswordPage from '@/pages/auth/ForgotPasswordPage';
import ResetPasswordPage from '@/pages/auth/ResetPasswordPage';
import AdminPlaceholderPage from '@/pages/admin/AdminPlaceholderPage';
import NotFoundPage from '@/pages/NotFoundPage';
import { Toaster } from '@/components/ui/sonner';
import RacerPortalLayout from '@/pages/racer/RacerPortalLayout';
import ProfilePage from '@/pages/racer/ProfilePage';
import CarsPage from '@/pages/racer/CarsPage';
import TranspondersPage from '@/pages/racer/TranspondersPage';
import EntriesPage from '@/pages/racer/EntriesPage';
import EventSchedulePage from '@/pages/events/EventSchedulePage';

const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/login" replace /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  { path: '/forgot-password', element: <ForgotPasswordPage /> },
  { path: '/reset-password', element: <ResetPasswordPage /> },
  {
    path: '/admin/*',
    element: (
      <ProtectedRoute roles={['ADMIN', 'RACE_DIRECTOR', 'REFEREE']}>
        <AdminPlaceholderPage />
      </ProtectedRoute>
    ),
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
  { path: '*', element: <NotFoundPage /> },
]);

export default function App() {
  return (
    <QueryProvider>
      <AuthProvider>
        <RouterProvider router={router} />
        <Toaster />
      </AuthProvider>
    </QueryProvider>
  );
}
