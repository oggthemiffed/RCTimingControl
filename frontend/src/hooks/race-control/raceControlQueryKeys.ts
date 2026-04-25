export const raceControlQueryKeys = {
  all: ['race-control'] as const,
  preRaceReadiness: (raceId: number) => ['race-control', 'pre-race-readiness', raceId] as const,
  runOrder: (eventId: number) => ['race-control', 'run-order', eventId] as const,
  resultSnapshot: (raceId: number) => ['race-control', 'result-snapshot', raceId] as const,
};
