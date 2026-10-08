export const raceControlQueryKeys = {
  all: ['race-control'] as const,
  preRaceReadinessAll: ['race-control', 'pre-race-readiness'] as const,
  preRaceReadiness: (raceId: number) => ['race-control', 'pre-race-readiness', raceId] as const,
  runOrder: (eventId: number) => ['race-control', 'run-order', eventId] as const,
  raceHistory: (raceId: number) => ['race-control', 'race-history', raceId] as const,
  resultSnapshot: (raceId: number) => ['race-control', 'result-snapshot', raceId] as const,
};
