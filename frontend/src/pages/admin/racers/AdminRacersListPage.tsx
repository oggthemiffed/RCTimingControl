import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Loader2, ChevronRight, Users } from 'lucide-react';
import api from '@/lib/api';

interface UserSummaryDto {
  id: number;
  firstName: string;
  lastName: string;
  memberships: { code: string; number: string }[];
}

const listUsers = () =>
  api.get<UserSummaryDto[]>('/api/v1/admin/users').then((r) => r.data);

export default function AdminRacersListPage() {
  const navigate = useNavigate();

  const { data: users, isLoading } = useQuery({
    queryKey: ['admin-users'],
    queryFn: listUsers,
  });

  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Racers</h1>
        <p className="text-sm text-muted-foreground mt-1">
          View and manage racer announcement settings.
        </p>
      </div>

      {isLoading && (
        <div className="flex items-center gap-2 py-8">
          <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
          <span className="text-sm text-muted-foreground">Loading racers…</span>
        </div>
      )}

      {!isLoading && (!users || users.length === 0) && (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <Users className="h-10 w-10 text-muted-foreground mb-4" aria-hidden="true" />
          <h2 className="text-lg font-semibold">No racers registered</h2>
          <p className="text-sm text-muted-foreground mt-1">
            Racers appear here once they register an account.
          </p>
        </div>
      )}

      {!isLoading && users && users.length > 0 && (
        <div className="space-y-1">
          {users.map((u) => (
            <button
              key={u.id}
              onClick={() => navigate(`/admin/racers/${u.id}`)}
              className="w-full flex items-center justify-between rounded-lg border bg-card px-4 py-3 text-left hover:bg-muted/50 transition-colors group"
            >
              <div>
                <p className="font-medium">
                  {u.firstName} {u.lastName}
                </p>
                {u.memberships.length > 0 && (
                  <p className="text-xs text-muted-foreground mt-0.5">
                    {u.memberships.map((m) => `${m.code} ${m.number}`).join(' · ')}
                  </p>
                )}
              </div>
              <ChevronRight className="h-4 w-4 text-muted-foreground group-hover:text-foreground transition-colors shrink-0" aria-hidden="true" />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
