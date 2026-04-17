import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import type { CarDto } from '@/lib/racerApi';

interface CarCardProps {
  car: CarDto;
  onClick: () => void;
}

export default function CarCard({ car, onClick }: CarCardProps) {
  return (
    <Card
      role="button"
      tabIndex={0}
      onClick={onClick}
      onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') onClick(); }}
      className="cursor-pointer hover:border-primary transition-colors"
    >
      <CardHeader className="pb-2">
        <CardTitle className="text-lg">{car.name}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-2">
        {car.primaryClassId != null && (
          <p className="text-sm text-muted-foreground">Class #{car.primaryClassId}</p>
        )}
        {car.tags.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {car.tags.slice(0, 3).map((t, i) => (
              <Badge key={`${t.categoryId}-${i}`} variant="secondary">
                {t.categoryName}: {t.value}
              </Badge>
            ))}
            {car.tags.length > 3 && (
              <Badge variant="outline">+{car.tags.length - 3}</Badge>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
