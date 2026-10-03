import BoardsRouter from '@/features/boards/BoardsRouter';
import LoginPage from '@/features/auth/LoginPage';

export default function App() {
  if (window.location.pathname.startsWith('/boards/')) {
    return <BoardsRouter />;
  }
  return <LoginPage />;
}
