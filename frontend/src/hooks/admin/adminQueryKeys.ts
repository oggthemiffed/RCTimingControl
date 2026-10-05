export const adminQueryKeys = {
  events: {
    all: () => ['admin', 'events'] as const,
    detail: (id: number) => ['admin', 'events', id] as const,
    entriesForClass: (eventId: number, classId: number) =>
      ['admin', 'events', eventId, 'classes', classId, 'entries'] as const,
    classesFor: (eventId: number) =>
      ['admin', 'events', eventId, 'classes'] as const,
    racehubClassMappings: (eventId: number) =>
      ['admin', 'events', eventId, 'racehub-class-mappings'] as const,
  },
  championships: {
    all: () => ['admin', 'championships'] as const,
    detail: (id: number) => ['admin', 'championships', id] as const,
    standings: (id: number) => ['admin', 'championships', id, 'standings'] as const,
    exclusions: (id: number) => ['admin', 'championships', id, 'exclusions'] as const,
  },
  club: {
    profile: () => ['admin', 'club', 'profile'] as const,
  },
  tracks: {
    all: () => ['admin', 'tracks'] as const,
  },
  formats: {
    all: () => ['admin', 'formats'] as const,
  },
  racingClasses: {
    all: () => ['admin', 'racing-classes'] as const,
  },
  competitors: {
    all: () => ['admin', 'competitors'] as const,
  },
  backups: {
    all: () => ['admin', 'backups'] as const,
  },
};
