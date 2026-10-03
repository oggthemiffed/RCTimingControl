// Hand-rolled path switch for the two static, anonymous board addresses — this project has no
// router library (deliberately, per project convention) and board addressability is exactly two
// static paths, so a full router would be overkill. `pathname` defaults to the real browser
// location but can be passed explicitly, which is what makes this trivially testable without
// mocking window.location/navigation.
import NowNextBoard from './NowNextBoard';
import ResultsBoard from './ResultsBoard';

export interface BoardsRouterProps {
  pathname?: string;
}

export default function BoardsRouter({ pathname = window.location.pathname }: BoardsRouterProps) {
  if (pathname === '/boards/now-next') {
    return <NowNextBoard />;
  }
  if (pathname === '/boards/results') {
    return <ResultsBoard />;
  }
  return (
    <div className="flex min-h-screen items-center justify-center p-8">
      <p className="text-lg text-slate-500">
        Unknown board — use /boards/now-next or /boards/results
      </p>
    </div>
  );
}
