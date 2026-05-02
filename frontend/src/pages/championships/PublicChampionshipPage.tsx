import { useParams } from 'react-router-dom';
import { ChampionshipStandingsTable } from '@/pages/admin/championships/ChampionshipStandingsTable';

export default function PublicChampionshipPage() {
  const { id } = useParams<{ id: string }>();
  const championshipId = Number(id);

  return (
    <div className="p-8 max-w-4xl mx-auto">
      <ChampionshipStandingsTable championshipId={championshipId} />
    </div>
  );
}
