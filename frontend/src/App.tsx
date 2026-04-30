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
import ChampionshipListPage from '@/pages/admin/championships/ChampionshipListPage';
import ChampionshipDetailPage from '@/pages/admin/championships/ChampionshipDetailPage';
import ClubProfilePage from '@/pages/admin/club/ClubProfilePage';
import AdminAudioSettingsPage from '@/pages/admin/club/AdminAudioSettingsPage';
import TracksPage from '@/pages/admin/tracks/TracksPage';
import FormatsPage from '@/pages/admin/formats/FormatsPage';
import CarTagCategoriesPage from '@/pages/admin/categories/CarTagCategoriesPage';
import RaceControlSelectPage from '@/pages/admin/race-control/RaceControlSelectPage';
import ForwarderTokenPage from '@/pages/admin/race-control/ForwarderTokenPage';
import AdminRacersListPage from '@/pages/admin/racers/AdminRacersListPage';
import AdminRacerDetailPage from '@/pages/admin/racers/AdminRacerDetailPage';
import RaceControlLayout from '@/pages/race-control/RaceControlLayout';
import CockpitPage from '@/pages/race-control/CockpitPage';
import RefereePage from '@/pages/race-control/RefereePage';
import PrintResultsPage from '@/pages/race-control/PrintResultsPage';
import { PracticeSessionPage } from '@/pages/race-control/PracticeSessionPage';
import { PracticeLandingPage } from '@/pages/race-control/PracticeLandingPage';
import PrintPracticeResultsPage from '@/pages/race-control/PrintPracticeResultsPage';

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
          { path: 'championships', element: <ChampionshipListPage /> },
          { path: 'championships/:id', element: <ChampionshipDetailPage /> },
          { path: 'club', element: <ClubProfilePage /> },
          { path: 'tracks', element: <TracksPage /> },
          { path: 'formats', element: <FormatsPage /> },
          { path: 'categories', element: <CarTagCategoriesPage /> },
          { path: 'race-control', element: <RaceControlSelectPage /> },
          { path: 'forwarder', element: <ForwarderTokenPage /> },
          { path: 'audio', element: <AdminAudioSettingsPage /> },
          { path: 'racers', element: <AdminRacersListPage /> },
          { path: 'racers/:userId', element: <AdminRacerDetailPage /> },
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
      {
        path: '/race-control/event/:eventId',
        element: (
          <ProtectedRoute roles={['RACE_DIRECTOR', 'REFEREE', 'ADMIN']}>
            <RaceControlLayout />
          </ProtectedRoute>
        ),
        children: [
          { index: true, element: <CockpitPage /> },
          { path: 'practice', element: <PracticeLandingPage /> },
          { path: 'referee', element: <RefereePage /> },
          { path: 'results/:raceId', element: <PrintResultsPage /> },
        ],
      },
      {
        path: '/race-control/practice/:sessionId',
        element: (
          <ProtectedRoute roles={['RACE_DIRECTOR', 'ADMIN']}>
            <PracticeSessionPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/race-control/practice/:sessionId/print',
        element: (
          <ProtectedRoute roles={['RACE_DIRECTOR', 'ADMIN']}>
            <PrintPracticeResultsPage />
          </ProtectedRoute>
        ),
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
