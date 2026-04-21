export const adminQueryKeys = {
  events: {
    all: () => ['admin', 'events'] as const,
    detail: (id: number) => ['admin', 'events', id] as const,
    entriesForClass: (eventId: number, classId: number) =>
      ['admin', 'events', eventId, 'classes', classId, 'entries'] as const,
    classesFor: (eventId: number) =>
      ['admin', 'events', eventId, 'classes'] as const,
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
  carTagCategories: {
    all: (includeArchived: boolean) =>
      ['admin', 'car-tag-categories', { includeArchived }] as const,
  },
};
