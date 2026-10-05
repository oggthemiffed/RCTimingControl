import { createBrowserRouter, RouterProvider, Navigate, Outlet } from 'react-router-dom';
import { QueryProvider } from '@/providers/QueryProvider';
import { AuthProvider } from '@/providers/AuthProvider';
import ProtectedRoute from '@/components/ProtectedRoute';
import SetupGuard from '@/pages/setup/SetupGuard';
import SetupLayout from '@/pages/setup/SetupLayout';
import LoginPage from '@/pages/auth/LoginPage';
import NotFoundPage from '@/pages/NotFoundPage';
import UnauthorizedPage from '@/pages/UnauthorizedPage';
import { Toaster } from '@/components/ui/sonner';
import EventSchedulePage from '@/pages/events/EventSchedulePage';
import AdminPanelLayout from '@/pages/admin/AdminPanelLayout';
import EventListPage from '@/pages/admin/events/EventListPage';
import EventDetailPage from '@/pages/admin/events/EventDetailPage';
import ChampionshipListPage from '@/pages/admin/championships/ChampionshipListPage';
import ChampionshipDetailPage from '@/pages/admin/championships/ChampionshipDetailPage';
import ClubProfilePage from '@/pages/admin/club/ClubProfilePage';
import AdminAudioSettingsPage from '@/pages/admin/club/AdminAudioSettingsPage';
import OfficialsPage from '@/pages/admin/officials/OfficialsPage';
import TracksPage from '@/pages/admin/tracks/TracksPage';
import FormatsPage from '@/pages/admin/formats/FormatsPage';
import RaceControlSelectPage from '@/pages/admin/race-control/RaceControlSelectPage';
import DecoderSettingsPage from '@/pages/admin/decoder/DecoderSettingsPage';
import BackupsPage from '@/pages/admin/backups/BackupsPage';
import ResultsExportsPage from '@/pages/admin/results-exports/ResultsExportsPage';
import CompetitorsPage from '@/pages/admin/competitors/CompetitorsPage';
import CheckInPage from '@/pages/race-control/check-in/CheckInPage';
import RaceControlLayout from '@/pages/race-control/RaceControlLayout';
import CockpitPage from '@/pages/race-control/CockpitPage';
import RefereePage from '@/pages/race-control/RefereePage';
import PrintResultsPage from '@/pages/race-control/PrintResultsPage';
import { PracticeSessionPage } from '@/pages/race-control/PracticeSessionPage';
import { PracticeLandingPage } from '@/pages/race-control/PracticeLandingPage';
import PrintPracticeResultsPage from '@/pages/race-control/PrintPracticeResultsPage';
import PublicResultsPage from '@/pages/results/PublicResultsPage';
import PublicChampionshipPage from '@/pages/championships/PublicChampionshipPage';
import { HelpProvider } from '@/context/HelpContext';
import MeetingGuidePage from '@/pages/print/MeetingGuidePage';
import AdminGuidePage from '@/pages/print/AdminGuidePage';
import AboutPage from '@/pages/AboutPage';
import NowNextBoard from '@/pages/boards/NowNextBoard';
import ResultsBoard from '@/pages/boards/ResultsBoard';
import OverlayBoard from '@/pages/boards/OverlayBoard';

function RootLayout() {
  return (
    <AuthProvider>
      <HelpProvider>
        <SetupGuard>
          <Outlet />
        </SetupGuard>
      </HelpProvider>
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
          { path: 'race-control', element: <RaceControlSelectPage /> },
          { path: 'decoder', element: <ProtectedRoute roles={['ADMIN']}><DecoderSettingsPage /></ProtectedRoute> },
          { path: 'backups', element: <ProtectedRoute roles={['ADMIN']}><BackupsPage /></ProtectedRoute> },
          { path: 'results-exports', element: <ProtectedRoute roles={['ADMIN']}><ResultsExportsPage /></ProtectedRoute> },
          { path: 'officials', element: <ProtectedRoute roles={['ADMIN']}><OfficialsPage /></ProtectedRoute> },
          { path: 'audio', element: <AdminAudioSettingsPage /> },
          { path: 'competitors', element: <CompetitorsPage /> },
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
          { path: 'check-in', element: <CheckInPage /> },
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
      { path: '/setup', element: <SetupLayout /> },
      { path: '/events', element: <EventSchedulePage /> },
      { path: '/results/:raceId', element: <PublicResultsPage /> },
      { path: '/championships/:id', element: <PublicChampionshipPage /> },
      { path: '/print/meeting-guide', element: <MeetingGuidePage /> },
      { path: '/print/admin-guide', element: <AdminGuidePage /> },
      { path: '/about', element: <AboutPage /> },
      { path: '/boards/now-next', element: <NowNextBoard /> },
      { path: '/boards/results', element: <ResultsBoard /> },
      { path: '/boards/overlay', element: <OverlayBoard /> },
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
